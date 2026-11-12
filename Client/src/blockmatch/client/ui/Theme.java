package blockmatch.client.ui;

import java.awt.Color;
import java.awt.Graphics2D;

/** 화면 색상과 블록 칸 그리기. 블록 이미지를 쓰게 되면 paintBlockCell만 바꾸면 된다. */
final class Theme {
    static final Color GRID_LINE = new Color(0xC5CBD8);
    static final Color EMPTY_CELL = new Color(0xE9ECF2);
    static final Color SLOT_BACKGROUND = new Color(0xF7F8FB);
    static final Color SLOT_BORDER = new Color(0xD3D8E2);
    /** 놓을 수 없는 자리 표시. 블록 색(빨강 포함)과 헷갈리지 않게 회색을 쓴다. */
    static final Color INVALID = new Color(0x4A5060);

    /** 색상 번호(BlockShape#getColorId) 1~6에 대응하는 색. 0번은 빈 칸이라 쓰지 않는다. */
    private static final Color[] BLOCK_COLORS = {
            null,
            new Color(0x4F9BFF), // 1 파랑
            new Color(0xFFA95A), // 2 주황
            new Color(0xAE85F0), // 3 보라
            new Color(0x5FCF80), // 4 초록
            new Color(0xFFD45A), // 5 노랑
            new Color(0xF7727F), // 6 빨강
    };

    private Theme() {
    }

    static Color blockColor(int colorId) {
        return BLOCK_COLORS[colorId];
    }

    /** 블록 한 칸을 그린다. 윗면과 왼쪽은 밝게, 아래와 오른쪽은 어둡게 칠해 입체감을 낸다. */
    static void paintBlockCell(Graphics2D g, int x, int y, int size, Color color) {
        int edge = Math.max(2, size / 8);
        g.setColor(color);
        g.fillRect(x, y, size, size);
        g.setColor(color.brighter());
        g.fillRect(x, y, size, edge);
        g.fillRect(x, y, edge, size);
        g.setColor(color.darker());
        g.fillRect(x, y + size - edge, size, edge);
        g.fillRect(x + size - edge, y, edge, size);
    }

    /** 놓일 자리 미리보기 칸. 반투명하게 칠한다. */
    static void paintPreviewCell(Graphics2D g, int x, int y, int size, Color color) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
        g.fillRect(x, y, size, size);
    }
}
