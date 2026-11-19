package blockmatch.common;

import java.util.regex.Pattern;

/**
 * 서버와 클라이언트가 주고받는 메시지 형식. 한 줄이 메시지 하나이고 형식은 "종류|값1|값2"이다.
 * (Common/docs/DESIGN.md 6장 참고)
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
    /** 클라이언트 → 서버: "PLACE|블록칸|행|열". 내 트레이의 블록을 내 보드에 놓겠다는 요청. */
    public static final String PLACE = "PLACE";
    /** 서버 → 클라이언트: "STATE|점수0|보드0|블록들0|점수1|보드1|블록들1". 두 플레이어의 상태 전체. */
    public static final String STATE = "STATE";

    /** REJECT의 사유 코드. */
    public static final String REASON_BAD_NAME = "BAD_NAME";
    public static final String REASON_BAD_REQUEST = "BAD_REQUEST";
    public static final String REASON_CANNOT_PLACE = "CANNOT_PLACE";
    public static final String REASON_NOT_PLAYING = "NOT_PLAYING";
    public static final String REASON_ROOM_FULL = "ROOM_FULL";

    private static final String SEPARATOR = "|";
    /** STATE의 블록들 값에서 블록 이름 사이의 구분자와, 이미 사용한 칸의 표시. */
    private static final String HAND_SEPARATOR = ",";
    private static final String EMPTY_SLOT = "-";

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

    /**
     * 두 플레이어의 상태를 STATE 메시지로 만든다. 플레이어마다 점수, 보드, 블록들 세 값을 보낸다.
     * 보드는 64칸의 색상 번호를 이어 붙인 문자열이고(빈 칸은 0), 블록들은 "T,SQUARE2,-"처럼 블록 이름을 쉼표로 잇는다.
     * 예: STATE|40|11100000...|-,SQUARE2,L|0|0000...|LINE3_H,T,S
     */
    public static String encodeState(GameState state) {
        StringBuilder message = new StringBuilder(STATE);
        for (int i = 0; i < GameConfig.PLAYER_COUNT; i++) {
            PlayerState player = state.getPlayer(i);
            message.append(SEPARATOR).append(player.getScore())
                    .append(SEPARATOR).append(encodeBoard(player.getBoard()))
                    .append(SEPARATOR).append(encodeHand(player.getHand()));
        }
        return message.toString();
    }

    /**
     * STATE 메시지에서 한 플레이어 몫(점수, 보드, 블록들)을 읽어 target에 덮어쓴다.
     * 값을 모두 확인한 뒤에 바꾸므로, 형식이 틀리면 target은 그대로다.
     *
     * @throws IllegalArgumentException 형식이 틀린 경우
     */
    public static void readPlayer(String score, String board, String hand, PlayerState target) {
        int points = Integer.parseInt(score);
        int size = GameConfig.BOARD_SIZE;
        if (board.length() != size * size) {
            throw new IllegalArgumentException("보드 칸 수가 맞지 않음: " + board.length());
        }
        String[] names = hand.split(HAND_SEPARATOR, -1);
        if (names.length != GameConfig.HAND_SIZE) {
            throw new IllegalArgumentException("블록 수가 맞지 않음: " + hand);
        }
        BlockShape[] shapes = new BlockShape[names.length];
        for (int i = 0; i < names.length; i++) {
            shapes[i] = names[i].equals(EMPTY_SLOT) ? null : BlockShape.valueOf(names[i]);
        }
        for (int i = 0; i < board.length(); i++) {
            if (!Character.isDigit(board.charAt(i))) {
                throw new IllegalArgumentException("보드에 숫자가 아닌 값: " + board.charAt(i));
            }
        }

        target.setScore(points);
        for (int i = 0; i < board.length(); i++) {
            target.getBoard().set(i / size, i % size, board.charAt(i) - '0');
        }
        System.arraycopy(shapes, 0, target.getHand(), 0, shapes.length);
    }

    private static String encodeBoard(Board board) {
        StringBuilder cells = new StringBuilder();
        for (int row = 0; row < GameConfig.BOARD_SIZE; row++) {
            for (int col = 0; col < GameConfig.BOARD_SIZE; col++) {
                cells.append(board.get(row, col));
            }
        }
        return cells.toString();
    }

    private static String encodeHand(BlockShape[] hand) {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < hand.length; i++) {
            if (i > 0) {
                names.append(HAND_SEPARATOR);
            }
            names.append(hand[i] == null ? EMPTY_SLOT : hand[i].name());
        }
        return names.toString();
    }
}
