package blockmatch.client.ui;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 게임 메인 창. 내 보드와 상대 보드를 나란히 두고, 각 보드 위에 플레이어 정보를 표시한다.
 * 블록 트레이는 내 보드 아래에, 채팅은 오른쪽에 둔다.
 */
public final class GameFrame extends JFrame {
    private static final int MY_COLUMN = 0;
    private static final int CENTER_COLUMN = 1;
    private static final int OPPONENT_COLUMN = 2;
    private static final int INFO_ROW = 0;
    private static final int CAPTION_ROW = 1;
    private static final int BOARD_ROW = 2;
    private static final int TRAY_ROW = 3;

    public GameFrame() {
        super("블록 매치");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 플레이어 정보와 보드의 열을 맞추기 위해 한 격자에 함께 배치한다.
        JPanel playArea = new JPanel(new GridBagLayout());
        place(playArea, new PlayerInfoPanel("플레이어 1 · 나"), MY_COLUMN, INFO_ROW);
        place(playArea, centerLabel("블록 매치", 20f), CENTER_COLUMN, INFO_ROW);
        place(playArea, new PlayerInfoPanel("플레이어 2"), OPPONENT_COLUMN, INFO_ROW);
        place(playArea, new JLabel("내 보드"), MY_COLUMN, CAPTION_ROW);
        place(playArea, new JLabel("상대 보드"), OPPONENT_COLUMN, CAPTION_ROW);
        place(playArea, new BoardPanel(), MY_COLUMN, BOARD_ROW);
        place(playArea, centerLabel("VS", 24f), CENTER_COLUMN, BOARD_ROW);
        place(playArea, new BoardPanel(), OPPONENT_COLUMN, BOARD_ROW);
        place(playArea, new BlockTrayPanel(), MY_COLUMN, TRAY_ROW);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(playArea, BorderLayout.CENTER);
        root.add(new ChatPanel(), BorderLayout.EAST);

        setContentPane(root);
        pack();
        setMinimumSize(getSize());
    }

    private static void place(JPanel grid, Component component, int column, int row) {
        boolean center = column == CENTER_COLUMN;
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = center ? 0 : 1;
        constraints.weighty = row == BOARD_ROW ? 1 : 0;
        constraints.insets = new Insets(4, center ? 24 : 0, 4, center ? 24 : 0);
        grid.add(component, constraints);
    }

    private static JLabel centerLabel(String text, float fontSize) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, fontSize));
        return label;
    }
}
