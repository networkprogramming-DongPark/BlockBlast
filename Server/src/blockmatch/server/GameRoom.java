package blockmatch.server;

import blockmatch.common.GameConfig;
import blockmatch.common.GameRules;
import blockmatch.common.GameState;
import blockmatch.common.Protocol;

import java.util.Random;

/**
 * 게임 한 판. 서버가 갖는 게임 상태의 원본이다.
 * 클라이언트의 배치 요청은 여기서 GameRules로 판정하고, 결과는 STATE 메시지로 두 클라이언트에 보낸다.
 * 스레드 안전하지 않으므로 GameServer의 synchronized 메서드 안에서만 쓴다.
 */
final class GameRoom {
    private final GameState state;
    private final GameRules rules = new GameRules(new Random());

    GameRoom(String firstPlayer, String secondPlayer) {
        state = new GameState(firstPlayer, secondPlayer);
        for (int i = 0; i < GameConfig.PLAYER_COUNT; i++) {
            rules.refillHandIfEmpty(state.getPlayer(i));
        }
    }

    /**
     * slot번 플레이어의 블록을 그 플레이어의 보드에 놓는다.
     *
     * @return 놓았으면 true, 규칙상 놓을 수 없으면 false (상태는 바뀌지 않음)
     */
    boolean place(int slot, int handIndex, int row, int col) {
        return rules.place(state.getPlayer(slot), handIndex, row, col);
    }

    String stateMessage() {
        return Protocol.encodeState(state);
    }
}
