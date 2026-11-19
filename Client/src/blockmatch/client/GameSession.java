package blockmatch.client;

import blockmatch.client.ui.GameFrame;
import blockmatch.common.GameConfig;
import blockmatch.common.GameState;
import blockmatch.common.Protocol;

import javax.swing.SwingUtilities;

/**
 * 서버가 보내 준 게임 진행 메시지를 화면용 GameState에 반영한다.
 *
 * 화면용 GameState는 0번이 항상 나, 1번이 항상 상대다. 서버의 플레이어 번호(WELCOME으로 받음)와 다를 수 있으므로
 * STATE를 받으면 서버 번호를 화면 번호로 바꿔서 넣는다.
 * 수신 스레드에서 호출되므로, 상태 변경과 화면 갱신은 모두 invokeLater로 EDT에 넘긴다.
 */
final class GameSession implements ServerConnection.GameListener {
    private static final int ME = 0;
    private static final int OPPONENT = 1;

    private final GameState view;
    private final GameFrame frame;
    /** 서버가 정한 내 플레이어 번호. WELCOME을 받기 전에는 -1. EDT에서만 읽고 쓴다. */
    private int myNumber = -1;

    GameSession(GameState view, GameFrame frame) {
        this.view = view;
        this.frame = frame;
    }

    @Override
    public void onWelcome(int number) {
        SwingUtilities.invokeLater(() -> myNumber = number);
    }

    @Override
    public void onStart(String firstPlayer, String secondPlayer, int seconds) {
        SwingUtilities.invokeLater(() -> {
            frame.setPlayerNames(myNumber == 0 ? firstPlayer : secondPlayer, myNumber == 0 ? secondPlayer : firstPlayer);
            frame.startTimer(seconds);
        });
    }

    @Override
    public void onState(String[] parts) {
        SwingUtilities.invokeLater(() -> applyState(parts));
    }

    @Override
    public void onGameOver(String[] parts) {
        SwingUtilities.invokeLater(() -> applyGameOver(parts));
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
            frame.showGameOver("무승부", null, "시간이 끝났습니다. 점수가 같아 무승부입니다." + scores, true);
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
        frame.showGameOver(win ? "승리!" : "패배", win, detail + scores, !noMoves);
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
