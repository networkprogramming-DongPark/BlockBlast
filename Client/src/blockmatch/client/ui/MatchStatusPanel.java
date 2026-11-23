package blockmatch.client.ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 두 보드 사이의 가운데 영역. 위에서부터 게임 이름, "VS"와 남은 시간, 방금 얻은 점수나 게임 결과, 버튼을 놓는다.
 * 남은 시간은 START를 받은 시점부터 이 창에서 센다. 실제 종료는 서버의 GAMEOVER로 판단한다.
 */
final class MatchStatusPanel extends JPanel {
    private static final int WIDTH = 220;
    private static final int EVENT_MILLIS = 2500;
    private static final int NOTICE_MILLIS = 4000;
    /** 남은 시간이 이보다 적으면(초) 시계를 빨갛게 바꾼다. */
    private static final int HURRY_SECONDS = 10;

    private final TimePill time = new TimePill();
    private final JLabel headline = label(Font.BOLD, 26f, Theme.ACCENT);
    private final JLabel points = label(Font.BOLD, 17f, Theme.GOLD);
    private final JLabel hint = label(Font.PLAIN, 13f, Theme.MUTED);
    /** 화면의 남은 시간을 고친다. 정확히 1초마다 불린다는 보장이 없으므로 끝나는 시각과 비교해 계산한다. */
    private final Timer ticker = new Timer(200, event -> updateTime());
    private final Timer clearEvent = new Timer(EVENT_MILLIS, event -> showEvent(" ", " ", " "));
    private long endMillis;
    private boolean finished;

    MatchStatusPanel(JComponent... buttons) {
        super(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, 0));
        setMinimumSize(new Dimension(WIDTH, 0));
        clearEvent.setRepeats(false);

        JPanel top = column();
        JLabel title = label(Font.BOLD, 22f, Theme.TEXT);
        title.setText("블록 매치");
        JLabel subtitle = label(Font.PLAIN, 12.5f, Theme.MUTED);
        subtitle.setText("1:1 실시간 대전");
        JLabel versus = label(Font.BOLD, 17f, Theme.MUTED);
        versus.setText("VS");
        top.add(title);
        top.add(subtitle);
        top.add(Box.createVerticalStrut(34));
        top.add(versus);
        top.add(Box.createVerticalStrut(10));
        top.add(time);

        JPanel middle = column();
        middle.add(Box.createVerticalGlue());
        middle.add(headline);
        middle.add(Box.createVerticalStrut(6));
        middle.add(points);
        middle.add(Box.createVerticalStrut(6));
        middle.add(hint);
        middle.add(Box.createVerticalGlue());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        bottom.setOpaque(false);
        for (JComponent button : buttons) {
            bottom.add(button);
        }

        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        showEvent(" ", " ", "상대를 기다리는 중이에요");
    }

    void startCountdown(int seconds) {
        endMillis = System.currentTimeMillis() + seconds * 1000L;
        finished = false;
        showEvent(" ", " ", " ");
        updateTime();
        ticker.start();
    }

    /** 방금 얻은 점수와 지운 줄 수를 잠시 보여 준다. */
    void showGain(int gained, int lines) {
        if (finished) {
            return;
        }
        String title = lines == 0 ? " " : lines == 1 ? "1줄 제거!" : lines + "줄 동시 제거!";
        String cheer = lines == 0 ? " " : lines == 1 ? "좋아요! 계속 이어가요" : "대단해요! 계속 이어가요";
        showEvent(title, String.format("+%,d점", gained), cheer);
        restartClear(EVENT_MILLIS);
    }

    /** 안내 문구를 잠시 보여 준다. */
    void showNotice(String text) {
        if (finished) {
            return;
        }
        showEvent(" ", " ", text);
        restartClear(NOTICE_MILLIS);
    }

    /**
     * 결과를 표시하고 시간을 멈춘다. win이 null이면 무승부.
     * 이 창의 시계는 START를 받은 뒤부터 세므로 서버보다 조금 늦다. 시간 종료로 끝났으면 00:00으로 맞춘다.
     */
    void showResult(String text, Boolean win, boolean timeUp) {
        ticker.stop();
        clearEvent.stop();
        finished = true;
        if (timeUp) {
            time.setText("00:00");
        }
        showEvent(text, " ", " ");
        headline.setForeground(win == null ? Theme.MUTED : win ? Theme.GREEN : Theme.RED);
    }

    /** 창이 닫히면 타이머가 남아 돌지 않게 멈춘다. */
    @Override
    public void removeNotify() {
        ticker.stop();
        clearEvent.stop();
        super.removeNotify();
    }

    private void showEvent(String title, String gained, String text) {
        headline.setForeground(Theme.ACCENT);
        headline.setText(title);
        points.setText(gained);
        // 긴 안내 문구는 가운데 영역의 폭에 맞춰 여러 줄로 나눈다.
        hint.setText(text.isBlank() ? " "
                : "<html><div style='text-align:center'>" + text.replace("&", "&amp;").replace("<", "&lt;") + "</div></html>");
    }

    private void restartClear(int millis) {
        clearEvent.setInitialDelay(millis);
        clearEvent.restart();
    }

    private void updateTime() {
        long remaining = Math.max(0, endMillis - System.currentTimeMillis());
        int seconds = (int) ((remaining + 999) / 1000);
        time.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        time.setHurry(seconds <= HURRY_SECONDS);
        if (remaining == 0) {
            ticker.stop(); // 서버가 GAMEOVER를 보낼 때까지 00:00으로 둔다.
        }
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static JLabel label(int style, float size, Color color) {
        JLabel label = new JLabel(" ", SwingConstants.CENTER);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        label.setAlignmentX(CENTER_ALIGNMENT);
        return label;
    }

    /** 남은 시간을 표시하는 알약 모양 배지. */
    private static final class TimePill extends JLabel {
        private boolean hurry;

        TimePill() {
            super("--:--", SwingConstants.CENTER);
            setFont(Theme.font(Font.BOLD, 24f));
            setForeground(Color.WHITE);
            setAlignmentX(CENTER_ALIGNMENT);
            Dimension size = new Dimension(136, 56);
            setPreferredSize(size);
            setMaximumSize(size);
        }

        void setHurry(boolean hurry) {
            this.hurry = hurry;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Theme.smooth(graphics);
            g.setColor(hurry ? Theme.RED : Theme.BOARD);
            // 화면 배율이 125%처럼 소수일 때 끝이 잘리지 않도록 1px 안쪽에 그린다.
            int arc = getHeight() - 2;
            g.fill(new RoundRectangle2D.Double(1, 1, getWidth() - 2, arc, arc, arc));
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
