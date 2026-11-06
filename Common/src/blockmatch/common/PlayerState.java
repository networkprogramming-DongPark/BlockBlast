package blockmatch.common;

/** 플레이어 한 명의 상태. 플레이어마다 자기 보드, 블록, 점수를 갖는다. */
public final class PlayerState {
    private final String name;
    private final Board board = new Board();
    /** 지금 놓을 수 있는 블록. 이미 사용한 칸은 null이다. */
    private final BlockShape[] hand = new BlockShape[GameConfig.HAND_SIZE];
    private int score;

    PlayerState(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Board getBoard() {
        return board;
    }

    public BlockShape[] getHand() {
        return hand;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
