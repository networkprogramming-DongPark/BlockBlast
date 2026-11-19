package blockmatch.client;

import blockmatch.client.ui.GameFrame;
import blockmatch.common.GameState;
import blockmatch.common.Protocol;

import javax.swing.SwingUtilities;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 클라이언트 진입점. 실행 인자: [닉네임] [서버 주소]
 * 닉네임을 주지 않으면 "플레이어1234"처럼 무작위 네 자리 숫자를 붙인다.
 * 서버는 같은 닉네임의 입장을 거부하므로, 닉네임 없이 두 번 실행해도 둘 다 들어갈 수 있게 하기 위해서다.
 * (닉네임은 이후 메인 화면에서 입력받을 예정)
 */
public final class ClientMain {
    private static final String DEFAULT_NICKNAME_PREFIX = "플레이어";
    private static final String DEFAULT_HOST = "localhost";
    private static final String OPPONENT_NAME = "상대";

    private ClientMain() {
    }

    public static void main(String[] args) {
        String nickname = args.length > 0 ? args[0]
                : DEFAULT_NICKNAME_PREFIX + ThreadLocalRandom.current().nextInt(1000, 10000);
        String host = args.length > 1 ? args[1] : DEFAULT_HOST;

        SwingUtilities.invokeLater(() -> {
            // 게임 상태의 원본은 서버에 있다. 이 GameState는 서버가 보낸 STATE를 받아 화면에 그리기 위한 것이다.
            GameState state = new GameState(nickname, OPPONENT_NAME);
            GameFrame frame = new GameFrame(state);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Swing 컴포넌트는 EDT에서만 변경한다.
            ServerConnection connection = new ServerConnection(host, nickname,
                    message -> SwingUtilities.invokeLater(() -> frame.showMessage(message)),
                    new GameSession(state, frame));
            frame.setChatSender(text -> connection.send(Protocol.CHAT, text));
            frame.setPlaceSender((handIndex, row, col) -> connection.send(Protocol.PLACE, handIndex, row, col));
            connection.start();
        });
    }
}
