package blockmatch.common;

import java.util.Random;

/**
 * 블록 배치, 줄 제거, 점수 계산 규칙. 클라이언트와 서버가 같은 규칙을 쓰도록 Common에 둔다.
 *
 * 점수: 놓은 칸 수 × 10 + 지운 줄 수 × 100 × 지운 줄 수.
 * 한 번에 여러 줄을 지울수록 더 많은 점수를 준다. (1줄 100점, 2줄 400점, 3줄 900점)
 */
public final class GameRules {
    public static final int POINTS_PER_CELL = 10;
    public static final int POINTS_PER_LINE = 100;

    private static final BlockShape[] SHAPES = BlockShape.values();

    private final Random random;

    public GameRules(Random random) {
        this.random = random;
    }

    /** 블록의 왼쪽 위 칸을 (row, col)에 맞춰 놓을 수 있는지 판정한다. */
    public static boolean canPlace(Board board, BlockShape shape, int row, int col) {
        for (int r = 0; r < shape.getHeight(); r++) {
            for (int c = 0; c < shape.getWidth(); c++) {
                if (!shape.isFilled(r, c)) {
                    continue;
                }
                int boardRow = row + r;
                int boardCol = col + c;
                if (!isInside(boardRow, boardCol) || board.get(boardRow, boardCol) != Board.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 손에 든 블록을 보드에 놓고, 가득 찬 줄을 지운 뒤 점수를 더한다.
     * 손의 블록을 모두 쓰면 새 블록 3개를 채운다.
     *
     * @return 놓았으면 true, 그 자리에 놓을 수 없거나 빈 칸이면 false
     */
    public boolean place(PlayerState player, int handIndex, int row, int col) {
        BlockShape shape = player.getHand()[handIndex];
        Board board = player.getBoard();
        if (shape == null || !canPlace(board, shape, row, col)) {
            return false;
        }

        int placedCells = 0;
        for (int r = 0; r < shape.getHeight(); r++) {
            for (int c = 0; c < shape.getWidth(); c++) {
                if (shape.isFilled(r, c)) {
                    board.set(row + r, col + c, shape.getColorId());
                    placedCells++;
                }
            }
        }

        int clearedLines = clearFullLines(board);
        player.setScore(player.getScore() + score(placedCells, clearedLines));

        player.getHand()[handIndex] = null;
        refillHandIfEmpty(player);
        return true;
    }

    /** 손이 모두 비어 있으면 무작위 블록 3개를 채운다. 게임 시작 때도 호출한다. */
    public void refillHandIfEmpty(PlayerState player) {
        BlockShape[] hand = player.getHand();
        for (BlockShape shape : hand) {
            if (shape != null) {
                return;
            }
        }
        for (int i = 0; i < hand.length; i++) {
            hand[i] = SHAPES[random.nextInt(SHAPES.length)];
        }
    }

    public static int score(int placedCells, int clearedLines) {
        return placedCells * POINTS_PER_CELL + clearedLines * POINTS_PER_LINE * clearedLines;
    }

    /**
     * 가득 찬 가로줄과 세로줄을 모두 찾은 뒤 한꺼번에 지운다.
     * 하나씩 지우면 가로와 세로가 동시에 찬 경우 한쪽을 놓치기 때문이다.
     *
     * @return 지운 줄 수 (가로와 세로를 각각 센다)
     */
    private static int clearFullLines(Board board) {
        int size = GameConfig.BOARD_SIZE;
        boolean[] fullRows = new boolean[size];
        boolean[] fullCols = new boolean[size];
        int lines = 0;

        for (int i = 0; i < size; i++) {
            fullRows[i] = true;
            fullCols[i] = true;
            for (int j = 0; j < size; j++) {
                if (board.get(i, j) == Board.EMPTY) {
                    fullRows[i] = false;
                }
                if (board.get(j, i) == Board.EMPTY) {
                    fullCols[i] = false;
                }
            }
            lines += (fullRows[i] ? 1 : 0) + (fullCols[i] ? 1 : 0);
        }

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (fullRows[row] || fullCols[col]) {
                    board.set(row, col, Board.EMPTY);
                }
            }
        }
        return lines;
    }

    private static boolean isInside(int row, int col) {
        return row >= 0 && row < GameConfig.BOARD_SIZE && col >= 0 && col < GameConfig.BOARD_SIZE;
    }
}
