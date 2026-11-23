package blockmatch.client.ui;

import javax.swing.JComponent;
import java.awt.Dimension;

/**
 * 내 화면, 가운데 영역, 상대 화면을 나란히 놓는다.
 * 두 보드는 남는 공간에 맞춰 같은 크기의 정사각형으로 커지고 줄어들며, 세 영역의 위아래 끝이 항상 맞는다.
 */
final class PlayArea extends JComponent {
    private static final int GAP = 16;

    private final PlayerColumn left;
    private final MatchStatusPanel center;
    private final PlayerColumn right;

    PlayArea(PlayerColumn left, MatchStatusPanel center, PlayerColumn right) {
        this.left = left;
        this.center = center;
        this.right = right;
        add(left);
        add(center);
        add(right);
        Dimension column = left.getPreferredSize();
        setPreferredSize(new Dimension(
                column.width * 2 + center.getPreferredSize().width + GAP * 2, column.height));
    }

    @Override
    public void doLayout() {
        int centerWidth = center.getPreferredSize().width;
        int board = Math.max(0, Math.min((getWidth() - centerWidth - GAP * 2) / 2,
                getHeight() - PlayerColumn.FIXED_HEIGHT));
        int height = board + PlayerColumn.FIXED_HEIGHT;
        int x = Math.max(0, (getWidth() - board * 2 - centerWidth - GAP * 2) / 2);
        int y = Math.max(0, (getHeight() - height) / 2);
        left.setBounds(x, y, board, height);
        center.setBounds(x + board + GAP, y, centerWidth, height);
        right.setBounds(x + board + GAP * 2 + centerWidth, y, board, height);
    }
}
