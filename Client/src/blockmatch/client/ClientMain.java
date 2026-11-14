package blockmatch.client;

import blockmatch.client.ui.GameFrame;
import blockmatch.common.GameRules;
import blockmatch.common.GameState;
import blockmatch.common.Protocol;

import javax.swing.SwingUtilities;
import java.util.Random;

/** 클라이언트 진입점. 실행 인자: [닉네임] [서버 주소] */
public final class ClientMain {
    private static final String DEFAULT_NICKNAME = "플레이어";
    private static final String DEFAULT_HOST = "localhost";
    private static final String OPPONENT_NAME = "상대";

    private ClientMain() {
    }

    public static void main(String[] args) {
        String nickname = args.length > 0 ? args[0] : DEFAULT_NICKNAME;
        String host = args.length > 1 ? args[1] : DEFAULT_HOST;

        SwingUtilities.invokeLater(() -> {
            // 11주차는 내 보드에서 혼자 플레이한다. 12주차부터 상태는 서버가 보내 준다.
            GameState state = new GameState(nickname, OPPONENT_NAME);
            GameRules rules = new GameRules(new Random());
            rules.refillHandIfEmpty(state.getPlayer(0));

            GameFrame frame = new GameFrame(state, rules);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Swing 컴포넌트는 EDT에서만 변경한다.
            ServerConnection connection = new ServerConnection(host, nickname,
                    message -> SwingUtilities.invokeLater(() -> frame.showMessage(message)));
            frame.setChatSender(text -> connection.send(Protocol.CHAT, text));
            connection.start();
        });
    }
}
