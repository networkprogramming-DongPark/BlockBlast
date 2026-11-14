package blockmatch.common;

import java.util.regex.Pattern;

/**
 * 서버와 클라이언트가 주고받는 메시지 형식. 한 줄이 메시지 하나이고 형식은 "종류|값1|값2"이다.
 * 지금은 접속 과정에 필요한 메시지만 있다. (Common/docs/DESIGN.md 6장 참고)
 */
public final class Protocol {
    public static final String JOIN = "JOIN";
    public static final String WELCOME = "WELCOME";
    public static final String START = "START";
    /** 클라이언트 → 서버는 "CHAT|내용", 서버 → 클라이언트는 "CHAT|이름|내용". */
    public static final String CHAT = "CHAT";
    public static final String REJECT = "REJECT";
    /** 서버 → 남은 클라이언트: "LEAVE|이름". 상대의 연결이 끊겼다. */
    public static final String LEAVE = "LEAVE";
    /** 서버 → 남은 클라이언트: "REJOIN|이름". 게임이 시작된 뒤 빈자리에 플레이어가 다시 들어왔다. */
    public static final String REJOIN = "REJOIN";

    /** REJECT의 사유 코드. */
    public static final String REASON_BAD_NAME = "BAD_NAME";
    public static final String REASON_BAD_REQUEST = "BAD_REQUEST";
    public static final String REASON_NOT_PLAYING = "NOT_PLAYING";
    public static final String REASON_ROOM_FULL = "ROOM_FULL";

    private static final String SEPARATOR = "|";

    private Protocol() {
    }

    public static String encode(String type, Object... values) {
        StringBuilder message = new StringBuilder(type);
        for (Object value : values) {
            message.append(SEPARATOR).append(value);
        }
        return message.toString();
    }

    public static String[] decode(String message) {
        return message.split(Pattern.quote(SEPARATOR), -1);
    }

    /**
     * 최대 limit개로 나눈다. 마지막 값이 채팅처럼 "|"를 포함할 수 있을 때 쓴다.
     * 예: decode("CHAT|동오|a|b", 3) → [CHAT, 동오, a|b]
     */
    public static String[] decode(String message, int limit) {
        return message.split(Pattern.quote(SEPARATOR), limit);
    }
}
