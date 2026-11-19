package blockmatch.server;

import blockmatch.common.GameConfig;

import java.io.IOException;

/** 서버 진입점. 실행 인자: [제한 시간(초)] 생략하면 GameConfig.TIME_LIMIT_SECONDS(5분) */
public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws IOException {
        int timeLimit = GameConfig.TIME_LIMIT_SECONDS;
        if (args.length > 0) {
            try {
                timeLimit = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                timeLimit = -1;
            }
            if (timeLimit <= 0) {
                System.out.println("제한 시간은 1 이상의 초 단위 숫자로 입력하세요: " + args[0]);
                return;
            }
        }
        new GameServer(GameConfig.DEFAULT_PORT, timeLimit).run();
    }
}
