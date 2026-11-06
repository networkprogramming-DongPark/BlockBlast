package blockmatch.client.ui;

import blockmatch.common.GameConfig;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/** 플레이어 한 명의 보드. 지금은 빈 격자만 그린다. */
final class BoardPanel extends JComponent {
    BoardPanel() {
        setPreferredSize(new Dimension(440, 440));
    }

    @Override
    protected void paintComponent(Graphics g) {
        int count = GameConfig.BOARD_SIZE;
        int cell = (Math.min(getWidth(), getHeight()) - 1) / count; // 오른쪽·아래쪽 테두리 1px 확보
        int startX = (getWidth() - cell * count) / 2;
        int startY = (getHeight() - cell * count) / 2;

        g.setColor(Color.GRAY);
        for (int row = 0; row < count; row++) {
            for (int col = 0; col < count; col++) {
                g.drawRect(startX + col * cell, startY + row * cell, cell, cell);
            }
        }
    }
}
