package blockmatch.common;

/**
 * 플레이어에게 제공되는 블록 모양.
 * '#'은 채워진 칸, '.'은 빈 칸이며 문자열 한 개가 한 행이다.
 * 색상 번호는 1~6이고, 실제 색은 클라이언트의 Theme이 정한다.
 */
public enum BlockShape {
    SINGLE(5, "#"),
    DOMINO_H(2, "##"),
    LINE3_H(5, "###"),
    LINE3_V(5, "#", "#", "#"),
    LINE4_H(6, "####"),
    LINE4_V(6, "#", "#", "#", "#"),
    SQUARE2(1, "##", "##"),
    SQUARE3(4, "###", "###", "###"),
    CORNER(4, "##", "#."),
    L(2, "#.", "#.", "##"),
    J(1, ".#", ".#", "##"),
    T(3, "###", ".#."),
    S(3, ".##", "##.");

    private final int colorId;
    private final String[] rows;

    BlockShape(int colorId, String... rows) {
        this.colorId = colorId;
        this.rows = rows;
    }

    public int getColorId() {
        return colorId;
    }

    public int getHeight() {
        return rows.length;
    }

    public int getWidth() {
        return rows[0].length();
    }

    public boolean isFilled(int row, int col) {
        return rows[row].charAt(col) == '#';
    }
}
