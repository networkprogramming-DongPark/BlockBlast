package blockmatch.client.ui;

import blockmatch.common.GameRules;
import blockmatch.common.GameState;
import blockmatch.common.PlayerState;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Predicate;

/**
 * 게임 창. 왼쪽에 내 화면, 오른쪽에 상대 화면을 나란히 두고 가운데에 남은 시간과 버튼을 둔다.
 * 두 화면 모두 플레이어 정보, 보드, 블록 트레이를 보여 준다. 채팅은 가운데 아래의 버튼으로 열고 닫는 패널이다.
 * 창 크기는 바뀌지 않고, 채팅을 열면 그 폭만큼 게임 화면이 줄어들어 가려지는 곳 없이 모두 보인다.
 * 내 트레이의 블록을 끌어 내 보드에 놓을 수 있다. 놓는 요청은 서버로 보내고, 서버가 보낸 상태로 두 보드를 다시 그린다.
 */
public final class GameFrame extends JFrame {
    /** 화면용 GameState에서는 0번이 항상 나, 1번이 상대다. 서버 번호와의 변환은 GameSession이 한다. */
    private static final int MY_PLAYER = 0;
    private static final int OPPONENT_PLAYER = 1;

    private static final int MARGIN = 24;
    private static final Color MY_AVATAR = new Color(0x7F9AE8);
    private static final Color OPPONENT_AVATAR = new Color(0xE59AA6);

    /** 자주 쓰는 문구. 누르면 바로 채팅으로 보낸다. */
    private static final String[] QUICK_PHRASES = {"블록을 빨리 놔주세요", "잘하시네요!", "잠깐만 기다려 주세요"};

    /** 블록 배치 요청을 서버로 보낸다. 보냈으면 true. */
    public interface PlaceSender {
        boolean send(int handIndex, int row, int col);
    }

    private final ChatPanel chatPanel = new ChatPanel();
    private final PlayerState me;
    private final PlayerState opponent;
    private final PlayerInfoPanel myInfo;
    private final PlayerInfoPanel opponentInfo;
    private final BoardPanel myBoard;
    private final BoardPanel opponentBoard;
    private final BlockTrayPanel myTray;
    private final BlockTrayPanel opponentTray;
    private final PlayerColumn opponentColumn;
    private final IconButton emoticonButton = new IconButton(IconButton.Kind.EMOTICON, "이모티콘과 빠른 문구");
    private final IconButton chatButton = new IconButton(IconButton.Kind.CHAT, "채팅 열기/닫기");
    private final IconButton exitButton = new IconButton(IconButton.Kind.EXIT, "메인 화면으로 나가기");
    private final MatchStatusPanel status = new MatchStatusPanel(emoticonButton, chatButton, exitButton);
    private PlaceSender placeSender = (handIndex, row, col) -> false;
    /** START를 받은 뒤부터 GAMEOVER를 받기 전까지 true. */
    private boolean playing;
    /** GAMEOVER를 받은 뒤에는 블록을 놓지 않는다. */
    private boolean gameOver;
    /** 바로 전 상태의 내 점수. START 뒤 첫 상태를 받기 전에는 -1이며, 이때는 점수 변화를 보여 주지 않는다. */
    private int previousScore = -1;
    /** 채팅창이 닫힌 동안 도착한 상대 채팅 수. 다시 열면 0으로 돌아간다. */
    private int unreadCount;
    /** 창의 닫기 버튼을 눌렀을 때 할 일. ClientApp이 메인 화면으로 돌아가도록 연결한다. */
    private Runnable onClose = () -> { };

