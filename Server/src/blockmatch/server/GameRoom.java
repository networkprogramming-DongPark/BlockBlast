package blockmatch.server;

import blockmatch.common.GameConfig;
import blockmatch.common.GameRules;
import blockmatch.common.GameState;
import blockmatch.common.Protocol;

import java.util.Random;

/**
 * 게임 한 판. 서버가 갖는 게임 상태의 원본이다.
 * 클라이언트의 배치 요청은 여기서 GameRules로 판정하고, 결과는 STATE 메시지로 두 클라이언트에 보낸다.
 *
 * 게임은 두 가지 경우에 끝난다.
 * 1. 제한 시간이 끝나면 점수가 높은 플레이어가 이긴다. 같으면 무승부다.
 * 2. 시간이 끝나기 전에 한 플레이어가 남은 블록을 하나도 놓을 수 없게 되면 그 즉시 그 플레이어가 진다.
 *
 * 스레드 안전하지 않으므로 GameServer의 synchronized 메서드 안에서만 쓴다.
 */
final class GameRoom {
    private final GameState state;
    private final GameRules rules = new GameRules(new Random());
    /** 제한 시간이 끝나는 시각(System.currentTimeMillis 기준). */
    private final long endMillis;
    private boolean over;
    private String gameOverMessage;

    GameRoom(String firstPlayer, String secondPlayer, int timeLimitSeconds) {
        state = new GameState(firstPlayer, secondPlayer);
        for (int i = 0; i < GameConfig.PLAYER_COUNT; i++) {
            rules.refillHandIfEmpty(state.getPlayer(i));
        }
        endMillis = System.currentTimeMillis() + timeLimitSeconds * 1000L;
    }

    /**
     * slot번 플레이어의 블록을 그 플레이어의 보드에 놓는다.
     * 놓은 뒤 그 플레이어가 남은 블록을 하나도 놓을 수 없으면 게임을 끝내고 상대를 승자로 한다.
     *
     * @return 놓았으면 true, 게임이 끝났거나 규칙상 놓을 수 없으면 false (상태는 바뀌지 않음)
     */
    boolean place(int slot, int handIndex, int row, int col) {
        if (over || !rules.place(state.getPlayer(slot), handIndex, row, col)) {
            return false;
        }
        // 블록 3개를 다 쓰면 놓을 수 있는 블록으로 다시 채워지므로, 여기서 막히는 경우는 블록이 남아 있을 때뿐이다.
        if (!GameRules.hasAnyMove(state.getPlayer(slot))) {
            finish(otherSlot(slot), Protocol.END_NO_MOVES);
        }
        return true;
    }

    /**
     * 제한 시간이 끝났을 때 부른다. 점수가 높은 플레이어가 이기고, 같으면 무승부다.
     *
     * @return 이번 호출로 게임이 끝났으면 true, 이미 끝나 있었으면 false
     */
    boolean timeUp() {
        if (over) {
            return false;
        }
        int first = state.getPlayer(0).getScore();
        int second = state.getPlayer(1).getScore();
        finish(first == second ? Protocol.DRAW : first > second ? 0 : 1, Protocol.END_TIME_UP);
        return true;
    }

    boolean isOver() {
        return over;
    }

    /** 남은 시간(초). 재접속한 플레이어에게 보내는 START에 쓴다. */
    int remainingSeconds() {
        long remaining = endMillis - System.currentTimeMillis();
        return (int) Math.max(0, (remaining + 999) / 1000);
    }

    String stateMessage() {
        return Protocol.encodeState(state);
    }

    /** 게임이 끝났으면 GAMEOVER 메시지, 아니면 null. */
    String gameOverMessage() {
        return gameOverMessage;
    }

    private void finish(int winner, String reason) {
        over = true;
        gameOverMessage = Protocol.encode(Protocol.GAMEOVER, winner, reason,
                state.getPlayer(0).getScore(), state.getPlayer(1).getScore());
    }

    private static int otherSlot(int slot) {
        return GameConfig.PLAYER_COUNT - 1 - slot;
    }
}
