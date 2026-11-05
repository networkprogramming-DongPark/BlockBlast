package blockmatch.client.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.GridLayout;

/** 상단 영역: 두 플레이어와 현재 턴, 공동 점수를 표시한다. */
final class HeaderPanel extends JPanel {
    HeaderPanel() {
        super(new GridLayout(1, 3));
        add(new JLabel("플레이어 1", SwingConstants.LEFT));
        add(new JLabel("현재 턴: -   공동 점수: 0", SwingConstants.CENTER));
        add(new JLabel("플레이어 2", SwingConstants.RIGHT));
    }
}
