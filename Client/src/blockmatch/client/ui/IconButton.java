package blockmatch.client.ui;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** 가운데 아래의 흰 사각 버튼. 아이콘을 선으로 그리고, 안 읽은 메시지 수를 오른쪽 위에 표시할 수 있다. */
final class IconButton extends JButton {
    enum Kind { EMOTICON, CHAT, EXIT }

    private static final int SIZE = 54;
    private static final int ICON = 24;

    private final Kind kind;
    private int badge;
    private boolean active;

    IconButton(Kind kind, String description) {
        this.kind = kind;
        setToolTipText(description);
        getAccessibleContext().setAccessibleName(description);
        setPreferredSize(new Dimension(SIZE, SIZE));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFocusable(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** 오른쪽 위에 표시할 숫자. 0이면 표시하지 않는다. */
    void setBadge(int count) {
        badge = count;
        repaint();
    }

    /** 켜진 상태(채팅창이 열려 있음 등)를 파란 테두리로 표시한다. */
    void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        boolean highlight = active || getModel().isRollover();
        Theme.paintCard(g, getWidth(), getHeight(), 24, highlight ? Theme.ACCENT_SOFT : Theme.CARD,
                active ? Theme.ACCENT : null);

        Graphics2D icon = (Graphics2D) g.create();
        icon.translate((getWidth() - ICON) / 2.0, (getHeight() - 3 - ICON) / 2.0);
        icon.setColor(Theme.TEXT);
        icon.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case EMOTICON -> paintFace(icon);
            case CHAT -> paintBubble(icon);
            case EXIT -> paintDoor(icon);
        }
        icon.dispose();

        if (badge > 0) {
            paintBadge(g);
        }
        g.dispose();
    }

    private static void paintFace(Graphics2D g) {
        g.draw(new Ellipse2D.Double(2, 2, 20, 20));
        g.fill(new Ellipse2D.Double(7.4, 8.2, 2.6, 2.6));
        g.fill(new Ellipse2D.Double(14, 8.2, 2.6, 2.6));
        g.draw(new Arc2D.Double(7, 7.5, 10, 9.5, 200, 140, Arc2D.OPEN));
    }

    private static void paintBubble(Graphics2D g) {
        Path2D bubble = new Path2D.Double();
        bubble.append(new Arc2D.Double(2.5, 2, 19.5, 18.5, 232, -342, Arc2D.OPEN), false);
        bubble.lineTo(3, 21.5);
        bubble.closePath();
        g.draw(bubble);
    }

    private static void paintDoor(Graphics2D g) {
        Path2D door = new Path2D.Double();
        door.moveTo(11, 3);
        door.lineTo(5, 3);
        door.lineTo(5, 21);
        door.lineTo(11, 21);
        g.draw(door);
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(10, 12);
        arrow.lineTo(21, 12);
        arrow.moveTo(17, 8);
        arrow.lineTo(21, 12);
        arrow.lineTo(17, 16);
        g.draw(arrow);
    }

    private void paintBadge(Graphics2D g) {
        String text = badge > 99 ? "99+" : String.valueOf(badge);
        g.setFont(Theme.font(Font.BOLD, 11f));
        FontMetrics metrics = g.getFontMetrics();
        int height = 18;
        int width = Math.max(height, metrics.stringWidth(text) + 10);
        int x = getWidth() - width - 1;
        int y = 1;
        g.setColor(Theme.RED);
        g.fill(new RoundRectangle2D.Double(x, y, width, height, height, height));
        g.setColor(Color.WHITE);
        g.drawString(text, x + (width - metrics.stringWidth(text)) / 2,
                y + (height - metrics.getHeight()) / 2 + metrics.getAscent());
    }
}
