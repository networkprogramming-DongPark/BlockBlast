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
}
