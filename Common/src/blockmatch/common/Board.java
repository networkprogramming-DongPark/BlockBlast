package blockmatch.common;

/**
 * 두 플레이어가 공유하는 8×8 보드.
 * 각 칸은 EMPTY 또는 블록의 색상 번호(BlockShape#getColorId)를 저장한다.
 */
public final class Board {
    public static final int EMPTY = 0;

    private final int[][] cells = new int[GameConfig.BOARD_SIZE][GameConfig.BOARD_SIZE];

    public int get(int row, int col) {
        return cells[row][col];
    }

    public void set(int row, int col, int value) {
        cells[row][col] = value;
    }
}
