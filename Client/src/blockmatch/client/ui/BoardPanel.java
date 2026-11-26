package blockmatch.client.ui;

import blockmatch.common.BlockShape;
import blockmatch.common.Board;
import blockmatch.common.GameConfig;
import blockmatch.common.PlaceResult;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.RoundRectangle2D;

/**
 * 플레이어 한 명의 보드. 둥근 남색 판 위에 채워진 칸과, 블록을 놓을 자리의 미리보기를 그린다.
 * 줄을 지우면 BoardEffects의 효과를 위에 덧그린다.
 */
final class BoardPanel extends JComponent {
    private static final int COUNT = GameConfig.BOARD_SIZE;

    private final Board board;
    private final BoardEffects effects = new BoardEffects(this::repaint);
    private BlockShape previewShape;
    private int previewRow;
    private int previewCol;
    private boolean previewValid;

    BoardPanel(Board board) {
        this.board = board;
        setPreferredSize(new Dimension(420, 420));
    }

    /** 칸 사이 간격을 포함한 한 칸의 크기(px). */
    int cellSize() {
        int side = Math.min(getWidth(), getHeight());
        return Math.max(0, side - padding(side) * 2) / COUNT;
    }

    /** 격자 왼쪽 위 모서리의 위치. 격자는 패널 가운데에 놓인다. */
    Point gridOrigin() {
        int grid = cellSize() * COUNT;
        return new Point((getWidth() - grid) / 2, (getHeight() - grid) / 2);
    }

    /** 칸 크기가 pitch일 때 칸 사이의 간격(px). */
    static int gap(int pitch) {
        return Math.max(2, pitch / 14);
    }

    void showPreview(BlockShape shape, int row, int col, boolean valid) {
        previewShape = shape;
        previewRow = row;
        previewCol = col;
        previewValid = valid;
        repaint();
    }

    void clearPreview() {
        previewShape = null;
        repaint();
    }

    /** 줄 삭제 효과를 시작한다. 지운 줄이 없으면 아무것도 하지 않는다. */
    void playClear(PlaceResult result, int combo) {
        effects.play(result, combo);
    }

    /** 창이 닫히면 효과와 타이머가 남지 않게 정리한다. */
    @Override
    public void removeNotify() {
        effects.clear();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        int pitch = cellSize();
        // 흔들림은 그리는 위치만 옮긴다. cellSize()와 gridOrigin()은 그대로라 배치 좌표에는 영향이 없다.
        g.translate(effects.shakeX() * pitch, effects.shakeY() * pitch);
        int side = Math.min(getWidth(), getHeight());
        int arc = Math.max(16, side / 9);
        g.setColor(Theme.BOARD);
        g.fill(new RoundRectangle2D.Double((getWidth() - side) / 2.0, (getHeight() - side) / 2.0, side, side, arc, arc));

        int gap = gap(pitch);
        int size = pitch - gap;
        Point origin = gridOrigin();
        if (size > 0) {
            for (int row = 0; row < COUNT; row++) {
                for (int col = 0; col < COUNT; col++) {
                    int x = origin.x + col * pitch + gap / 2;
                    int y = origin.y + row * pitch + gap / 2;
                    int value = board.get(row, col);
                    if (value == Board.EMPTY) {
                        Theme.paintEmptyCell(g, x, y, size);
                    } else {
                        Theme.paintBlockCell(g, x, y, size, Theme.blockColor(value));
                    }
                }
            }
            effects.paintUnder(g, origin.x, origin.y, pitch, gap);
            if (previewShape != null) {
                paintPreview(g, origin, pitch, gap, size);
            }
            effects.paintOver(g, origin.x, origin.y, pitch);
        }
        g.dispose();
    }

    private void paintPreview(Graphics2D g, Point origin, int pitch, int gap, int size) {
        for (int r = 0; r < previewShape.getHeight(); r++) {
            for (int c = 0; c < previewShape.getWidth(); c++) {
                int row = previewRow + r;
                int col = previewCol + c;
                if (!previewShape.isFilled(r, c) || row < 0 || row >= COUNT || col < 0 || col >= COUNT) {
                    continue;
                }
                int x = origin.x + col * pitch + gap / 2;
                int y = origin.y + row * pitch + gap / 2;
                if (previewValid) {
                    Theme.paintValidPreview(g, x, y, size, Theme.blockColor(previewShape.getColorId()));
                } else {
                    Theme.paintInvalidPreview(g, x, y, size);
                }
            }
        }
    }

    private static int padding(int side) {
        return Math.max(6, side / 32);
    }
}
