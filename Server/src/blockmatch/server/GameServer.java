package blockmatch.server;

import blockmatch.common.GameConfig;
import blockmatch.common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * 플레이어 두 명의 접속을 받고, 두 명이 모두 입장하면 게임을 시작시킨다.
 *
 * 게임 중에 한 명이 나가면 남은 사람에게 LEAVE를 보내고 자리를 비워 둔다.
 * 그 자리에 다시 들어오면(같은 이름이면 같은 번호로) 남은 사람에게 REJOIN을 보낸다.
 * 둘 다 나가면 처음 상태로 돌아가 새 게임을 시작할 수 있다.
 */
final class GameServer {
    private static final int MAX_NAME_LENGTH = 10;

    private final int port;
    private final ClientHandler[] players = new ClientHandler[GameConfig.PLAYER_COUNT];
    /** 자리별 플레이어 이름. 게임이 시작된 뒤에는 나간 사람의 이름도 남겨 두어 재접속을 알아본다. */
    private final String[] names = new String[GameConfig.PLAYER_COUNT];
    private boolean started;

    GameServer(int port) {
        this.port = port;
    }

    void run() throws IOException {
        // 나갔다가 다시 들어오는 접속을 받아야 하므로 서버가 꺼질 때까지 계속 받는다.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("서버 시작: 포트 " + port);
            for (int count = 1; ; count++) {
                Socket socket = serverSocket.accept();
                System.out.println("접속: " + socket.getRemoteSocketAddress());
                new Thread(new ClientHandler(socket, this), "player-" + count).start();
            }
        }
    }

    /**
     * 플레이어 스레드마다 호출하므로 synchronized로 보호한다.
     *
     * @return 입장시켰으면 true. 이름이 잘못되었거나 자리가 없으면 REJECT를 보내고 false
     */
    synchronized boolean join(ClientHandler player, String name) throws IOException {
        if (!isValidName(name) || isNameInUse(name)) {
            player.send(Protocol.encode(Protocol.REJECT, Protocol.REASON_BAD_NAME));
            return false;
        }
        int slot = findFreeSlot(name);
        if (slot < 0) {
            player.send(Protocol.encode(Protocol.REJECT, Protocol.REASON_ROOM_FULL));
            return false;
        }

        // 게임이 시작된 뒤에 같은 이름이 같은 자리로 돌아오면 재접속이다.
        boolean returning = started && name.equals(names[slot]);
        players[slot] = player;
        names[slot] = name;
        player.send(Protocol.encode(Protocol.WELCOME, slot));

        if (presentCount() < GameConfig.PLAYER_COUNT) {
            return true;
        }
        String start = Protocol.encode(Protocol.START, names[0], names[1], GameConfig.TIME_LIMIT_SECONDS);
        if (!started) {
            started = true;
            for (ClientHandler each : players) {
                each.send(start);
            }
        } else {
            player.send(start);
            sendQuietly(players[otherSlot(slot)], Protocol.encode(Protocol.REJOIN, name));
            System.out.println((returning ? "재접속: " : "새 플레이어 입장: ") + name);
        }
        return true;
    }

    /** 연결이 끊긴 플레이어를 자리에서 빼고, 게임 중이었으면 남은 사람에게 알린다. */
    synchronized void leave(ClientHandler player) {
        int slot = indexOf(player);
        if (slot < 0) {
            return; // 입장하기 전에 거부되었거나 끊긴 연결
        }
        players[slot] = null;

        if (!started) {
            names[slot] = null;
            return;
        }
        ClientHandler remaining = players[otherSlot(slot)];
        if (remaining == null) {
            // 둘 다 나갔으면 처음 상태로 돌아가 새 게임을 받을 수 있게 한다.
            started = false;
            Arrays.fill(names, null);
            return;
        }
        sendQuietly(remaining, Protocol.encode(Protocol.LEAVE, names[slot]));
    }

    /**
     * 채팅을 보낸 사람 이름을 붙여 자리에 있는 플레이어 모두에게 전달한다. (보낸 사람 포함)
     * 게임이 시작되기 전이면 NOT_PLAYING, 비었거나 너무 길면 BAD_REQUEST로 거부한다.
     */
    synchronized void chat(ClientHandler sender, String text) throws IOException {
        int slot = indexOf(sender);
        if (slot < 0 || !started) {
            sender.send(Protocol.encode(Protocol.REJECT, Protocol.REASON_NOT_PLAYING));
            return;
        }
        if (text.isBlank() || text.length() > GameConfig.CHAT_MAX_LENGTH) {
            sender.send(Protocol.encode(Protocol.REJECT, Protocol.REASON_BAD_REQUEST));
            return;
        }

        String message = Protocol.encode(Protocol.CHAT, names[slot], text);
        for (ClientHandler each : players) {
            sendQuietly(each, message);
        }
    }

    private static boolean isValidName(String name) {
        return !name.isBlank() && name.length() <= MAX_NAME_LENGTH && name.indexOf('|') < 0;
    }

    private boolean isNameInUse(String name) {
        for (int i = 0; i < players.length; i++) {
            if (players[i] != null && name.equals(names[i])) {
                return true;
            }
        }
        return false;
    }

    /** 비어 있는 자리를 찾는다. 같은 이름이 쓰던 자리가 있으면 그 자리를 먼저 준다. */
    private int findFreeSlot(String name) {
        for (int i = 0; i < players.length; i++) {
            if (players[i] == null && name.equals(names[i])) {
                return i;
            }
        }
        for (int i = 0; i < players.length; i++) {
            if (players[i] == null) {
                return i;
            }
        }
        return -1;
    }

    private int presentCount() {
        int count = 0;
        for (ClientHandler each : players) {
            if (each != null) {
                count++;
            }
        }
        return count;
    }

    private int indexOf(ClientHandler player) {
        for (int i = 0; i < players.length; i++) {
            if (players[i] == player) {
                return i;
            }
        }
        return -1;
    }

    private static int otherSlot(int slot) {
        return GameConfig.PLAYER_COUNT - 1 - slot;
    }

    /** 한 명에게 보내지 못해도 보낸 사람의 연결과 다른 플레이어에게는 영향이 없어야 한다. */
    private static void sendQuietly(ClientHandler target, String message) {
        if (target == null) {
            return;
        }
        try {
            target.send(message);
        } catch (IOException e) {
            System.out.println("전달 실패: " + e.getMessage());
        }
    }
}
