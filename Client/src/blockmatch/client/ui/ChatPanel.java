package blockmatch.client.ui;

import blockmatch.common.GameConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Predicate;

/**
 * 게임 화면 오른쪽의 채팅 영역. 서버 접속 상태와 대화 내용을 표시하고, 입력한 채팅을 보낸다.
 * 보낸 채팅도 서버가 두 플레이어에게 다시 보내 주므로, 내 화면에는 서버에서 돌아온 것만 표시한다.
 */
final class ChatPanel extends JPanel {
    static final int WIDTH = 280;

    private final JTextArea messages = new JTextArea();
    private final JTextField input = new JTextField();
    /** 채팅 내용을 서버로 보낸다. 보냈으면 true. */
    private Predicate<String> sender = text -> false;

    ChatPanel() {
        super(new BorderLayout(0, 5));
        setBorder(BorderFactory.createTitledBorder("채팅"));
        setPreferredSize(new Dimension(WIDTH, 0));

        messages.setEditable(false);
        messages.setLineWrap(true);
        add(new JScrollPane(messages), BorderLayout.CENTER);

        JButton sendButton = new JButton("전송");
        JPanel inputRow = new JPanel(new BorderLayout(5, 0));
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(sendButton, BorderLayout.EAST);
        add(inputRow, BorderLayout.SOUTH);

        // 전송 버튼과 입력창에서 Enter 둘 다 같은 동작을 한다.
        sendButton.addActionListener(event -> sendInput());
        input.addActionListener(event -> sendInput());
    }

    void setSender(Predicate<String> sender) {
        this.sender = sender;
    }

    void appendMessage(String message) {
        messages.append(message + "\n");
        messages.setCaretPosition(messages.getDocument().getLength()); // 항상 마지막 줄이 보이게 한다.
    }

    void focusInput() {
        input.requestFocusInWindow();
    }

    private void sendInput() {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > GameConfig.CHAT_MAX_LENGTH) {
            appendMessage("채팅은 " + GameConfig.CHAT_MAX_LENGTH + "자까지 보낼 수 있습니다.");
            return;
        }
        if (send(text)) {
            input.setText("");
        }
    }

    /**
     * 입력창을 거치지 않고 바로 보낸다. 이모티콘과 빠른 문구 버튼이 쓴다.
     * 보내지 못하면 채팅 영역에 안내를 남긴다.
     *
     * @return 보냈으면 true
     */
    boolean send(String text) {
        if (sender.test(text)) {
            return true;
        }
        appendMessage("서버에 연결되어 있지 않아 보내지 못했습니다.");
        return false;
    }
}
