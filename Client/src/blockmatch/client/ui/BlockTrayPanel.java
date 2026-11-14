package blockmatch.client.ui;

import blockmatch.common.BlockShape;
import blockmatch.common.GameConfig;
import blockmatch.common.PlayerState;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** 보드 아래의 블록 트레이. 플레이어가 지금 놓을 수 있는 블록 3개를 보여 준다. */
final class BlockTrayPanel extends JComponent {
    private static final int SLOT_COUNT = GameConfig.HAND_SIZE;
    private static final int GAP = 10;
    private static final int MAX_CELL = 22;
    /** 가장 긴 블록(4칸)도 칸 안에 들어가도록 칸 크기를 정한다. */
    private static final int LONGEST_SHAPE = 4;
    private static final int PADDING = 8;

    private final PlayerState player;
    /** 끌고 있는 블록의 칸. 끄는 동안 트레이에서는 숨긴다. */
    private int hiddenSlot = -1;

    BlockTrayPanel(PlayerState player) {
        this.player = player;
        setPreferredSize(new Dimension(440, 100));
        setMinimumSize(new Dimension(150, 100));
    }

    Rectangle slotBounds(int slot) {
        int width = (getWidth() - GAP * (SLOT_COUNT - 1)) / SLOT_COUNT;
        return new Rectangle(slot * (width + GAP), 0, width, getHeight() - 1);
    }

    /** 위치에 해당하는 칸 번호. 칸 밖이면 -1. */
    int slotAt(Point point) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (slotBounds(slot).contains(point)) {
                return slot;
            }
        }
        return -1;
    }

    /** 트레이에서 블록 한 칸의 크기(px). */
    int cellSize() {
        Rectangle slot = slotBounds(0);
        int fit = (Math.min(slot.width, slot.height) - PADDING * 2) / LONGEST_SHAPE;
        return Math.max(1, Math.min(MAX_CELL, fit));
    }

    /** 칸 가운데에 그린 블록의 왼쪽 위 모서리 위치. */
    Point shapeOrigin(int slot, BlockShape shape) {
        Rectangle bounds = slotBounds(slot);
        int cell = cellSize();
        return new Point(bounds.x + (bounds.width - shape.getWidth() * cell) / 2,
                bounds.y + (bounds.height - shape.getHeight() * cell) / 2);
    }

    void setHiddenSlot(int slot) {
        hiddenSlot = slot;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        BlockShape[] hand = player.getHand();
        int cell = cellSize();

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            Rectangle bounds = slotBounds(slot);
            g.setColor(Theme.SLOT_BACKGROUND);
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);
            g.setColor(Theme.SLOT_BORDER);
            g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);

            BlockShape shape = hand[slot];
            if (shape == null || slot == hiddenSlot) {
                continue;
            }
            Point origin = shapeOrigin(slot, shape);
            for (int r = 0; r < shape.getHeight(); r++) {
                for (int c = 0; c < shape.getWidth(); c++) {
                    if (shape.isFilled(r, c)) {
                        Theme.paintBlockCell(g, origin.x + c * cell, origin.y + r * cell, cell - 1,
                                Theme.blockColor(shape.getColorId()));
                    }
                }
            }
        }
        g.dispose();
    }
}
