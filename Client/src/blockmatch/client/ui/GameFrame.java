package blockmatch.client.ui;

import blockmatch.common.GameState;
import blockmatch.common.PlayerState;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.function.Predicate;

/**
 * 게임 메인 창. 내 보드와 상대 보드를 나란히 두고, 각 보드 위에 플레이어 정보를 표시한다.
 * 블록 트레이는 두 보드 아래에 각각 둔다. 채팅은 오른쪽 아래 버튼으로 열고 닫는 패널이다.
 * 창 크기는 바뀌지 않고, 채팅을 열면 그 폭만큼 게임 화면이 줄어들어 가려지는 곳 없이 모두 보인다.
 * 내 트레이의 블록을 끌어 내 보드에 놓을 수 있다. 놓는 요청은 서버로 보내고, 서버가 보낸 상태로 두 보드를 다시 그린다.
 */
public final class GameFrame extends JFrame {
    private static final int MY_COLUMN = 0;
    private static final int CENTER_COLUMN = 1;
    private static final int OPPONENT_COLUMN = 2;
    private static final int INFO_ROW = 0;
    private static final int CAPTION_ROW = 1;
    private static final int BOARD_ROW = 2;
    private static final int TRAY_ROW = 3;
    /** 화면용 GameState에서는 0번이 항상 나, 1번이 상대다. 서버 번호와의 변환은 GameSession이 한다. */
    private static final int MY_PLAYER = 0;
    private static final int OPPONENT_PLAYER = 1;

    private static final int ROOT_GAP = 10;

    /** 이모티콘 (웃음, 화남, 슬픔, 지루함). 소스 인코딩에 영향받지 않도록 유니코드 코드로 적었다. */
    private static final String[] EMOTICONS = {"😊", "😠", "😢", "😑"};
    private static final String[] EMOTICON_NAMES = {"웃음", "화남", "슬픔", "지루함"};
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
    private PlaceSender placeSender = (handIndex, row, col) -> false;
    /** 내가 보낸 채팅은 "이름: 내용"으로 돌아온다. 닫힌 동안 내가 보낸 것은 안 읽은 메시지로 세지 않는다. */
    private final String myChatPrefix;
    private final JButton chatToggle = new JButton();
    /** 채팅창이 닫힌 동안 도착한 메시지 수. 다시 열면 0으로 돌아간다. */
    private int unreadCount;

    public GameFrame(GameState state) {
        super("블록 매치");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        me = state.getPlayer(MY_PLAYER);
        opponent = state.getPlayer(OPPONENT_PLAYER);
        myChatPrefix = me.getName() + ": ";
        myInfo = new PlayerInfoPanel(me.getName() + " · 나");
        opponentInfo = new PlayerInfoPanel(opponent.getName());
        myBoard = new BoardPanel(me.getBoard());
        opponentBoard = new BoardPanel(opponent.getBoard());
        myTray = new BlockTrayPanel(me);
        opponentTray = new BlockTrayPanel(opponent);

        // 플레이어 정보와 보드의 열을 맞추기 위해 한 격자에 함께 배치한다.
        JPanel playArea = new JPanel(new GridBagLayout());
        place(playArea, myInfo, MY_COLUMN, INFO_ROW);
        place(playArea, centerLabel("블록 매치", 20f), CENTER_COLUMN, INFO_ROW);
        place(playArea, opponentInfo, OPPONENT_COLUMN, INFO_ROW);
        place(playArea, new JLabel("내 보드"), MY_COLUMN, CAPTION_ROW);
        place(playArea, new JLabel("상대 보드"), OPPONENT_COLUMN, CAPTION_ROW);
        place(playArea, myBoard, MY_COLUMN, BOARD_ROW);
        place(playArea, centerLabel("VS", 24f), CENTER_COLUMN, BOARD_ROW);
        place(playArea, opponentBoard, OPPONENT_COLUMN, BOARD_ROW);
        place(playArea, myTray, MY_COLUMN, TRAY_ROW);
        place(playArea, opponentTray, OPPONENT_COLUMN, TRAY_ROW);

        // 채팅 열기/닫기 버튼은 채팅창이 닫혀도 보이도록 창 오른쪽 아래에 따로 둔다.
        chatToggle.setFocusable(false);
        chatToggle.addActionListener(event -> toggleChat());
        updateChatToggleText();
        // 이모티콘 버튼은 채팅 열기/닫기 버튼 왼쪽에 둔다. 누르면 버튼 위로 팝업이 열린다.
        JButton emoticonButton = new JButton("이모티콘");
        emoticonButton.setFocusable(false);
        JPopupMenu emoticonMenu = createEmoticonMenu();
        emoticonButton.addActionListener(event -> {
            // 창 밖으로 나가지 않도록 팝업의 오른쪽 끝을 채팅 버튼의 오른쪽 끝에 맞춘다.
            Dimension size = emoticonMenu.getPreferredSize();
            int right = chatToggle.getX() + chatToggle.getWidth();
            emoticonMenu.show(emoticonButton, right - emoticonButton.getX() - size.width, -size.height);
        });

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bottomBar.add(emoticonButton);
        bottomBar.add(chatToggle);

        JPanel root = new JPanel(new BorderLayout(ROOT_GAP, ROOT_GAP));
        root.setBorder(BorderFactory.createEmptyBorder(ROOT_GAP, ROOT_GAP, ROOT_GAP, ROOT_GAP));
        root.add(playArea, BorderLayout.CENTER);
        root.add(chatPanel, BorderLayout.EAST);
        root.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(root);
        new BlockDragHandler(this, me, myTray, myBoard, this::requestPlace);

        // 창 크기는 채팅창 없이 게임 화면만으로 정하고, 그 뒤에 채팅창을 연다. 그러면 창 크기는 그대로이고
        // 채팅창이 차지하는 폭만큼 게임 화면이 줄어들어 가려지는 곳 없이 모두 보인다.
        chatPanel.setVisible(false);
        pack();
        setMinimumSize(getSize());
        chatPanel.setVisible(true);
        updateChatToggleText();
    }

