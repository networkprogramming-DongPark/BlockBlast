package blockmatch.server;

import blockmatch.common.Protocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/** 플레이어 한 명의 연결. 전용 스레드에서 메시지를 받는다. */
final class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameServer server;
    private final SocketAddress address;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private boolean joined;
    private boolean rejected;

    ClientHandler(Socket socket, GameServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        address = socket.getRemoteSocketAddress();
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        try (socket) {
            String line;
            // 입장이 거부되면 연결을 닫는다.
            while (!rejected && (line = reader.readLine()) != null) {
                System.out.println("수신 " + address + ": " + line);
                handle(line);
            }
        } catch (IOException e) {
            System.out.println("통신 오류 " + address + ": " + e.getMessage());
        } finally {
            // 정상 종료든 강제 종료든 자리를 비우고 상대에게 알린다.
            server.leave(this);
        }
        System.out.println("접속 종료: " + address);
    }

    private void handle(String line) throws IOException {
        // 값이 하나뿐인 메시지의 값에는 "|"가 들어갈 수 있으므로 종류와 값 두 덩어리로만 나눈다.
        String[] parts = Protocol.decode(line, 2);
        if (parts.length < 2) {
            return;
        }
        switch (parts[0]) {
            case Protocol.JOIN -> {
                if (!joined) {
                    joined = server.join(this, parts[1]);
                    rejected = !joined;
                }
            }
            case Protocol.CHAT -> server.chat(this, parts[1]);
            default -> System.out.println("알 수 없는 메시지 " + address + ": " + line);
        }
    }

    /** START는 다른 플레이어의 스레드에서도 보내므로 synchronized로 보호한다. */
    synchronized void send(String message) throws IOException {
        writer.write(message);
        writer.newLine();
        writer.flush();
        System.out.println("송신 " + address + ": " + message);
    }
}
