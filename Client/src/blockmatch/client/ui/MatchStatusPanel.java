package blockmatch.client.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 두 보드 사이의 가운데 영역. "VS" 아래에 남은 시간을, 게임이 끝나면 결과(승리/패배/무승부)를 표시한다.
 * 남은 시간은 START를 받은 시점부터 이 창에서 센다. 실제 종료는 서버의 GAMEOVER로 판단한다.
 */
final class MatchStatusPanel extends JPanel {
    private static final Color WIN = new Color(0x2E7D32);
    private static final Color LOSE = new Color(0xC62828);

    private final JLabel time = label("--:--", 22f);
    private final JLabel result = label(" ", 22f);
    /** 화면의 남은 시간을 고친다. 정확히 1초마다 불린다는 보장이 없으므로 끝나는 시각과 비교해 계산한다. */
    private final Timer ticker = new Timer(200, event -> updateTime());
    private long endMillis;

    MatchStatusPanel() {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(6, 0, 6, 0);
        add(label("VS", 24f), c);
        add(time, c);
        add(result, c);
    }

    void startCountdown(int seconds) {
        endMillis = System.currentTimeMillis() + seconds * 1000L;
        result.setText(" ");
        updateTime();
        ticker.start();
    }

    /**
     * 결과를 표시하고 시간을 멈춘다. win이 null이면 무승부.
     * 이 창의 시계는 START를 받은 뒤부터 세므로 서버보다 조금 늦다. 시간 종료로 끝났으면 00:00으로 맞춘다.
     */
    void showResult(String text, Boolean win, boolean timeUp) {
        ticker.stop();
        if (timeUp) {
            time.setText("00:00");
        }
        result.setText(text);
        result.setForeground(win == null ? getForeground() : win ? WIN : LOSE);
    }

    private void updateTime() {
        long remaining = Math.max(0, endMillis - System.currentTimeMillis());
        int seconds = (int) ((remaining + 999) / 1000);
        time.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        if (remaining == 0) {
            ticker.stop(); // 서버가 GAMEOVER를 보낼 때까지 00:00으로 둔다.
        }
    }

    private static JLabel label(String text, float size) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, size));
        return label;
    }
}
