package blockmatch.client;

import blockmatch.client.ui.GameFrame;
import blockmatch.client.ui.MainFrame;
import blockmatch.common.GameState;
import blockmatch.common.Protocol;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

/**
 * 메인 화면과 게임 화면 사이의 전환을 맡는다. 모든 메서드는 EDT에서 호출한다.
 *
 * 메인 화면에서 "게임 시작"을 누르면 게임 창을 만들고 서버에 접속한다. 게임이 끝나거나, 입장이 거부되거나,
 * 서버와 연결이 끊기거나, 게임 창을 닫으면 연결을 끊고 메인 화면으로 돌아온다.
 */
final class ClientApp {
    private static final String OPPONENT_NAME = "상대";

    private final String host;
    private final MainFrame mainFrame;
    /** 지금 진행 중인 게임 창과 연결. 메인 화면에 있을 때는 null. */
    private GameFrame gameFrame;
    private ServerConnection connection;

    ClientApp(String host, String initialNickname) {
        this.host = host;
        mainFrame = new MainFrame(initialNickname, this::startGame);
    }

    void showMain() {
        mainFrame.open(null);
    }

    private void startGame(String nickname) {
        // 게임 상태의 원본은 서버에 있다. 이 GameState는 서버가 보낸 STATE를 받아 화면에 그리기 위한 것이다.
        GameState state = new GameState(nickname, OPPONENT_NAME);
        GameFrame frame = new GameFrame(state);
        // 이 게임 창에서 생긴 일로만 메인 화면으로 돌아가도록 frame을 함께 넘긴다.
        Consumer<String> backToMain = message -> returnToMain(frame, message);

        // Swing 컴포넌트는 EDT에서만 변경한다.
        ServerConnection newConnection = new ServerConnection(host, nickname,
                message -> SwingUtilities.invokeLater(() -> frame.showMessage(message)),
                new GameSession(state, frame, backToMain));
        frame.setChatSender(text -> newConnection.send(Protocol.CHAT, text));
        frame.setPlaceSender((handIndex, row, col) -> newConnection.send(Protocol.PLACE, handIndex, row, col));
        frame.setOnClose(() -> returnToMain(frame, null));

        gameFrame = frame;
        connection = newConnection;
        mainFrame.setVisible(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        newConnection.start();
    }

    /**
     * 연결을 끊고 게임 창을 닫은 뒤 메인 화면을 보여 준다.
     * 한 게임에서 여러 이유로 거의 동시에 불릴 수 있으므로, 지금 게임 창에 대한 첫 호출만 처리한다.
     */
    private void returnToMain(GameFrame frame, String message) {
        if (frame != gameFrame) {
            return;
        }
        gameFrame = null;
        connection.close();
        connection = null;
        frame.dispose();
        mainFrame.open(message);
    }
}
