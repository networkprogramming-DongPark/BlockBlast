package blockmatch.server;

import blockmatch.common.GameConfig;

import java.io.IOException;

/** 서버 진입점. */
public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws IOException {
        new GameServer(GameConfig.DEFAULT_PORT).run();
    }
}
