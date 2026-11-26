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

/**
 * 서버 연결. 수신 스레드에서 동작한다.
 * 채팅 영역에 보여 줄 문장은 listener로, 게임 진행 메시지(WELCOME, START, STATE)는 game으로 전달한다.
 */
final class ServerConnection {
    /** 게임 진행 메시지를 받는 쪽. 수신 스레드에서 호출된다. */
    interface GameListener {
        void onWelcome(int number);

        /** seconds는 남은 제한 시간(초)이다. 처음 시작이면 전체 제한 시간, 다시 들어온 경우에는 남은 시간이다. */
        void onStart(String firstPlayer, String secondPlayer, int seconds);

        /** parts는 STATE 메시지를 "|"로 나눈 것이다. */
        void onState(String[] parts);

        /** 블록이 놓였다. parts는 PLACED 메시지를 "|"로 나눈 것이다. */
        void onPlaced(String[] parts);

        /** parts는 GAMEOVER 메시지를 "|"로 나눈 것이다. */
        void onGameOver(String[] parts);

        /** 채팅을 받았다. 내가 보낸 채팅도 서버가 다시 보내 준다. */
        void onChat(String name, String text);

        /** 게임 중에 상대가 나갔거나(false), 나간 자리에 다시 들어왔다(true). */
        void onOpponentPresence(String name, boolean present);

        /** 닉네임이 거부되었거나 방이 가득 차서 들어가지 못했다. message는 사용자에게 보여 줄 이유다. */
        void onJoinRejected(String message);

        /** 서버에 접속하지 못했거나 연결이 끊겼다. close()로 직접 끊은 경우에는 부르지 않는다. */
        void onDisconnected(String message);
    }

    private final String host;
    private final String nickname;
    private final Consumer<String> listener;
    private final GameListener game;
    /** 연결된 동안만 값이 있다. 수신 스레드가 쓰고 EDT가 send()로 읽는다. */
    private volatile BufferedWriter writer;
    /** close()에서 끊을 수 있도록 연결된 소켓을 기억한다. */
    private volatile Socket socket;
    /** close()로 직접 끊었으면 true. 이때는 연결이 끊겼다고 알리지 않는다. */
    private volatile boolean closed;
    /** 아래 두 값은 수신 스레드(handle)에서만 읽고 쓴다. 상대가 다시 들어왔을 때 재접속인지 알아보는 데 쓴다. */
    private int myNumber = -1;
    private String opponentName = "";

    ServerConnection(String host, String nickname, Consumer<String> listener, GameListener game) {
        this.host = host;
        this.nickname = nickname;
        this.listener = listener;
        this.game = game;
    }

    void start() {
        Thread thread = new Thread(this::run, "server-connection");
        thread.setDaemon(true); // 창을 닫으면 함께 끝나도록 한다.
        thread.start();
    }

    /** 연결을 끊는다. 메인 화면으로 돌아갈 때 부른다. 어느 스레드에서 불러도 된다. */
    void close() {
        closed = true;
        Socket current = socket;
        if (current != null) {
            try {
                current.close(); // 수신 스레드의 readLine()이 예외로 끝난다.
            } catch (IOException ignored) {
                // 이미 닫혀 있으면 할 일이 없다.
            }
        }
    }

    private void run() {
        boolean connected = false;
        try (Socket socket = new Socket(host, GameConfig.DEFAULT_PORT);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            this.socket = socket;
            if (closed) {
                return; // 접속하는 사이에 close()가 불렸다.
            }
            connected = true;
            writer = out;
            listener.accept("서버에 접속했습니다: " + socket.getRemoteSocketAddress());
            send(Protocol.JOIN, nickname);

            String line;
            while ((line = reader.readLine()) != null) {
                handle(line);
            }
            if (!closed) {
                game.onDisconnected("서버와 연결이 끊어졌습니다.");
            }
        } catch (IOException e) {
            if (!closed) {
                game.onDisconnected(connected ? "서버와 연결이 끊어졌습니다. (" + e.getMessage() + ")"
                        : "서버에 접속할 수 없습니다. 서버가 켜져 있는지 확인하세요. (" + e.getMessage() + ")");
            }
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
                game.onWelcome(myNumber);
                listener.accept("입장했습니다. 플레이어 번호: " + parts[1] + " (상대가 들어오면 게임이 시작됩니다)");
            }
            case Protocol.START -> {
                // 내가 0번이면 상대는 1번이다.
                opponentName = myNumber == 0 ? parts[2] : parts[1];
                int seconds = parseNumber(parts[3]);
                game.onStart(parts[1], parts[2], Math.max(0, seconds));
                listener.accept("게임 시작: " + parts[1] + " vs " + parts[2] + " (남은 시간 " + parts[3] + "초)");
            }
            case Protocol.STATE -> game.onState(parts);
            case Protocol.PLACED -> game.onPlaced(parts);
            case Protocol.GAMEOVER -> game.onGameOver(parts);
            case Protocol.LEAVE -> {
                game.onOpponentPresence(parts[1], false);
                listener.accept("상대 " + parts[1] + "님이 접속을 종료했습니다.");
            }
            case Protocol.REJOIN -> {
                boolean returning = parts[1].equals(opponentName);
                opponentName = parts[1];
                game.onOpponentPresence(parts[1], true);
                listener.accept("상대 " + parts[1] + (returning ? "님이 재접속했습니다." : "님이 접속했습니다."));
            }
            // 채팅 내용에는 "|"가 들어갈 수 있으므로 이름과 내용 두 값까지만 나눈다.
            case Protocol.CHAT -> {
                String[] chat = Protocol.decode(line, 3);
                if (chat.length >= 3) {
                    game.onChat(chat[1], chat[2]);
                } else {
                    listener.accept("알 수 없는 메시지: " + line);
                }
            }
            case Protocol.REJECT -> {
                String reason = parts.length >= 2 ? parts[1] : "";
                // 입장 단계의 거부는 게임을 할 수 없으므로 메인 화면으로 돌려보낸다. 나머지는 채팅 영역에 안내만 한다.
                if (reason.equals(Protocol.REASON_BAD_NAME) || reason.equals(Protocol.REASON_ROOM_FULL)) {
                    game.onJoinRejected(rejectText(reason));
                } else {
                    listener.accept(rejectText(reason));
                }
            }
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
            case Protocol.REASON_BAD_NAME -> "사용할 수 없는 닉네임입니다. 상대와 같은 닉네임인지 확인하세요.";
            case Protocol.REASON_ROOM_FULL -> "방이 가득 차서 입장할 수 없습니다.";
            case Protocol.REASON_NOT_PLAYING -> "게임이 시작된 뒤에 채팅하거나 블록을 놓을 수 있습니다.";
            case Protocol.REASON_CANNOT_PLACE -> "그 자리에는 블록을 놓을 수 없습니다.";
            case Protocol.REASON_BAD_REQUEST ->
                    "보낼 수 없는 메시지입니다. (비어 있거나 " + GameConfig.CHAT_MAX_LENGTH + "자를 넘음)";
            default -> "서버가 요청을 거부했습니다: " + reason;
        };
    }
}
