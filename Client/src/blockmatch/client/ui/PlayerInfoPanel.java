package blockmatch.client.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

/** 보드 위쪽에 플레이어의 닉네임과 점수를 표시한다. */
final class PlayerInfoPanel extends JPanel {
    PlayerInfoPanel(String nickname) {
        super(new BorderLayout(12, 0));

        JLabel name = new JLabel(nickname);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
        add(name, BorderLayout.WEST);

        JLabel score = new JLabel("0점");
        score.setFont(score.getFont().deriveFont(Font.BOLD, 26f));
        add(score, BorderLayout.CENTER);
    }
}
