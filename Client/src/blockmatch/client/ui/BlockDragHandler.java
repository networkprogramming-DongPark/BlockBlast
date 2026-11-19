package blockmatch.client.ui;

import blockmatch.common.BlockShape;
import blockmatch.common.GameConfig;
import blockmatch.common.GameRules;
import blockmatch.common.PlayerState;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 내 트레이의 블록을 마우스로 끌어 내 보드에 놓는다.
 * 블록이 트레이에서 보드로 넘어가야 하므로, 끄는 블록은 창 전체를 덮는 glass pane에 그린다.
 *
 * 버튼을 놓으면 직접 배치하지 않고 서버에 배치 요청(PLACE)만 보낸다. 실제 배치와 점수 계산은 서버가 하고,
 * 서버가 보낸 STATE로 화면이 바뀐다. 놓을 수 없는 자리는 서버에 보내기 전에 걸러 낸다.
 */
final class BlockDragHandler extends MouseAdapter {
    private final PlayerState player;
    private final BlockTrayPanel tray;
    private final BoardPanel board;
    private final GameFrame.PlaceSender sender;
    private final DragLayer layer = new DragLayer();

    /** 끌고 있는 블록의 트레이 칸 번호. 끌고 있지 않으면 -1. */
    private int slot = -1;
    private BlockShape shape;
    /** 마우스로 잡은 위치. 블록 왼쪽 위 모서리에서 몇 칸 떨어졌는지(칸 단위)로 저장한다. */
    private double grabX;
    private double grabY;
    /** glass pane 좌표의 마우스 위치. */
    private Point mouse;
    /** 보드 위에서는 칸에 맞춘 미리보기만 보여 주고, 끄는 블록은 그리지 않는다. */
    private boolean overBoard;

    BlockDragHandler(JFrame frame, PlayerState player, BlockTrayPanel tray, BoardPanel board,
                     GameFrame.PlaceSender sender) {
        this.player = player;
        this.tray = tray;
        this.board = board;
        this.sender = sender;
        frame.setGlassPane(layer);
        tray.addMouseListener(this);
        tray.addMouseMotionListener(this);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int pressed = tray.slotAt(e.getPoint());
        if (!SwingUtilities.isLeftMouseButton(e) || pressed < 0 || player.getHand()[pressed] == null) {
            return;
        }
        slot = pressed;
        shape = player.getHand()[pressed];

        Point origin = tray.shapeOrigin(slot, shape);
        int cell = tray.cellSize();
        grabX = clamp((e.getX() - origin.x) / (double) cell, shape.getWidth());
        grabY = clamp((e.getY() - origin.y) / (double) cell, shape.getHeight());

        tray.setHiddenSlot(slot);
        layer.setVisible(true);
        update(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (slot >= 0) {
            update(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (slot < 0) {
            return;
        }
        Cell target = targetCell(e);
        boolean canPlace = target != null
                && GameRules.canPlace(player.getBoard(), shape, target.row(), target.col());
        int handIndex = slot;

        slot = -1;
        shape = null;
        tray.setHiddenSlot(-1);
        board.clearPreview();
        layer.setVisible(false);
        if (canPlace) {
            sender.send(handIndex, target.row(), target.col());
        }
    }

    private void update(MouseEvent e) {
        mouse = SwingUtilities.convertPoint(tray, e.getPoint(), layer);
        Cell target = targetCell(e);
        overBoard = target != null;
        if (target == null) {
            board.clearPreview();
        } else {
            board.showPreview(shape, target.row(), target.col(),
                    GameRules.canPlace(player.getBoard(), shape, target.row(), target.col()));
        }
        layer.repaint();
    }

    /** 마우스가 보드 격자 위에 있으면, 블록 왼쪽 위 칸이 놓일 보드 칸. 격자 밖이면 null. */
    private Cell targetCell(MouseEvent e) {
        Point p = SwingUtilities.convertPoint(tray, e.getPoint(), board);
        int cell = board.cellSize();
        Point origin = board.gridOrigin();
        int gridSize = cell * GameConfig.BOARD_SIZE;
        if (cell <= 0 || p.x < origin.x || p.y < origin.y || p.x >= origin.x + gridSize || p.y >= origin.y + gridSize) {
            return null;
        }
        int col = (int) Math.round((p.x - origin.x) / (double) cell - grabX);
        int row = (int) Math.round((p.y - origin.y) / (double) cell - grabY);
        return new Cell(row, col);
    }

    private static double clamp(double value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private record Cell(int row, int col) {
    }

    /** 끌고 있는 블록을 마우스 위치에 보드 칸 크기로 그린다. */
    private final class DragLayer extends JComponent {
        @Override
        protected void paintComponent(Graphics graphics) {
            if (shape == null || mouse == null || overBoard) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
            int cell = board.cellSize();
            int left = (int) Math.round(mouse.x - grabX * cell);
            int top = (int) Math.round(mouse.y - grabY * cell);
            for (int r = 0; r < shape.getHeight(); r++) {
                for (int c = 0; c < shape.getWidth(); c++) {
                    if (shape.isFilled(r, c)) {
                        Theme.paintBlockCell(g, left + c * cell, top + r * cell, cell - 1,
                                Theme.blockColor(shape.getColorId()));
                    }
                }
            }
            g.dispose();
        }
    }
}