    public GameFrame(GameState state) {
        super("블록 매치");
        // 닫기 버튼을 눌러도 프로그램을 끝내지 않고 onClose로 메인 화면에 돌아간다.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                onClose.run();
            }
        });

        me = state.getPlayer(MY_PLAYER);
        opponent = state.getPlayer(OPPONENT_PLAYER);
        myInfo = new PlayerInfoPanel(me.getName(), true, MY_AVATAR);
        opponentInfo = new PlayerInfoPanel(opponent.getName(), false, OPPONENT_AVATAR);
        myBoard = new BoardPanel(me.getBoard());
        opponentBoard = new BoardPanel(opponent.getBoard());
        myTray = new BlockTrayPanel(me, true);
        opponentTray = new BlockTrayPanel(opponent, false);

        PlayerColumn myColumn = new PlayerColumn(myInfo, "내 보드", Theme.TEXT, myBoard, myTray);
        myColumn.setStatus("블록을 끌어 놓으세요", Theme.MUTED);
        opponentColumn = new PlayerColumn(opponentInfo, "상대 보드", Theme.MUTED, opponentBoard, opponentTray);
        opponentColumn.setStatus("대기 중", Theme.MUTED);

        chatButton.addActionListener(event -> toggleChat());
        exitButton.addActionListener(event -> confirmExit());
        JPopupMenu emoticonMenu = createEmoticonMenu();
        emoticonButton.addActionListener(event -> {
            // 버튼 세 개의 가운데(채팅 버튼)에 맞춰 버튼 위쪽에 연다.
            Dimension size = emoticonMenu.getPreferredSize();
            int center = chatButton.getX() - emoticonButton.getX() + chatButton.getWidth() / 2;
            emoticonMenu.show(emoticonButton, center - size.width / 2, -size.height - 8);
        });

        JPanel root = new JPanel(new BorderLayout(MARGIN, 0));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
        root.add(new PlayArea(myColumn, status, opponentColumn), BorderLayout.CENTER);
        root.add(chatPanel, BorderLayout.EAST);
        setContentPane(root);
        new BlockDragHandler(this, me, myTray, myBoard, this::requestPlace);

        // 창 크기는 채팅창 없이 게임 화면만으로 정한다. 채팅창은 닫힌 채로 시작한다.
        chatPanel.setVisible(false);
        pack();
        setMinimumSize(getSize());
    }

    /** 서버 접속 상태 같은 안내 문구를 채팅 영역에 남긴다. EDT에서 호출해야 한다. */
    public void showMessage(String message) {
        chatPanel.appendNotice(message);
    }

    /** 받은 채팅을 표시한다. 이모티콘이면 보낸 사람의 점수 옆에도 잠시 보여 준다. EDT에서 호출해야 한다. */
    public void showChat(String name, String text) {
        boolean mine = name.equals(me.getName());
        chatPanel.appendChat(name, text, mine);
        Emoticon emoticon = Emoticon.of(text);
        if (emoticon != null) {
            (mine ? myInfo : opponentInfo).showEmoticon(emoticon);
        }
        if (!chatPanel.isVisible() && !mine) {
            unreadCount++;
            chatButton.setBadge(unreadCount);
        }
    }

    /** 상대가 나갔거나 다시 들어왔음을 표시한다. EDT에서 호출해야 한다. */
    public void showOpponentPresence(String name, boolean present) {
        opponentInfo.setNickname(name, false);
        if (present) {
            opponentColumn.setStatus("● 플레이 중", Theme.GREEN);
            status.showNotice(name + "님이 들어왔어요");
        } else {
            opponentColumn.setStatus("접속 종료", Theme.RED);
            status.showNotice(name + "님이 나갔어요");
        }
    }

    /** 창의 닫기 버튼을 눌렀을 때 할 일을 연결한다. */
    public void setOnClose(Runnable action) {
        onClose = action;
    }

    /** 블록 배치 요청을 서버로 보내는 동작을 연결한다. */
    public void setPlaceSender(PlaceSender sender) {
        placeSender = sender;
    }

    /** 입력한 채팅을 서버로 보내는 동작을 연결한다. 보냈으면 true를 돌려줘야 한다. */
    public void setChatSender(Predicate<String> sender) {
        chatPanel.setSender(sender);
    }

    /** START로 받은 두 플레이어의 닉네임을 표시한다. EDT에서 호출해야 한다. */
    public void setPlayerNames(String myName, String opponentName) {
        myInfo.setNickname(myName, true);
        opponentInfo.setNickname(opponentName, false);
    }

    /** START를 받으면 남은 시간 표시를 시작한다. 다시 들어온 경우에는 남은 시간부터 센다. EDT에서 호출해야 한다. */
    public void startTimer(int seconds) {
        playing = true;
        gameOver = false;
        previousScore = -1;
        opponentColumn.setStatus("● 플레이 중", Theme.GREEN);
        status.startCountdown(seconds);
    }

    /**
     * 게임 결과를 가운데에 크게 표시하고, 자세한 내용은 채팅 영역에 남긴다. 이후에는 블록을 놓을 수 없다.
     * EDT에서 호출해야 한다.
     *
     * @param win 이겼으면 true, 졌으면 false, 무승부면 null
     * @param timeUp 제한 시간이 끝나서 끝났으면 true
     */
    public void showGameOver(String headline, Boolean win, String detail, boolean timeUp) {
        playing = false;
        gameOver = true;
        opponentColumn.setStatus("게임 종료", Theme.MUTED);
        status.showResult(headline, win, timeUp);
        showMessage(detail);
    }

    /** 결과 창을 띄우고 닫힐 때까지 기다린다. EDT에서 호출해야 한다. */
    public void showResultDialog(String headline, Boolean win, String body) {
        Color color = win == null ? Theme.MUTED : win ? Theme.GREEN : Theme.RED;
        GameDialog.show(this, "게임 종료 - " + headline, headline, color, body, "확인");
    }

    /** GameState가 바뀐 뒤 두 플레이어의 점수, 보드, 트레이를 다시 그린다. EDT에서 호출해야 한다. */
    public void refresh() {
        int score = me.getScore();
        if (previousScore >= 0 && score > previousScore) {
            int gained = score - previousScore;
            status.showGain(gained, GameRules.clearedLines(gained));
        }
        previousScore = score;
        myInfo.setScore(score);
        opponentInfo.setScore(opponent.getScore());
        myBoard.repaint();
        opponentBoard.repaint();
        myTray.repaint();
        opponentTray.repaint();
    }

    private boolean requestPlace(int handIndex, int row, int col) {
        if (gameOver) {
            showMessage("게임이 끝나 더 이상 블록을 놓을 수 없습니다.");
            return true;
        }
        if (placeSender.send(handIndex, row, col)) {
            return true;
        }
        showMessage("서버에 연결되어 있지 않아 블록을 놓을 수 없습니다.");
        status.showNotice("서버에 연결되어 있지 않아요");
        return false;
    }

    /** 게임 중에는 실수로 나가지 않도록 한 번 더 묻는다. */
    private void confirmExit() {
        if (!playing || GameDialog.ask(this, "나가기", "게임에서 나갈까요?",
                "진행 중인 게임을 두고 메인 화면으로 돌아갑니다.", "나가기", "계속하기")) {
            onClose.run();
        }
    }

    /**
     * 채팅창을 열거나 닫는다. 창 크기는 그대로이고, 채팅창이 차지하는 폭만큼 게임 화면이 줄었다 늘어난다.
     */
    private void toggleChat() {
        boolean show = !chatPanel.isVisible();
        chatPanel.setVisible(show);
        chatButton.setActive(show);
        revalidate();
        if (show) {
            unreadCount = 0;
            chatButton.setBadge(0);
            chatPanel.focusInput();
        }
    }

    /** 위쪽에 이모티콘 버튼 4개, 아래쪽에 빠른 문구 버튼 3개를 둔 팝업. 누르면 바로 채팅으로 보내고 닫힌다. */
    private JPopupMenu createEmoticonMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Theme.CARD);
        menu.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER));

        JPanel faces = new JPanel(new GridLayout(1, 0, 6, 0));
        faces.setOpaque(false);
        for (Emoticon emoticon : Emoticon.values()) {
            JButton button = quickButton(menu, emoticon.text(), "");
            button.setIcon(emoticon.icon(28));
            button.setToolTipText(emoticon.label());
            button.getAccessibleContext().setAccessibleName(emoticon.label());
            faces.add(button);
        }

        JPanel phrases = new JPanel(new GridLayout(0, 1, 0, 6));
        phrases.setOpaque(false);
        for (String phrase : QUICK_PHRASES) {
            phrases.add(quickButton(menu, phrase, phrase));
        }

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(faces, BorderLayout.NORTH);
        content.add(phrases, BorderLayout.CENTER);
        menu.add(content);
        return menu;
    }

    private JButton quickButton(JPopupMenu menu, String message, String label) {
        RoundButton button = new RoundButton(label, false);
        button.setFocusable(false);
        button.addActionListener(event -> {
            menu.setVisible(false);
            chatPanel.send(message);
        });
        return button;
    }
}
