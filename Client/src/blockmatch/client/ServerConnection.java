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
    /** 연결된 동안만 값이 있다. 수신 스레드가 쓰고 EDT가 send()로 읽는다. */
    private volatile BufferedWriter writer;
    /** 아래 두 값은 수신 스레드(handle)에서만 읽고 쓴다. 상대가 다시 들어왔을 때 재접속인지 알아보는 데 쓴다. */
    private int myNumber = -1;
    private String opponentName = "";

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
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            writer = out;
            listener.accept("서버에 접속했습니다: " + socket.getRemoteSocketAddress());
            send(Protocol.JOIN, nickname);

            String line;
            while ((line = reader.readLine()) != null) {
                handle(line);
            }
            listener.accept("서버와 연결이 끊어졌습니다.");
        } catch (IOException e) {
            listener.accept("서버 연결 오류: " + e.getMessage());
        } finally {
            writer = null;
        }
    }

    /**
     * 서버에 메시지 한 줄을 보낸다. 어느 스레드에서 불러도 된다.
     *
     * @return 보냈으면 true, 연결되어 있지 않거나 보내다 실패했으면 false
     */
    boolean send(String type, Object... values) {
        BufferedWriter out = writer;
        if (out == null) {
            return false;
        }
        synchronized (out) {
            try {
                out.write(Protocol.encode(type, values));
                out.newLine();
                out.flush();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private void handle(String line) {
        String[] parts = Protocol.decode(line);
        switch (parts[0]) {
            case Protocol.WELCOME -> {
                myNumber = parseNumber(parts[1]);
                listener.accept("입장했습니다. 플레이어 번호: " + parts[1]);
            }
            case Protocol.START -> {
                // 내가 0번이면 상대는 1번이다.
                opponentName = myNumber == 0 ? parts[2] : parts[1];
                listener.accept("게임 시작: " + parts[1] + " vs " + parts[2] + " (제한 시간 " + parts[3] + "초)");
            }
            case Protocol.LEAVE -> listener.accept("상대 " + parts[1] + "님이 접속을 종료했습니다.");
            case Protocol.REJOIN -> {
                boolean returning = parts[1].equals(opponentName);
                opponentName = parts[1];
                listener.accept("상대 " + parts[1] + (returning ? "님이 재접속했습니다." : "님이 접속했습니다."));
            }
            // 채팅 내용에는 "|"가 들어갈 수 있으므로 이름과 내용 두 값까지만 나눈다.
            case Protocol.CHAT -> {
                String[] chat = Protocol.decode(line, 3);
                listener.accept(chat.length >= 3 ? chat[1] + ": " + chat[2] : "알 수 없는 메시지: " + line);
            }
            case Protocol.REJECT -> listener.accept(rejectText(parts.length >= 2 ? parts[1] : ""));
            default -> listener.accept("알 수 없는 메시지: " + line);
        }
    }

    private static int parseNumber(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String rejectText(String reason) {
        return switch (reason) {
            case Protocol.REASON_BAD_NAME -> "사용할 수 없는 닉네임입니다. (비었거나 10자 초과, '|' 포함, 상대와 같은 이름)";
            case Protocol.REASON_ROOM_FULL -> "방이 가득 차서 입장할 수 없습니다.";
            case Protocol.REASON_NOT_PLAYING -> "게임이 시작된 뒤에 채팅할 수 있습니다.";
            case Protocol.REASON_BAD_REQUEST ->
                    "보낼 수 없는 메시지입니다. (비어 있거나 " + GameConfig.CHAT_MAX_LENGTH + "자를 넘음)";
            default -> "서버가 요청을 거부했습니다: " + reason;
        };
    }
}
