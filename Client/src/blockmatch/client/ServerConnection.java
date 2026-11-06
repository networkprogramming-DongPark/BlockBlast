package blockmatch.client;

import blockmatch.common.GameConfig;
import blockmatch.common.Protocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** 서버 연결. 수신 스레드에서 동작하며, 상태 메시지를 listener로 전달한다. */
final class ServerConnection {
    private final String host;
    private final String nickname;
    private final Consumer<String> listener;

    ServerConnection(String host, String nickname, Consumer<String> listener) {
        this.host = host;
        this.nickname = nickname;
        this.listener = listener;
    }

    void start() {
        Thread thread = new Thread(this::run, "server-connection");
        thread.setDaemon(true); // 창을 닫으면 함께 끝나도록 한다.
        thread.start();
    }

    private void run() {
        try (Socket socket = new Socket(host, GameConfig.DEFAULT_PORT);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            listener.accept("서버에 접속했습니다: " + socket.getRemoteSocketAddress());
            writer.write(Protocol.encode(Protocol.JOIN, nickname));
            writer.newLine();
            writer.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                handle(line);
            }
            listener.accept("서버와 연결이 끊어졌습니다.");
        } catch (IOException e) {
            listener.accept("서버 연결 오류: " + e.getMessage());
        }
    }

    private void handle(String line) {
        String[] parts = Protocol.decode(line);
        switch (parts[0]) {
            case Protocol.WELCOME -> listener.accept("입장했습니다. 플레이어 번호: " + parts[1]);
            case Protocol.START -> listener.accept(
                    "게임 시작: " + parts[1] + " vs " + parts[2] + " (제한 시간 " + parts[3] + "초)");
            default -> listener.accept("알 수 없는 메시지: " + line);
        }
    }
}
