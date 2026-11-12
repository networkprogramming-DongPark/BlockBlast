package blockmatch.client.ui;

import blockmatch.common.BlockShape;
import blockmatch.common.Board;
import blockmatch.common.GameConfig;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;

/** 플레이어 한 명의 보드. 채워진 칸과 블록을 놓을 자리의 미리보기를 그린다. */
final class BoardPanel extends JComponent {
    private static final int COUNT = GameConfig.BOARD_SIZE;

    private final Board board;
    private BlockShape previewShape;
    private int previewRow;
    private int previewCol;
    private boolean previewValid;

    BoardPanel(Board board) {
        this.board = board;
        setPreferredSize(new Dimension(440, 440));
    }

    /** 한 칸의 크기(px). 오른쪽·아래쪽 테두리 1px을 확보한다. */
    int cellSize() {
        return (Math.min(getWidth(), getHeight()) - 1) / COUNT;
    }

    /** 격자 왼쪽 위 모서리의 위치. 격자는 패널 가운데에 놓인다. */
    Point gridOrigin() {
        int cell = cellSize();
        return new Point((getWidth() - cell * COUNT) / 2, (getHeight() - cell * COUNT) / 2);
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

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        int cell = cellSize();
        Point origin = gridOrigin();

        g.setColor(Theme.GRID_LINE);
        g.fillRect(origin.x, origin.y, cell * COUNT + 1, cell * COUNT + 1);
        for (int row = 0; row < COUNT; row++) {
            for (int col = 0; col < COUNT; col++) {
                int x = origin.x + col * cell + 1;
                int y = origin.y + row * cell + 1;
                int value = board.get(row, col);
                if (value == Board.EMPTY) {
                    g.setColor(Theme.EMPTY_CELL);
                    g.fillRect(x, y, cell - 1, cell - 1);
                } else {
                    Theme.paintBlockCell(g, x, y, cell - 1, Theme.blockColor(value));
                }
            }
        }

        if (previewShape != null) {
            paintPreview(g, cell, origin);
        }
    }

    private void paintPreview(Graphics2D g, int cell, Point origin) {
        Color color = previewValid ? Theme.blockColor(previewShape.getColorId()) : Theme.INVALID;
        for (int r = 0; r < previewShape.getHeight(); r++) {
            for (int c = 0; c < previewShape.getWidth(); c++) {
                int row = previewRow + r;
                int col = previewCol + c;
                if (previewShape.isFilled(r, c) && row >= 0 && row < COUNT && col >= 0 && col < COUNT) {
                    Theme.paintPreviewCell(g, origin.x + col * cell + 1, origin.y + row * cell + 1, cell - 1, color);
                }
            }
        }
    }
}
