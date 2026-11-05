package blockmatch.common;

/**
 * 게임 한 판의 전체 상태. 화면은 이 객체만 보고 그린다.
 * 12주차부터는 서버가 원본을 갖고, 변경 내용을 클라이언트에 전달한다.
 */
public final class GameState {
    private final Board board = new Board();
    private final String[] playerNames;
    /** 현재 턴에 놓을 수 있는 블록. 이미 사용한 칸은 null이다. */
    private final BlockShape[] hand = new BlockShape[GameConfig.HAND_SIZE];
    private int currentPlayer;
    private int score;

    public GameState(String firstPlayer, String secondPlayer) {
        playerNames = new String[] {firstPlayer, secondPlayer};
    }

    public Board getBoard() {
        return board;
    }

    public String getPlayerName(int index) {
        return playerNames[index];
    }

    public BlockShape[] getHand() {
        return hand;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
