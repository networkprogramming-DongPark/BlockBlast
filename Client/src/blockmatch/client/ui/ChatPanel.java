package blockmatch.client.ui;

import blockmatch.common.GameConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Predicate;

/**
 * 게임 화면 오른쪽의 채팅 영역. 서버 접속 상태와 대화 내용을 표시하고, 입력한 채팅을 보낸다.
 * 보낸 채팅도 서버가 두 플레이어에게 다시 보내 주므로, 내 화면에는 서버에서 돌아온 것만 표시한다.
 */
final class ChatPanel extends JPanel {
    static final int WIDTH = 280;

    private final JTextPane messages = new JTextPane();
    private final RoundTextField input = new RoundTextField(0);
    /** 채팅 내용을 서버로 보낸다. 보냈으면 true. */
    private Predicate<String> sender = text -> false;

    ChatPanel() {
        super(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 19, 16));
        setPreferredSize(new Dimension(WIDTH, 0));

        JLabel title = new JLabel("채팅");
        title.setFont(Theme.font(Font.BOLD, 15f));
        title.setForeground(Theme.TEXT);
        add(title, BorderLayout.NORTH);

        messages.setEditorKit(new WrapEditorKit());
        messages.setEditable(false);
        messages.setOpaque(false);
        // 이모지 등 글꼴에 없는 글자도 다른 글꼴로 대신 보이도록 자바 논리 글꼴을 쓴다.
        messages.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        messages.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane scroll = new JScrollPane(messages, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        add(scroll, BorderLayout.CENTER);

        RoundButton sendButton = new RoundButton("전송", true).withSize(64, 38);
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
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

    /** 접속 상태 같은 안내 문구를 회색으로 남긴다. */
    void appendNotice(String text) {
        append(style(Theme.MUTED, false), text + "\n");
    }

    /** 대화 한 줄을 남긴다. 이모티콘 하나만 보낸 것은 그림으로 보여 준다. */
    void appendChat(String name, String text, boolean mine) {
        append(style(mine ? Theme.ACCENT : Theme.TEXT, true), name + ": ");
        Emoticon emoticon = Emoticon.of(text);
        if (emoticon == null) {
            append(style(Theme.TEXT, false), text + "\n");
            return;
        }
        MutableAttributeSet picture = style(Theme.TEXT, false);
        StyleConstants.setIcon(picture, emoticon.icon(20));
        append(picture, text);
        append(style(Theme.TEXT, false), "\n");
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
            appendNotice("채팅은 " + GameConfig.CHAT_MAX_LENGTH + "자까지 보낼 수 있습니다.");
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
        appendNotice("서버에 연결되어 있지 않아 보내지 못했습니다.");
        return false;
    }

    private void append(MutableAttributeSet style, String text) {
        StyledDocument document = messages.getStyledDocument();
        try {
            document.insertString(document.getLength(), text, style);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e); // 항상 문서 끝에 넣으므로 일어나지 않는다.
        }
        messages.setCaretPosition(document.getLength()); // 항상 마지막 줄이 보이게 한다.
    }

    private static MutableAttributeSet style(Color color, boolean bold) {
        MutableAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, bold);
        return style;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Theme.smooth(graphics);
        Theme.paintCard(g, getWidth(), getHeight(), 28, Theme.CARD, Theme.CARD_BORDER);
        g.dispose();
    }

    /** 띄어쓰기 없이 긴 글도 채팅 영역의 폭에서 줄을 바꾸게 한다. */
    private static final class WrapEditorKit extends StyledEditorKit {
        @Override
        public ViewFactory getViewFactory() {
            ViewFactory base = super.getViewFactory();
            return element -> isText(element) ? new WrapLabelView(element) : base.create(element);
        }

        private static boolean isText(Element element) {
            return AbstractDocument.ContentElementName.equals(element.getName());
        }
    }

    private static final class WrapLabelView extends LabelView {
        WrapLabelView(Element element) {
            super(element);
        }

        @Override
        public float getMinimumSpan(int axis) {
            return axis == View.X_AXIS ? 0 : super.getMinimumSpan(axis);
        }
    }

    /** 화살표 버튼 없이 얇은 막대만 있는 스크롤바. */
    private static final class SlimScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void paintTrack(Graphics g, JComponent component, Rectangle bounds) {
            // 바탕은 그리지 않는다.
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
            Graphics2D g = Theme.smooth(graphics);
            g.setColor(Theme.CARD_SHADOW);
            g.fill(new RoundRectangle2D.Double(bounds.x + 1, bounds.y, bounds.width - 2, bounds.height, 6, 6));
            g.dispose();
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return emptyButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return emptyButton();
        }

        private static JButton emptyButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }
    }
}
