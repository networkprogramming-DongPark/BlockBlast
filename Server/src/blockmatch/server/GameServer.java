package blockmatch.server;

import blockmatch.common.GameConfig;
import blockmatch.common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/** 플레이어 두 명의 접속을 받고, 두 명이 모두 입장하면 게임을 시작시킨다. */
final class GameServer {
    private final int port;
    private final ClientHandler[] players = new ClientHandler[GameConfig.PLAYER_COUNT];
    private final String[] names = new String[GameConfig.PLAYER_COUNT];
    private int joinedCount;

    GameServer(int port) {
        this.port = port;
    }

    void run() throws IOException {
        // 두 명을 받으면 대기 소켓을 닫는다. 세 번째 접속은 연결 거부된다.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("서버 시작: 포트 " + port);
            for (int i = 0; i < GameConfig.PLAYER_COUNT; i++) {
                Socket socket = serverSocket.accept();
                System.out.println("접속: " + socket.getRemoteSocketAddress());
                new Thread(new ClientHandler(socket, this), "player-" + i).start();
            }
        }
    }

    /** 플레이어 스레드마다 호출하므로 synchronized로 보호한다. */
    synchronized void join(ClientHandler player, String name) throws IOException {
        int number = joinedCount++;
        players[number] = player;
        names[number] = name;
        player.send(Protocol.encode(Protocol.WELCOME, number));

        if (joinedCount == GameConfig.PLAYER_COUNT) {
            String start = Protocol.encode(Protocol.START, names[0], names[1], GameConfig.TIME_LIMIT_SECONDS);
            for (ClientHandler each : players) {
                each.send(start);
            }
        }
    }
}
