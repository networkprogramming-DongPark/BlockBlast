package blockmatch.client;

import blockmatch.client.ui.GameFrame;

import javax.swing.SwingUtilities;

/** 클라이언트 진입점. */
public final class ClientMain {
    private ClientMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
