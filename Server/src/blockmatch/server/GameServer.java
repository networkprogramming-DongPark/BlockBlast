package blockmatch.server;

import blockmatch.common.GameConfig;
import blockmatch.common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 플레이어 두 명의 접속을 받고, 두 명이 모두 입장하면 게임을 시작시킨다.
 *
 * 게임 중에 한 명이 나가면 남은 사람에게 LEAVE를 보내고 자리를 비워 둔다.
 * 그 자리에 다시 들어오면(같은 이름이면 같은 번호로) 남은 사람에게 REJOIN을 보낸다.
 * 둘 다 나가면 처음 상태로 돌아가 새 게임을 시작할 수 있다.
 *
 * 게임이 시작되면 GameRoom이 게임 상태의 원본을 갖는다. 클라이언트의 PLACE 요청을 판정하고,
 * 놓았으면 두 플레이어 모두에게 STATE를, 놓을 수 없으면 요청한 플레이어에게만 REJECT를 보낸다.
 * 제한 시간이 끝나거나 한 플레이어가 더 놓을 블록이 없으면 두 플레이어 모두에게 GAMEOVER를 보낸다.
 */
final class GameServer {
    private final int port;
    private final int timeLimitSeconds;
    /** 제한 시간이 끝나면 timeUp()을 부르는 타이머 스레드. 창을 닫을 때 서버가 남지 않도록 데몬 스레드로 만든다. */
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "game-timer");
        thread.setDaemon(true);
        return thread;
    });
    private final ClientHandler[] players = new ClientHandler[GameConfig.PLAYER_COUNT];
    /** 자리별 플레이어 이름. 게임이 시작된 뒤에는 나간 사람의 이름도 남겨 두어 재접속을 알아본다. */
    private final String[] names = new String[GameConfig.PLAYER_COUNT];
    private boolean started;
    /** 게임이 시작된 동안만 있다. 둘 다 나가면 버린다. */
    private GameRoom room;
    private ScheduledFuture<?> timeUpTask;

    GameServer(int port, int timeLimitSeconds) {
        this.port = port;
        this.timeLimitSeconds = timeLimitSeconds;
    }

    void run() throws IOException {
        // 나갔다가 다시 들어오는 접속을 받아야 하므로 서버가 꺼질 때까지 계속 받는다.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("서버 시작: 포트 " + port + ", 제한 시간 " + timeLimitSeconds + "초");
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
        if (!Protocol.isValidNickname(name) || isNameInUse(name)) {
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
        if (!started) {
            started = true;
            room = new GameRoom(names[0], names[1], timeLimitSeconds);
            GameRoom startedRoom = room;
            timeUpTask = timer.schedule(() -> timeUp(startedRoom), timeLimitSeconds, TimeUnit.SECONDS);
            String start = Protocol.encode(Protocol.START, names[0], names[1], timeLimitSeconds);
            String state = room.stateMessage();
            for (ClientHandler each : players) {
                each.send(start);
                each.send(state);
            }
        } else {
            // 다시 들어온 플레이어는 진행 중이던 보드와 점수, 남은 시간을 그대로 이어받는다.
            player.send(Protocol.encode(Protocol.START, names[0], names[1], room.remainingSeconds()));
            player.send(room.stateMessage());
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
            room = null;
            if (timeUpTask != null) {
                timeUpTask.cancel(false);
                timeUpTask = null;
            }
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

    /**
     * 블록 배치 요청을 판정한다. values는 "블록칸|행|열"이다.
     * 놓았으면 PLACED와 바뀐 상태를 자리에 있는 모두에게 보내고, 아니면 요청한 플레이어에게만 거부 사유를 보낸다.
     */
    synchronized void place(ClientHandler sender, String values) throws IOException {
        int slot = indexOf(sender);
        if (slot < 0 || room == null || room.isOver()) {
            sender.send(Protocol.encode(Protocol.REJECT, Protocol.REASON_NOT_PLAYING));
            return;
        }
        int[] request = parsePlace(values);
        if (request == null) {
            sender.send(Protocol.encode(Protocol.REJECT, Protocol.REASON_BAD_REQUEST));
            return;
        }
        String placed = room.place(slot, request[0], request[1], request[2]);
        if (placed == null) {
            sender.send(Protocol.encode(Protocol.REJECT, Protocol.REASON_CANNOT_PLACE));
            return;
        }
        String state = room.stateMessage();
        for (ClientHandler each : players) {
            // PLACED를 먼저 보내야 클라이언트가 지워진 칸을 빈칸으로 그리기 전에 효과를 시작한다.
            sendQuietly(each, placed);
            sendQuietly(each, state);
        }
        // 놓은 뒤 남은 블록을 하나도 놓을 수 없으면 GameRoom이 게임을 끝낸다.
        if (room.isOver()) {
            endGame();
        }
    }

    /** 타이머 스레드에서 부른다. 그 사이 게임이 바뀌었거나 이미 끝났으면 아무것도 하지 않는다. */
    private synchronized void timeUp(GameRoom expired) {
        if (room == expired && expired.timeUp()) {
            endGame();
        }
    }

    /**
     * 게임 결과를 자리에 있는 모두에게 알리고 타이머를 멈춘 뒤, 방을 비워 다음 게임을 받을 준비를 한다.
     * 클라이언트는 결과를 보여 준 뒤 메인 화면으로 돌아가 연결을 끊는다. 그 사이 남아 있는 연결은 자리가 없으므로
     * 채팅이나 배치를 보내도 NOT_PLAYING으로 거부되고, 끝난 게임에 다시 들어오는 일도 없다.
     */
    private void endGame() {
        String gameOver = room.gameOverMessage();
        System.out.println("게임 종료: " + gameOver);
        for (ClientHandler each : players) {
            sendQuietly(each, gameOver);
        }
        if (timeUpTask != null) {
            timeUpTask.cancel(false);
            timeUpTask = null;
        }
        started = false;
        room = null;
        Arrays.fill(players, null);
        Arrays.fill(names, null);
    }

    /** "블록칸|행|열"을 숫자 세 개로 바꾼다. 개수나 형식이 틀리거나 블록칸이 범위 밖이면 null. */
    private static int[] parsePlace(String values) {
        String[] parts = Protocol.decode(values);
        if (parts.length != 3) {
            return null;
        }
        try {
            int handIndex = Integer.parseInt(parts[0]);
            if (handIndex < 0 || handIndex >= GameConfig.HAND_SIZE) {
                return null;
            }
            // 행과 열이 보드 밖이면 GameRules가 놓을 수 없다고 판정한다.
            return new int[] {handIndex, Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
        } catch (NumberFormatException e) {
            return null;
        }
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
