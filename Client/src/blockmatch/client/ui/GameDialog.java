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
import java.awt.FlowLayout;
import java.awt.Font;

/** 게임 화면과 같은 모양의 확인 창. 닫을 때까지 게임 창을 조작할 수 없다. 게임 결과는 창이 아니라 게임 화면 위의 ResultOverlay로 보여 준다. */
final class GameDialog extends JDialog {
    private boolean confirmed;

    private GameDialog(JFrame owner, String title, String headline, Color headlineColor, String body,
                       String confirm, String cancel) {
        super(owner, title, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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
        content.add(Box.createVerticalStrut(18));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        if (cancel != null) {
            RoundButton cancelButton = new RoundButton(cancel, false).withSize(120, 42);
            cancelButton.addActionListener(event -> dispose());
            buttons.add(cancelButton);
        }
        RoundButton confirmButton = new RoundButton(confirm, true).withSize(150, 42);
        confirmButton.addActionListener(event -> {
            confirmed = true;
            dispose();
        });
        buttons.add(confirmButton);
        buttons.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        content.add(buttons);

        setContentPane(content);
        getRootPane().setDefaultButton(confirmButton);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    /** @return 확인을 눌렀으면 true, 취소하거나 창을 닫았으면 false */
    static boolean ask(JFrame owner, String title, String headline, String body, String confirm, String cancel) {
        GameDialog dialog = new GameDialog(owner, title, headline, Theme.TEXT, body, confirm, cancel);
        dialog.setVisible(true);
        return dialog.confirmed;
    }

    private static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        return label;
    }
}
