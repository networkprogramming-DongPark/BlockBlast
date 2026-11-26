package blockmatch.common;

import java.util.ArrayList;
import java.util.List;
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
     * @return 놓았으면 얻은 점수와 지운 칸을 담은 결과, 그 자리에 놓을 수 없거나 빈 칸이면 null
     */
    public PlaceResult place(PlayerState player, int handIndex, int row, int col) {
        BlockShape shape = player.getHand()[handIndex];
        Board board = player.getBoard();
        if (shape == null || !canPlace(board, shape, row, col)) {
            return null;
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

        FullLines full = clearFullLines(board);
        int gained = score(placedCells, full.count());
        player.setScore(player.getScore() + gained);

        player.getHand()[handIndex] = null;
        refillHandIfEmpty(player);
        return new PlaceResult(gained, full.rows(), full.cols(), full.cleared());
    }

    /**
     * 손이 모두 비어 있으면 무작위 블록 3개를 채운다. 게임 시작 때도 호출한다.
     *
     * 지금 보드에 놓을 자리가 있는 모양 중에서만 고르므로, 받은 블록 3개는 각각 지금 보드에 놓을 수 있다.
     * 다만 세 블록을 모두 놓을 수 있는 순서까지는 보장하지 않는다. 앞의 블록을 잘못 놓으면 나머지 블록을
     * 놓을 곳이 없어질 수 있고, 어디에 놓을지 판단하는 것도 게임의 일부로 남겨 둔다.
     */
    public void refillHandIfEmpty(PlayerState player) {
        BlockShape[] hand = player.getHand();
        for (BlockShape shape : hand) {
            if (shape != null) {
                return;
            }
        }
        List<BlockShape> placeable = new ArrayList<>();
        for (BlockShape shape : SHAPES) {
            if (canPlaceAnywhere(player.getBoard(), shape)) {
                placeable.add(shape);
            }
        }
        // 가득 찬 줄은 놓자마자 지워지므로 보드에는 항상 빈칸이 남고, 1칸 블록(SINGLE)은 언제나 놓을 수 있다.
        // 그래도 만일을 대비해 놓을 수 있는 모양이 없으면 전체에서 고른다.
        List<BlockShape> candidates = placeable.isEmpty() ? List.of(SHAPES) : placeable;
        for (int i = 0; i < hand.length; i++) {
            hand[i] = candidates.get(random.nextInt(candidates.size()));
        }
    }

    /** 보드의 어느 자리에든 이 블록을 놓을 수 있으면 true. */
    public static boolean canPlaceAnywhere(Board board, BlockShape shape) {
        for (int row = 0; row + shape.getHeight() <= GameConfig.BOARD_SIZE; row++) {
            for (int col = 0; col + shape.getWidth() <= GameConfig.BOARD_SIZE; col++) {
                if (canPlace(board, shape, row, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 손에 남은 블록 중 하나라도 보드에 놓을 수 있으면 true. 하나도 없으면 그 플레이어는 더 진행할 수 없다. */
    public static boolean hasAnyMove(PlayerState player) {
        for (BlockShape shape : player.getHand()) {
            if (shape != null && canPlaceAnywhere(player.getBoard(), shape)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 한 번의 배치로 얻은 점수에서 지운 줄 수를 구한다. 블록은 9칸 이하라 배치 점수가 한 줄 점수(100점)보다 작으므로
     * 점수만 보고도 줄 수가 하나로 정해진다.
     */
    public static int clearedLines(int gained) {
        return (int) Math.sqrt(gained / POINTS_PER_LINE);
    }

    public static int score(int placedCells, int clearedLines) {
        return placedCells * POINTS_PER_CELL + clearedLines * POINTS_PER_LINE * clearedLines;
    }

    /**
     * 가득 찬 가로줄과 세로줄을 모두 찾은 뒤 한꺼번에 지운다.
     * 하나씩 지우면 가로와 세로가 동시에 찬 경우 한쪽을 놓치기 때문이다.
     *
     * @return 지운 줄과, 지우기 직전의 칸 색
     */
    private static FullLines clearFullLines(Board board) {
        int size = GameConfig.BOARD_SIZE;
        boolean[] fullRows = new boolean[size];
        boolean[] fullCols = new boolean[size];
        int rowMask = 0;
        int colMask = 0;

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
            rowMask |= fullRows[i] ? 1 << i : 0;
            colMask |= fullCols[i] ? 1 << i : 0;
        }

        Board cleared = new Board();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (fullRows[row] || fullCols[col]) {
                    cleared.set(row, col, board.get(row, col));
                    board.set(row, col, Board.EMPTY);
                }
            }
        }
        return new FullLines(rowMask, colMask, cleared);
    }

    private record FullLines(int rows, int cols, Board cleared) {
        int count() {
            return Integer.bitCount(rows) + Integer.bitCount(cols);
        }
    }

    private static boolean isInside(int row, int col) {
        return row >= 0 && row < GameConfig.BOARD_SIZE && col >= 0 && col < GameConfig.BOARD_SIZE;
    }
}
