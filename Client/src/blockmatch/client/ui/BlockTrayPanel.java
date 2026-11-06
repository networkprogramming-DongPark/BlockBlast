package blockmatch.client.ui;

import blockmatch.common.GameConfig;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.GridLayout;

/** 보드 아래의 블록 트레이. 지금은 빈 칸만 표시한다. */
final class BlockTrayPanel extends JPanel {
    BlockTrayPanel() {
        super(new GridLayout(1, GameConfig.HAND_SIZE, 10, 0));
        setPreferredSize(new Dimension(440, 100));
        for (int i = 1; i <= GameConfig.HAND_SIZE; i++) {
            JLabel slot = new JLabel("블록 " + i, SwingConstants.CENTER);
            slot.setBorder(BorderFactory.createEtchedBorder());
            add(slot);
        }
    }
}
