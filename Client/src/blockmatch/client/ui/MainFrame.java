package blockmatch.client.ui;

import blockmatch.common.GameConfig;
import blockmatch.common.Protocol;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 메인 화면. 게임 이름을 크게 보여 주고, 닉네임을 입력받아 게임을 시작한다.
 * 게임이 끝나거나 접속에 실패하면 이 화면으로 돌아오고, 그 이유를 아래쪽 안내 줄에 표시한다.
 */
public final class MainFrame extends JFrame {
    private static final Color TITLE = new Color(0x1F2D5A);
    private static final Color HINT = new Color(0x6B7280);
    private static final Color ERROR = new Color(0xC62828);
    /** 안내 문구가 이 폭(px)을 넘으면 다음 줄로 넘긴다. */
    private static final int STATUS_WIDTH = 340;

    /** "게임 시작"을 눌렀을 때 쓸 수 있는 닉네임을 받아 게임을 시작한다. */
    public interface StartListener {
        void start(String nickname);
    }

    private final JTextField nickname = new JTextField(14);
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final StartListener listener;

    public MainFrame(String initialNickname, StartListener listener) {
        super("BlockBlast Duo");
        this.listener = listener;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel title = label("BlockBlast Duo", Font.BOLD, 46f, TITLE);
        JLabel subtitle = label("1:1 실시간 블록 대전", Font.PLAIN, 15f, HINT);
        JLabel nicknameLabel = label("닉네임", Font.BOLD, 15f, null);
        JLabel hint = label("1~" + GameConfig.NICKNAME_MAX_LENGTH + "자, '|'는 쓸 수 없습니다", Font.PLAIN, 12f, HINT);

        nickname.setText(initialNickname == null ? "" : initialNickname);
        nickname.setFont(nickname.getFont().deriveFont(16f));
        nickname.setHorizontalAlignment(SwingConstants.CENTER);
        nickname.addActionListener(event -> tryStart()); // Enter로도 시작한다.

        JButton startButton = new JButton("게임 시작");
        startButton.setFont(startButton.getFont().deriveFont(Font.BOLD, 16f));
        startButton.setPreferredSize(new Dimension(220, 44));
        startButton.addActionListener(event -> tryStart());

        status.setFont(status.getFont().deriveFont(13f));
        showStatus(" ", false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(4, 0, 4, 0);
        content.add(title, c);
        c.insets = new Insets(0, 0, 36, 0);
        content.add(subtitle, c);
        c.insets = new Insets(4, 0, 4, 0);
        content.add(nicknameLabel, c);
        content.add(nickname, c);
        content.add(hint, c);
        c.insets = new Insets(24, 0, 8, 0);
        content.add(startButton, c);
        c.insets = new Insets(4, 0, 4, 0);
        content.add(status, c);

        setContentPane(content);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    /**
     * 메인 화면을 보여 준다. message가 있으면 안내 줄에 표시한다(지난 게임 결과나 접속 실패 이유).
     * EDT에서 호출해야 한다.
     */
    public void open(String message) {
        showStatus(message == null ? " " : message, false);
        setVisible(true);
        toFront();
        nickname.requestFocusInWindow();
    }

    private void tryStart() {
        String name = nickname.getText().trim();
        if (name.isEmpty()) {
            showStatus("닉네임을 입력하세요.", true);
        } else if (name.length() > GameConfig.NICKNAME_MAX_LENGTH) {
            showStatus("닉네임은 " + GameConfig.NICKNAME_MAX_LENGTH + "자 이하로 입력하세요.", true);
        } else if (!Protocol.isValidNickname(name)) {
            showStatus("닉네임에 '|'는 쓸 수 없습니다.", true);
        } else {
            nickname.setText(name);
            showStatus(" ", false);
            listener.start(name);
        }
    }

    /** 안내 문구를 표시한다. 접속 실패 이유처럼 긴 문구는 여러 줄로 나누고, 창 높이를 그만큼 늘린다. */
    private void showStatus(String text, boolean error) {
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        status.setText("<html><div style='text-align:center; width:" + STATUS_WIDTH + "px'>" + escaped + "</div></html>");
        status.setForeground(error ? ERROR : HINT);
        pack();
    }

    private static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(style, size));
        if (color != null) {
            label.setForeground(color);
        }
        return label;
    }
}
