package blockmatch.server;

import blockmatch.common.GameConfig;

/** 서버 진입점. 소켓 통신은 12주차에 구현한다. (Common/docs/DESIGN.md 참고) */
public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) {
        System.out.println("블록 매치 서버: 12주차 구현 예정 (포트 " + GameConfig.DEFAULT_PORT + ")");
    }
}
