package blockmatch.client.ui;

import blockmatch.common.BlockShape;
import blockmatch.common.GameConfig;
import blockmatch.common.Protocol;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 메인 화면. 게임 이름을 크게 보여 주고, 닉네임을 입력받아 게임을 시작한다.
 * 게임이 끝나거나 접속에 실패하면 이 화면으로 돌아오고, 그 이유를 아래쪽 안내 줄에 표시한다.
 */
public final class MainFrame extends JFrame {
    /** 안내 문구가 이 폭(px)을 넘으면 다음 줄로 넘긴다. */
    private static final int STATUS_WIDTH = 270;

    /** "게임 시작"을 눌렀을 때 쓸 수 있는 닉네임을 받아 게임을 시작한다. */
    public interface StartListener {
        void start(String nickname);
    }

    private final JTextField nickname = new RoundTextField(16);
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final StartListener listener;

    public MainFrame(String initialNickname, StartListener listener) {
        super("BlockBlast Duo");
        this.listener = listener;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel title = label("BlockBlast Duo", Font.BOLD, 50f, Theme.TEXT);
        JLabel subtitle = label("1:1 실시간 대전", Font.PLAIN, 16f, Theme.MUTED);
        JLabel nicknameLabel = label("닉네임", Font.BOLD, 15f, Theme.TEXT);
        JLabel hint = label("1~" + GameConfig.NICKNAME_MAX_LENGTH + "자, '|'는 쓸 수 없습니다", Font.PLAIN, 12f, Theme.MUTED);

        nickname.setText(initialNickname == null ? "" : initialNickname);
        nickname.setFont(Theme.font(Font.PLAIN, 17f));
        nickname.setHorizontalAlignment(SwingConstants.CENTER);
        nickname.addActionListener(event -> tryStart()); // Enter로도 시작한다.

        RoundButton startButton = new RoundButton("게임 시작", true).withSize(260, 50);
        startButton.setFont(Theme.font(Font.BOLD, 17f));
        startButton.addActionListener(event -> tryStart());

        status.setFont(Theme.font(Font.PLAIN, 13f));

        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = Theme.smooth(graphics);
                Theme.paintCard(g, getWidth(), getHeight(), 36, Theme.CARD, Theme.CARD_BORDER);
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(28, 48, 24, 48));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 0);
        card.add(nicknameLabel, c);
        card.add(nickname, c);
        card.add(hint, c);
        c.insets = new Insets(10, 0, 12, 0);
        card.add(startButton, c);
        c.insets = new Insets(0, 0, 0, 0);
        card.add(status, c);

        JPanel content = new Backdrop();
        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0;
        content.add(title, row);
        row.insets = new Insets(4, 0, 34, 0);
        content.add(subtitle, row);
        row.insets = new Insets(0, 0, 0, 0);
        content.add(card, row);

        setContentPane(content);
        showStatus(" ", false);
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

    /** 안내 문구를 표시한다. 접속 실패 이유처럼 긴 문구는 여러 줄로 나뉜다. */
    private void showStatus(String text, boolean error) {
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        status.setText("<html><div style='text-align:center; width:" + STATUS_WIDTH + "px'>" + escaped + "</div></html>");
        status.setForeground(error ? Theme.RED : Theme.MUTED);
        revalidate();
    }

    private static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        return label;
    }

    /** 가장자리에 블록을 흩어 놓은 바탕. */
    private static final class Backdrop extends JPanel {
        private static final int CELL = 38;

        Backdrop() {
            super(new GridBagLayout());
            setBackground(Theme.BACKGROUND);
            setPreferredSize(new Dimension(920, 600));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = Theme.smooth(graphics);
            int right = getWidth();
            int bottom = getHeight();
            paintShape(g, BlockShape.L, 48, 56);
            paintShape(g, BlockShape.SQUARE2, 64, bottom / 2 - 10);
            paintShape(g, BlockShape.T, 40, bottom - 132);
            paintShape(g, BlockShape.S, right - 40 - CELL * 3, 60);
            paintShape(g, BlockShape.LINE3_V, right - 72 - CELL, bottom / 2 - 50);
            paintShape(g, BlockShape.LINE4_H, right - 40 - CELL * 4, bottom - 92);
            g.dispose();
        }

        private static void paintShape(Graphics2D g, BlockShape shape, int x, int y) {
            for (int r = 0; r < shape.getHeight(); r++) {
                for (int c = 0; c < shape.getWidth(); c++) {
                    if (shape.isFilled(r, c)) {
                        Theme.paintBlockCell(g, x + c * CELL, y + r * CELL, CELL - 3, Theme.blockColor(shape.getColorId()));
                    }
                }
            }
        }
    }
}
