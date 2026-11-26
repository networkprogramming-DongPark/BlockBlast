package blockmatch.client;

import blockmatch.client.ui.GameFrame;
import blockmatch.common.GameConfig;
import blockmatch.common.GameState;
import blockmatch.common.Protocol;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

/**
 * 서버가 보내 준 게임 진행 메시지를 화면용 GameState에 반영한다.
 *
 * 화면용 GameState는 0번이 항상 나, 1번이 항상 상대다. 서버의 플레이어 번호(WELCOME으로 받음)와 다를 수 있으므로
 * STATE를 받으면 서버 번호를 화면 번호로 바꿔서 넣는다.
 * 수신 스레드에서 호출되므로, 상태 변경과 화면 갱신은 모두 invokeLater로 EDT에 넘긴다.
 *
 * 게임이 끝나면 다시 게임하기와 메인으로 돌아가기 버튼이 있는 결과 창을 띄운다. 두 플레이어가 모두 다시 하기를
 * 누르면 서버가 START를 다시 보내 같은 창에서 새 게임이 이어진다. 메인으로 돌아가기를 누르거나, 입장이 거부되거나,
 * 연결이 끊기면 메인 화면으로 돌아가 그 이유를 보여 준다.
 */
final class GameSession implements ServerConnection.GameListener {
    private static final int ME = 0;
    private static final int OPPONENT = 1;

    private final GameState view;
    private final GameFrame frame;
    /** 메인 화면으로 돌아간다. 값은 메인 화면에 보여 줄 안내 문구다. */
    private final Consumer<String> backToMain;
    /** 서버가 정한 내 플레이어 번호. WELCOME을 받기 전에는 -1. EDT에서만 읽고 쓴다. */
    private int myNumber = -1;
    /** 상대가 지금 접속해 있는지. 게임 중에 상대가 나간 채 끝나면 처음부터 다시 하기를 막는 데 쓴다. EDT에서만 읽고 쓴다. */
    private boolean opponentPresent = true;

    GameSession(GameState view, GameFrame frame, Consumer<String> backToMain) {
        this.view = view;
        this.frame = frame;
        this.backToMain = backToMain;
    }

    @Override
    public void onWelcome(int number) {
        SwingUtilities.invokeLater(() -> myNumber = number);
    }

    @Override
    public void onStart(String firstPlayer, String secondPlayer, int seconds) {
        SwingUtilities.invokeLater(() -> {
            opponentPresent = true;
            frame.setPlayerNames(myNumber == 0 ? firstPlayer : secondPlayer, myNumber == 0 ? secondPlayer : firstPlayer);
            frame.startTimer(seconds);
        });
    }

    @Override
    public void onState(String[] parts) {
        SwingUtilities.invokeLater(() -> applyState(parts));
    }

    @Override
    public void onPlaced(String[] parts) {
        SwingUtilities.invokeLater(() -> applyPlaced(parts));
    }

    @Override
    public void onGameOver(String[] parts) {
        SwingUtilities.invokeLater(() -> applyGameOver(parts));
    }

    @Override
    public void onChat(String name, String text) {
        SwingUtilities.invokeLater(() -> frame.showChat(name, text));
    }

    @Override
    public void onOpponentPresence(String name, boolean present) {
        SwingUtilities.invokeLater(() -> {
            opponentPresent = present;
            frame.showOpponentPresence(name, present);
        });
    }

    @Override
    public void onRematchRequest(String name) {
        SwingUtilities.invokeLater(() -> frame.showRematchRequest(name));
    }

    @Override
    public void onJoinRejected(String message) {
        SwingUtilities.invokeLater(() -> backToMain.accept(message));
    }

    @Override
    public void onDisconnected(String message) {
        SwingUtilities.invokeLater(() -> backToMain.accept(message));
    }

    /** parts는 GAMEOVER 메시지를 나눈 것이다: [GAMEOVER, 승자번호, 사유, 점수0, 점수1] */
    private void applyGameOver(String[] parts) {
        int winner;
        int myScore;
        int opponentScore;
        try {
            winner = Integer.parseInt(parts[1]);
            myScore = Integer.parseInt(parts[3 + myNumber]);
            opponentScore = Integer.parseInt(parts[3 + (1 - myNumber)]);
        } catch (RuntimeException e) {
            frame.showMessage("잘못된 게임 종료 메시지를 받았습니다.");
            return;
        }
        String scores = " (내 점수 " + myScore + "점, 상대 점수 " + opponentScore + "점)";
        if (winner == Protocol.DRAW) {
            finish("무승부", null, "시간이 끝났습니다. 점수가 같아 무승부입니다.", scores, true);
            return;
        }
        boolean win = winner == myNumber;
        boolean noMoves = Protocol.END_NO_MOVES.equals(parts[2]);
        String detail;
        if (noMoves) {
            detail = win ? "상대가 더 이상 놓을 수 있는 블록이 없어 승리했습니다." : "놓을 수 있는 블록이 없어 패배했습니다.";
        } else {
            detail = win ? "시간이 끝났습니다. 점수가 더 높아 승리했습니다." : "시간이 끝났습니다. 점수가 낮아 패배했습니다.";
        }
        finish(win ? "승리!" : "패배", win, detail, scores, !noMoves);
    }

    /**
     * 게임 화면에 결과를 표시하고 결과 창을 띄운다. 메인으로 돌아가기를 누르면 메인 화면에도 지난 게임 결과를
     * 한 줄로 남긴다.
     */
    private void finish(String headline, Boolean win, String detail, String scores, boolean timeUp) {
        frame.showGameOver(headline, win, detail + scores, timeUp);
        String result = "지난 게임: " + headline + scores;
        frame.showResultDialog(headline, win, detail + "\n" + scores.trim(), opponentPresent,
                () -> backToMain.accept(result));
    }

    /** 놓은 플레이어의 보드에 효과를 보여 준다. 게임 상태는 뒤이어 오는 STATE가 바꾼다. */
    private void applyPlaced(String[] parts) {
        if (myNumber < 0) {
            return;
        }
        try {
            Protocol.Placed placed = Protocol.readPlaced(parts);
            frame.showPlaced(placed.player() == myNumber ? ME : OPPONENT, placed.combo(), placed.result());
        } catch (IllegalArgumentException e) {
            frame.showMessage("잘못된 배치 결과 메시지를 받았습니다: " + e.getMessage());
        }
    }

    /** parts는 STATE 메시지를 나눈 것이다: [STATE, 점수0, 보드0, 블록들0, 점수1, 보드1, 블록들1] */
    private void applyState(String[] parts) {
        if (myNumber < 0 || parts.length != 1 + 3 * GameConfig.PLAYER_COUNT) {
            frame.showMessage("잘못된 게임 상태 메시지를 받았습니다.");
            return;
        }
        try {
            for (int server = 0; server < GameConfig.PLAYER_COUNT; server++) {
                int index = 1 + 3 * server;
                int target = server == myNumber ? ME : OPPONENT;
                Protocol.readPlayer(parts[index], parts[index + 1], parts[index + 2], view.getPlayer(target));
            }
        } catch (IllegalArgumentException e) {
            frame.showMessage("잘못된 게임 상태 메시지를 받았습니다: " + e.getMessage());
        }
        frame.refresh();
    }
}
