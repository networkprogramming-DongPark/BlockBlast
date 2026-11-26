package blockmatch.client.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** 모서리가 둥근 버튼. 강조 버튼(남색 바탕)과 보통 버튼(흰 바탕) 두 가지로 쓴다. */
final class RoundButton extends JButton {
    private final boolean primary;

    RoundButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setFont(Theme.font(Font.BOLD, 14f));
        setForeground(primary ? Color.WHITE : Theme.TEXT);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    RoundButton withSize(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        return this;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Color base = primary ? Theme.BOARD : Theme.CARD;
        Color fill = !isEnabled() ? Theme.CARD_BORDER : getModel().isPressed() ? Theme.mix(base, Color.BLACK, 0.12)
                : getModel().isRollover() ? (primary ? Theme.mix(base, Color.WHITE, 0.14) : Theme.ACCENT_SOFT) : base;
        Graphics2D g = Theme.smooth(graphics);
        int arc = Math.min(18, getHeight());
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
        if (!primary || isFocusOwner()) {
            g.setColor(isFocusOwner() ? Theme.ACCENT : Theme.CARD_BORDER);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, arc, arc));
        }
        if (!isEnabled()) {
            // 누를 수 없는 버튼은 회색 바탕에 흐린 글자로 직접 그린다. (기본 모양은 글자에 그림자가 생긴다)
            g.setFont(getFont());
            g.setColor(Theme.MUTED);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(getText(), (getWidth() - metrics.stringWidth(getText())) / 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
            return;
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
