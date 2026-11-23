package blockmatch.client.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * 보드 위쪽의 플레이어 정보. 왼쪽부터 아바타, 닉네임, 점수를 표시한다.
 * 이모티콘을 보내면 점수 오른쪽에 말풍선처럼 잠시 보여 준다. 자리가 좁으면 닉네임을 줄여서(…) 표시한다.
 */
final class PlayerInfoPanel extends JComponent {
    private static final int AVATAR = 42;
    private static final int BUBBLE = 42;
    private static final int GAP = 10;
    private static final int EMOTICON_MILLIS = 4000;

    private final JLabel name = new JLabel();
    private final JLabel score = new JLabel();
    private final Color avatarColor;
    private final Timer hideEmoticon = new Timer(EMOTICON_MILLIS, event -> setEmoticon(null));
    private String initial = "";
    private Emoticon emoticon;

    PlayerInfoPanel(String nickname, boolean mine, Color avatarColor) {
        this.avatarColor = avatarColor;
        setPreferredSize(new Dimension(420, 52));
        name.setFont(Theme.font(Font.BOLD, 15f));
        name.setForeground(Theme.TEXT);
        score.setFont(Theme.font(Font.BOLD, 24f));
        score.setForeground(Theme.TEXT);
        add(name);
        add(score);
        hideEmoticon.setRepeats(false);
        setNickname(nickname, mine);
        setScore(0);
    }

    void setNickname(String nickname, boolean mine) {
        name.setText(mine ? nickname + " · 나" : nickname);
        name.setToolTipText(nickname);
        initial = nickname.isEmpty() ? "" : nickname.substring(0, nickname.offsetByCodePoints(0, 1));
        revalidate();
        repaint();
    }

    void setScore(int points) {
        score.setText(String.format("%,d점", points));
        revalidate();
        repaint();
    }

    /** 이모티콘을 잠시 보여 준다. */
    void showEmoticon(Emoticon sent) {
        setEmoticon(sent);
        hideEmoticon.restart();
    }

    private void setEmoticon(Emoticon emoticon) {
        this.emoticon = emoticon;
        repaint();
    }

    @Override
    public void doLayout() {
        int height = getHeight();
        Dimension scoreSize = score.getPreferredSize();
        int left = AVATAR + GAP;
        int room = getWidth() - left - GAP - scoreSize.width - GAP - BUBBLE;
        int nameWidth = Math.max(0, Math.min(name.getPreferredSize().width, room));
        name.setBounds(left, 0, nameWidth, height);
        score.setBounds(left + nameWidth + GAP, 0, scoreSize.width, height);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        int top = (getHeight() - AVATAR) / 2;
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(0, top, AVATAR, AVATAR));
        g.setColor(avatarColor);
        g.fill(new Ellipse2D.Double(2, top + 2, AVATAR - 4, AVATAR - 4));
        g.setColor(Color.WHITE);
        g.setFont(Theme.font(Font.BOLD, 18f));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(initial, (AVATAR - metrics.stringWidth(initial)) / 2f,
                top + (AVATAR - metrics.getHeight()) / 2f + metrics.getAscent());

        if (emoticon != null) {
            g.translate(score.getX() + score.getWidth() + GAP, (getHeight() - BUBBLE) / 2);
            Theme.paintCard(g, BUBBLE, BUBBLE, 18, Theme.CARD, null);
            emoticon.icon(26).paintIcon(this, g, (BUBBLE - 26) / 2, (BUBBLE - 3 - 26) / 2);
        }
        g.dispose();
    }
}
