package blockmatch.client;

import blockmatch.client.ui.GameFrame;

import javax.swing.SwingUtilities;

/** 클라이언트 진입점. 실행 인자: [닉네임] [서버 주소] */
public final class ClientMain {
    private static final String DEFAULT_NICKNAME = "플레이어";
    private static final String DEFAULT_HOST = "localhost";

    private ClientMain() {
    }

    public static void main(String[] args) {
        String nickname = args.length > 0 ? args[0] : DEFAULT_NICKNAME;
        String host = args.length > 1 ? args[1] : DEFAULT_HOST;

        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Swing 컴포넌트는 EDT에서만 변경한다.
            new ServerConnection(host, nickname,
                    message -> SwingUtilities.invokeLater(() -> frame.showMessage(message))).start();
        });
    }
}
