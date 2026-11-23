package blockmatch.client.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * 플레이어 한 명의 화면. 위에서부터 플레이어 정보, 보드 이름과 안내, 보드, 블록 트레이를 놓는다.
 * 보드는 항상 정사각형이고, 나머지도 보드와 같은 폭으로 맞춘다. 크기와 위치는 PlayArea가 정한다.
 */
final class PlayerColumn extends JComponent {
    private static final int INFO_HEIGHT = 52;
    private static final int CAPTION_HEIGHT = 26;
    private static final int TRAY_HEIGHT = 96;
    private static final int GAP = 10;
    /** 보드를 뺀 나머지의 높이. */
    static final int FIXED_HEIGHT = INFO_HEIGHT + CAPTION_HEIGHT + TRAY_HEIGHT + GAP * 3;
    private static final int BOARD_SIZE = 420;

    private final PlayerInfoPanel info;
    private final JLabel caption;
    private final JLabel status = new JLabel(" ", SwingConstants.RIGHT);
    private final BoardPanel board;
    private final BlockTrayPanel tray;

    PlayerColumn(PlayerInfoPanel info, String captionText, Color captionColor, BoardPanel board, BlockTrayPanel tray) {
        this.info = info;
        this.board = board;
        this.tray = tray;
        caption = new JLabel(captionText);
        caption.setFont(Theme.font(Font.BOLD, 15f));
        caption.setForeground(captionColor);
        status.setFont(Theme.font(Font.PLAIN, 12.5f));
        status.setForeground(Theme.MUTED);
        add(info);
        add(caption);
        add(status);
        add(board);
        add(tray);
        setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE + FIXED_HEIGHT));
    }

    /** 보드 이름 오른쪽의 안내 문구. */
    void setStatus(String text, Color color) {
        status.setText(text);
        status.setForeground(color);
    }

    @Override
    public void doLayout() {
        int side = Math.max(0, Math.min(getWidth(), getHeight() - FIXED_HEIGHT));
        int y = 0;
        info.setBounds(0, y, side, INFO_HEIGHT);
        y += INFO_HEIGHT + GAP;
        int captionWidth = Math.min(side, caption.getPreferredSize().width);
        caption.setBounds(0, y, captionWidth, CAPTION_HEIGHT);
        status.setBounds(captionWidth, y, side - captionWidth, CAPTION_HEIGHT);
        y += CAPTION_HEIGHT + GAP;
        board.setBounds(0, y, side, side);
        y += side + GAP;
        tray.setBounds(0, y, side, TRAY_HEIGHT);
    }
}
