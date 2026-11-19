package blockmatch.common;

/** 클라이언트와 서버가 함께 쓰는 게임 상수. */
public final class GameConfig {
    public static final int BOARD_SIZE = 8;
    public static final int HAND_SIZE = 3;
    public static final int PLAYER_COUNT = 2;
    /** 게임 한 판의 제한 시간(5분). 서버 실행 인자로 바꿀 수 있다. */
    public static final int TIME_LIMIT_SECONDS = 300;
    public static final int DEFAULT_PORT = 30000;
    public static final int CHAT_MAX_LENGTH = 100;

    private GameConfig() {
    }
}