    /** 서버 접속 상태 등을 채팅 영역에 표시한다. EDT에서 호출해야 한다. */
    public void showMessage(String message) {
        chatPanel.appendMessage(message);
        if (!chatPanel.isVisible() && !message.startsWith(myChatPrefix)) {
            unreadCount++;
            updateChatToggleText();
        }
    }

    /** 블록 배치 요청을 서버로 보내는 동작을 연결한다. */
    public void setPlaceSender(PlaceSender sender) {
        placeSender = sender;
    }

    /** START로 받은 두 플레이어의 닉네임을 표시한다. EDT에서 호출해야 한다. */
    public void setPlayerNames(String myName, String opponentName) {
        myInfo.setNickname(myName + " · 나");
        opponentInfo.setNickname(opponentName);
    }

    /** GameState가 바뀐 뒤 두 플레이어의 점수, 보드, 트레이를 다시 그린다. EDT에서 호출해야 한다. */
    public void refresh() {
        myInfo.setScore(me.getScore());
        opponentInfo.setScore(opponent.getScore());
        myBoard.repaint();
        opponentBoard.repaint();
        myTray.repaint();
        opponentTray.repaint();
    }

    private boolean requestPlace(int handIndex, int row, int col) {
        if (placeSender.send(handIndex, row, col)) {
            return true;
        }
        showMessage("서버에 연결되어 있지 않아 블록을 놓을 수 없습니다.");
        return false;
    }

    /** 입력한 채팅을 서버로 보내는 동작을 연결한다. 보냈으면 true를 돌려줘야 한다. */
    public void setChatSender(Predicate<String> sender) {
        chatPanel.setSender(sender);
    }

    /**
     * 채팅창을 열거나 닫는다. 창 크기는 그대로이고, 채팅창이 차지하는 폭만큼 게임 화면이 줄었다 늘어난다.
     */
    private void toggleChat() {
        boolean show = !chatPanel.isVisible();
        chatPanel.setVisible(show);
        if (show) {
            unreadCount = 0;
        }
        updateChatToggleText();
        if (show) {
            chatPanel.focusInput();
        }
    }

    /** 위쪽에 이모티콘 버튼 4개, 아래쪽에 빠른 문구 버튼 3개를 둔 팝업. 누르면 바로 채팅으로 보내고 닫힌다. */
    private JPopupMenu createEmoticonMenu() {
        JPopupMenu menu = new JPopupMenu();

        JPanel faces = new JPanel(new GridLayout(1, EMOTICONS.length, 6, 0));
        for (int i = 0; i < EMOTICONS.length; i++) {
            JButton button = quickButton(menu, EMOTICONS[i]);
            button.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
            button.setToolTipText(EMOTICON_NAMES[i]);
            faces.add(button);
        }

        JPanel content = new JPanel(new GridLayout(0, 1, 0, 6));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(faces);
        for (String phrase : QUICK_PHRASES) {
            content.add(quickButton(menu, phrase));
        }
        menu.add(content);
        return menu;
    }

    private JButton quickButton(JPopupMenu menu, String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.addActionListener(event -> {
            menu.setVisible(false);
            chatPanel.send(text);
        });
        return button;
    }

    private void updateChatToggleText() {
        if (chatPanel.isVisible()) {
            chatToggle.setText("채팅 닫기");
        } else {
            chatToggle.setText(unreadCount > 0 ? "채팅 열기 (" + unreadCount + ")" : "채팅 열기");
        }
    }

    private static void place(JPanel grid, Component component, int column, int row) {
        boolean center = column == CENTER_COLUMN;
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = center ? 0 : 1;
        constraints.weighty = row == BOARD_ROW ? 1 : 0;
        constraints.insets = new Insets(4, center ? 24 : 0, 4, center ? 24 : 0);
        grid.add(component, constraints);
    }

    private static JLabel centerLabel(String text, float fontSize) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, fontSize));
        return label;
    }
}
