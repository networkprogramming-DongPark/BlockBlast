package blockmatch.client.ui;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.RoundRectangle2D;

/** 모서리가 둥근 입력 칸. 입력 중이면 테두리를 강조한다. */
final class RoundTextField extends JTextField {
    private static final int ARC = 16;

    RoundTextField(int columns) {
        super(columns);
        setOpaque(false);
        setFont(Theme.font(Font.PLAIN, 14f));
        setForeground(Theme.TEXT);
        setCaretColor(Theme.TEXT);
        setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent event) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent event) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        g.setColor(Theme.BACKGROUND);
        g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), ARC, ARC));
        g.setColor(isFocusOwner() ? Theme.ACCENT : Theme.CARD_BORDER);
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, ARC, ARC));
        g.dispose();
        super.paintComponent(graphics);
    }
}
