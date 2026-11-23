package blockmatch.client.ui;

import blockmatch.common.BlockShape;
import blockmatch.common.GameConfig;
import blockmatch.common.PlayerState;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** 보드 아래의 블록 트레이. 플레이어가 지금 놓을 수 있는 블록 3개를 흰 카드 세 장에 보여 준다. */
final class BlockTrayPanel extends JComponent {
    private static final int SLOT_COUNT = GameConfig.HAND_SIZE;
    private static final int GAP = 10;
    private static final int MAX_CELL = 19;
    /** 가장 긴 블록(4칸)도 칸 안에 들어가도록 칸 크기를 정한다. */
    private static final int LONGEST_SHAPE = 4;
    private static final int PADDING = 9;

    private final PlayerState player;
    /** 끌고 있는 블록의 칸. 끄는 동안 트레이에서는 블록을 숨기고 칸을 강조한다. */
    private int hiddenSlot = -1;
    /** 마우스가 올라가 있는 칸. 내 트레이에서만 쓴다. */
    private int hoverSlot = -1;

    /** @param draggable 내 트레이면 true. 마우스를 올린 칸을 강조하고 손 모양 커서를 보여 준다. */
    BlockTrayPanel(PlayerState player, boolean draggable) {
        this.player = player;
        setPreferredSize(new Dimension(420, 96));
        if (draggable) {
            MouseAdapter hover = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    setHoverSlot(slotAt(event.getPoint()));
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    setHoverSlot(-1);
                }
            };
            addMouseListener(hover);
            addMouseMotionListener(hover);
        }
    }

    Rectangle slotBounds(int slot) {
        int width = (getWidth() - GAP * (SLOT_COUNT - 1)) / SLOT_COUNT;
        return new Rectangle(slot * (width + GAP), 0, width, getHeight());
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

    private void setHoverSlot(int slot) {
        boolean usable = slot >= 0 && player.getHand()[slot] != null;
        int next = usable ? slot : -1;
        if (next != hoverSlot) {
            hoverSlot = next;
            setCursor(Cursor.getPredefinedCursor(usable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            BlockShape shape = player.getHand()[slot];
            boolean dragging = slot == hiddenSlot;
            paintSlot(g, slotBounds(slot), dragging || (slot == hoverSlot && shape != null));
            if (shape != null && !dragging) {
                paintShape(g, slot, shape);
            }
        }
        g.dispose();
    }

    private static void paintSlot(Graphics2D g, Rectangle bounds, boolean highlight) {
        RoundRectangle2D card = new RoundRectangle2D.Double(
                bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2, 18, 18);
        g.setColor(highlight ? Theme.ACCENT_SOFT : Theme.CARD);
        g.fill(card);
        Color border = highlight ? Theme.ACCENT : Theme.CARD_BORDER;
        g.setColor(border);
        g.setStroke(new BasicStroke(highlight ? 2f : 1f));
        g.draw(card);
    }

    private void paintShape(Graphics2D g, int slot, BlockShape shape) {
        Point origin = shapeOrigin(slot, shape);
        int cell = cellSize();
        for (int r = 0; r < shape.getHeight(); r++) {
            for (int c = 0; c < shape.getWidth(); c++) {
                if (shape.isFilled(r, c)) {
                    Theme.paintBlockCell(g, origin.x + c * cell, origin.y + r * cell, Math.max(1, cell - 1),
                            Theme.blockColor(shape.getColorId()));
                }
            }
        }
    }
}
