package blockmatch.client;

import javax.swing.SwingUtilities;

/**
 * 클라이언트 진입점. 실행 인자: [닉네임] [서버 주소]
 * 메인 화면에서 닉네임을 입력하고 게임을 시작한다. 닉네임 인자를 주면 입력 칸에 미리 채워 둔다.
 */
public final class ClientMain {
    private static final String DEFAULT_HOST = "localhost";

    private ClientMain() {
    }

    public static void main(String[] args) {
        String nickname = args.length > 0 ? args[0] : "";
        String host = args.length > 1 ? args[1] : DEFAULT_HOST;
        SwingUtilities.invokeLater(() -> new ClientApp(host, nickname).showMain());
    }
}
