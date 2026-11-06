package blockmatch.common;

/**
 * 게임 한 판의 전체 상태. 화면은 이 객체만 보고 그린다.
 * 두 플레이어는 차례 없이 동시에 진행한다.
 * 12주차부터는 서버가 원본을 갖고, 변경 내용을 클라이언트에 전달한다.
 */
public final class GameState {
    private final PlayerState[] players;

    public GameState(String firstPlayer, String secondPlayer) {
        players = new PlayerState[] {new PlayerState(firstPlayer), new PlayerState(secondPlayer)};
    }

    public PlayerState getPlayer(int index) {
        return players[index];
    }
}
