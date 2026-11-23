package blockmatch.client.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * 보낼 수 있는 이모티콘. 채팅으로는 문자(유니코드)를 보내고, 화면에는 직접 그린 얼굴을 보여 준다.
 * Swing은 컬러 이모지 글꼴을 흑백으로 그리는 경우가 많아서 글자 대신 그림을 쓴다.
 */
enum Emoticon {
    SMILE("😊", "웃음", new Color(0xFFD45A)),
    ANGRY("😠", "화남", new Color(0xF58A6B)),
    SAD("😢", "슬픔", new Color(0xFFD45A)),
    BORED("😑", "지루함", new Color(0xFFD45A));

    private static final Color LINE = new Color(0x5A3A1B);
    private static final Color TEAR = new Color(0x58A6F5);

    private final String text;
    private final String label;
    private final Color face;

    Emoticon(String text, String label, Color face) {
        this.text = text;
        this.label = label;
        this.face = face;
    }

    /** 채팅으로 보내는 문자. */
    String text() {
        return text;
    }

    String label() {
        return label;
    }

    /** 채팅 내용이 이모티콘 하나뿐이면 그 이모티콘, 아니면 null. */
    static Emoticon of(String text) {
        for (Emoticon emoticon : values()) {
            if (emoticon.text.equals(text)) {
                return emoticon;
            }
        }
        return null;
    }

    Icon icon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = Theme.smooth(graphics);
                g.translate(x, y);
                g.scale(size / 24.0, size / 24.0);
                paint(g);
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /** 24×24 크기로 얼굴을 그린다. */
    private void paint(Graphics2D g) {
        g.setColor(face);
        g.fill(new Ellipse2D.Double(1, 1, 22, 22));
        g.setColor(LINE);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (this) {
            case SMILE -> {
                g.draw(new Arc2D.Double(5.6, 8.2, 4.4, 4.4, 20, 140, Arc2D.OPEN));
                g.draw(new Arc2D.Double(14, 8.2, 4.4, 4.4, 20, 140, Arc2D.OPEN));
                g.draw(new Arc2D.Double(7, 8.5, 10, 9, 200, 140, Arc2D.OPEN));
            }
            case ANGRY -> {
                g.draw(new Line2D.Double(5.8, 7.8, 10, 9.8));
                g.draw(new Line2D.Double(18.2, 7.8, 14, 9.8));
                g.fill(new Ellipse2D.Double(7.6, 10, 2.4, 2.4));
                g.fill(new Ellipse2D.Double(14, 10, 2.4, 2.4));
                g.draw(new Arc2D.Double(8, 15, 8, 5, 25, 130, Arc2D.OPEN));
            }
            case SAD -> {
                g.fill(new Ellipse2D.Double(7.2, 9, 2.4, 2.4));
                g.fill(new Ellipse2D.Double(14.4, 9, 2.4, 2.4));
                g.draw(new Arc2D.Double(8, 15, 8, 5, 25, 130, Arc2D.OPEN));
                g.setColor(TEAR);
                g.fill(new Ellipse2D.Double(15, 12.4, 3, 5));
            }
            case BORED -> {
                g.draw(new Line2D.Double(6.6, 10, 10, 10));
                g.draw(new Line2D.Double(14, 10, 17.4, 10));
                g.draw(new Line2D.Double(8, 16, 16, 16));
            }
        }
    }
}
