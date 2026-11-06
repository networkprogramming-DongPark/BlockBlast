package blockmatch.client.ui;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** 게임 메인 창. 상단에 게임 정보, 왼쪽에 보드와 블록 트레이, 오른쪽에 채팅을 둔다. */
public final class GameFrame extends JFrame {
    public GameFrame() {
        super("블록 매치");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel playArea = new JPanel(new BorderLayout(0, 10));
        playArea.add(new BoardPanel(), BorderLayout.CENTER);
        playArea.add(new BlockTrayPanel(), BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(new HeaderPanel(), BorderLayout.NORTH);
        root.add(playArea, BorderLayout.CENTER);
        root.add(new ChatPanel(), BorderLayout.EAST);

        setContentPane(root);
        pack();
    }
}
