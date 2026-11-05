package blockmatch.client.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** 게임 화면 오른쪽의 채팅 영역. 전송 기능은 13주차에 연결한다. */
final class ChatPanel extends JPanel {
    ChatPanel() {
        super(new BorderLayout(0, 5));
        setBorder(BorderFactory.createTitledBorder("채팅"));
        setPreferredSize(new Dimension(280, 0));

        JTextArea messages = new JTextArea();
        messages.setEditable(false);
        add(new JScrollPane(messages), BorderLayout.CENTER);

        JPanel inputRow = new JPanel(new BorderLayout(5, 0));
        inputRow.add(new JTextField(), BorderLayout.CENTER);
        inputRow.add(new JButton("전송"), BorderLayout.EAST);
        add(inputRow, BorderLayout.SOUTH);
    }
}
