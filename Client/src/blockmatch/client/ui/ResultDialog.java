package blockmatch.client.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.BooleanSupplier;

/**
 * 게임이 끝나면 띄우는 결과 창. "다시 게임하기"와 "메인으로 돌아가기" 버튼이 있다.
 *
 * 열려 있는 동안에도 상대의 상황(한 판 더 하자고 함, 나감)에 따라 안내 문구와 버튼이 바뀌어야 하므로
 * 모달 창이 아니다. 뒤의 게임 창도 그대로 쓸 수 있어서 결과를 보면서 채팅할 수 있다.
 */
final class ResultDialog extends JDialog {
    /** 안내 문구 자리의 크기를 정하는 데 쓴다. 가장 긴 문구를 넣어 두어 문구가 바뀌어도 창 크기가 그대로이다. */
    private static final String LONGEST_STATUS = "상대방이 나갔습니다. 메인으로 돌아가 주세요.";

    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final RoundButton rematchButton = new RoundButton("다시 게임하기", true).withSize(150, 42);

    /**
     * @param onRematch 다시 게임하기를 누르면 부른다. 서버로 보냈으면 true를 돌려줘야 한다.
     * @param onMain 메인으로 돌아가기를 누르거나 창을 닫으면 부른다.
     */
    ResultDialog(JFrame owner, String headline, Color headlineColor, String body,
                 BooleanSupplier onRematch, Runnable onMain) {
        super(owner, "게임 종료 - " + headline, false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                onMain.run();
            }
        });

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(28, 44, 26, 44));
        content.add(label(headline, Font.BOLD, 30f, headlineColor));
        content.add(Box.createVerticalStrut(14));
        for (String line : body.split("\n")) {
            content.add(label(line, Font.PLAIN, 14f, Theme.MUTED));
            content.add(Box.createVerticalStrut(4));
        }
        content.add(Box.createVerticalStrut(12));

        status.setFont(Theme.font(Font.BOLD, 15f));
        status.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        status.setText(LONGEST_STATUS);
        Dimension size = status.getPreferredSize();
        status.setPreferredSize(size);
        status.setMinimumSize(size);
        status.setText(" ");
        content.add(status);
        content.add(Box.createVerticalStrut(14));

        RoundButton mainButton = new RoundButton("메인으로 돌아가기", false).withSize(160, 42);
        mainButton.addActionListener(event -> onMain.run());
        rematchButton.addActionListener(event -> {
            if (onRematch.getAsBoolean()) {
                // 서버의 확인(REMATCH)을 기다리지 않고 바로 눌린 상태로 바꿔 두 번 누르지 않게 한다.
                showWaiting();
            } else {
                showStatus("서버에 연결되어 있지 않아요.", Theme.RED);
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        buttons.add(mainButton);
        buttons.add(rematchButton);
        buttons.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        content.add(buttons);

        setContentPane(content);
        getRootPane().setDefaultButton(rematchButton);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    /** 내가 다시 하기를 눌렀고 상대를 기다린다. */
    void showWaiting() {
        rematchButton.setEnabled(false);
        rematchButton.setText("기다리는 중");
        showStatus("상대방을 기다리는 중...", Theme.ACCENT);
    }

    /** 상대가 먼저 다시 하기를 눌렀다. 나도 누르면 바로 새 게임이 시작된다. */
    void showInvite(String name) {
        if (rematchButton.isEnabled()) {
            showStatus(name + "님: 한판 더해요!", Theme.GOLD);
        }
    }

    /** 상대가 나갔다. 다시 하기는 할 수 없고 메인으로 돌아가기만 남는다. */
    void showOpponentLeft() {
        rematchButton.setEnabled(false);
        rematchButton.setText("다시 게임하기");
        getRootPane().setDefaultButton(null);
        showStatus("상대방이 나갔습니다.", Theme.RED);
    }

    private void showStatus(String text, Color color) {
        status.setForeground(color);
        status.setText(text);
    }

    private static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        return label;
    }
}
