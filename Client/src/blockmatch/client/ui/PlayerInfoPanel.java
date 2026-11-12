package blockmatch.client.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

/** 보드 위쪽에 플레이어의 닉네임과 점수를 표시한다. */
final class PlayerInfoPanel extends JPanel {
    private final JLabel score = new JLabel();

    PlayerInfoPanel(String nickname) {
        super(new BorderLayout(12, 0));

        JLabel name = new JLabel(nickname);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
        add(name, BorderLayout.WEST);

        score.setFont(score.getFont().deriveFont(Font.BOLD, 26f));
        setScore(0);
        add(score, BorderLayout.CENTER);
    }

    void setScore(int points) {
        score.setText(String.format("%,d점", points));
    }
}
