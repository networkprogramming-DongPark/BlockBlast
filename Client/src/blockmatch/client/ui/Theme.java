package blockmatch.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** 화면 전체에서 함께 쓰는 색, 글꼴, 그리기 도구. 블록 모양을 바꾸려면 paintBlockCell만 고치면 된다. */
final class Theme {
    static final Color BACKGROUND = new Color(0xF5F7FC);
    static final Color CARD = Color.WHITE;
    static final Color CARD_BORDER = new Color(0xE3E8F2);
    static final Color CARD_SHADOW = new Color(0xDDE3F0);
    static final Color TEXT = new Color(0x131A45);
    static final Color MUTED = new Color(0x7B829C);
    static final Color ACCENT = new Color(0x6E86EE);
    static final Color ACCENT_SOFT = new Color(0xE8EFFB);
    static final Color GOLD = new Color(0xA58A3C);
    static final Color GREEN = new Color(0x3FA56B);
    static final Color RED = new Color(0xD1495B);

    static final Color BOARD = new Color(0x24294F);
    static final Color EMPTY_CELL = new Color(0x333B63);

    /** 색상 번호(BlockShape#getColorId) 1~6에 대응하는 색. 0번은 빈 칸이라 쓰지 않는다. */
    private static final Color[] BLOCK_COLORS = {
            null,
            new Color(0x8399EF), // 1 파랑
            new Color(0xDDB57C), // 2 주황
            new Color(0xA388DF), // 3 보라
            new Color(0xA4D297), // 4 초록
            new Color(0xE9D780), // 5 노랑
            new Color(0xCA7D83), // 6 빨강
    };

    /**
     * 앞에서부터 찾아 컴퓨터에 설치된 첫 글꼴을 쓴다. 모두 없으면 자바 기본 글꼴을 쓴다.
     * Noto Sans KR 같은 가변 글꼴은 자바가 가장 얇은 굵기로만 읽을 수 있어서 쓰지 않는다.
     */
    private static final String[] FONT_CANDIDATES = {"맑은 고딕", "Malgun Gothic", "Apple SD Gothic Neo"};
    private static final String FONT_FAMILY = findFontFamily();

    /** 칸 크기별 블록 칸의 다섯 면. 같은 크기를 계속 그리므로 한 번만 계산한다. EDT에서만 쓴다. */
    private static final Map<Integer, Shape[]> CELL_FACES = new HashMap<>();

    private Theme() {
    }

    static Color blockColor(int colorId) {
        return BLOCK_COLORS[colorId];
    }

    static Font font(int style, float size) {
        return new Font(FONT_FAMILY, style, 1).deriveFont(size);
    }

    /** 가장자리를 매끄럽게 그리도록 설정한 복사본. 다 쓰면 dispose()해야 한다. */
    static Graphics2D smooth(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    /** 아래쪽에 옅은 그림자가 있는 흰 카드. 그림자 높이만큼 카드는 높이보다 작게 그려진다. */
    static void paintCard(Graphics2D g, int width, int height, int arc, Color fill, Color border) {
        int shadow = 3;
        g.setColor(CARD_SHADOW);
        g.fill(new RoundRectangle2D.Double(0, shadow, width, height - shadow, arc, arc));
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Double(0, 0, width, height - shadow, arc, arc));
        if (border != null) {
            g.setColor(border);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, width - 1, height - shadow - 1, arc, arc));
        }
    }

    /**
     * 블록 한 칸을 그린다. 모서리가 둥글고, 위와 왼쪽 면은 밝게 오른쪽과 아래 면은 어둡게 칠해 입체감을 낸다.
     */
    static void paintBlockCell(Graphics2D graphics, int x, int y, int size, Color color) {
        Shape[] faces = CELL_FACES.computeIfAbsent(size, Theme::cellFaces);
        Graphics2D g = smooth(graphics);
        g.translate(x, y);
        g.setColor(color); // 면 사이의 틈이 보이지 않도록 전체를 먼저 칠한다.
        g.fill(new RoundRectangle2D.Double(0, 0, size, size, arc(size), arc(size)));
        Color[] shades = {mix(color, Color.WHITE, 0.40), mix(color, Color.WHITE, 0.22),
                mix(color, Color.BLACK, 0.18), mix(color, Color.BLACK, 0.30)};
        for (int i = 0; i < faces.length; i++) {
            g.setColor(shades[i]);
            g.fill(faces[i]);
        }
        g.dispose();
    }

    /** 비어 있는 보드 칸. */
    static void paintEmptyCell(Graphics2D g, int x, int y, int size) {
        g.setColor(EMPTY_CELL);
        g.fill(new RoundRectangle2D.Double(x, y, size, size, arc(size), arc(size)));
    }

    /** 놓을 수 있는 자리의 미리보기. 칸을 조금 밝게 하고 블록 색의 테두리를 두른다. */
    static void paintValidPreview(Graphics2D g, int x, int y, int size, Color color) {
        RoundRectangle2D cell = new RoundRectangle2D.Double(x + 1, y + 1, size - 2, size - 2, arc(size), arc(size));
        g.setColor(new Color(255, 255, 255, 34));
        g.fill(cell);
        g.setColor(mix(color, Color.WHITE, 0.20));
        g.setStroke(new BasicStroke(2f));
        g.draw(cell);
    }

    /** 놓을 수 없는 자리의 미리보기. 블록 색(빨강 포함)과 헷갈리지 않도록 색 대신 회색 바탕과 X 표시를 쓴다. */
    static void paintInvalidPreview(Graphics2D g, int x, int y, int size) {
        g.setColor(new Color(0x15, 0x18, 0x30, 150));
        g.fill(new RoundRectangle2D.Double(x, y, size, size, arc(size), arc(size)));
        g.setColor(new Color(0xB9BED2));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int margin = size / 3;
        g.drawLine(x + margin, y + margin, x + size - margin, y + size - margin);
        g.drawLine(x + size - margin, y + margin, x + margin, y + size - margin);
    }

    static Color mix(Color from, Color to, double ratio) {
        return new Color(
                (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * ratio),
                (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * ratio),
                (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * ratio));
    }

    private static int arc(int size) {
        return Math.max(3, size / 5);
    }

    /** 위, 왼쪽, 오른쪽, 아래 면을 둥근 사각형 안쪽으로 잘라 만든다. 가운데 면은 바탕색이 그대로 보인다. */
    private static Shape[] cellFaces(int size) {
        int edge = Math.max(2, size / 8);
        int in = size - edge;
        Polygon[] sides = {
                polygon(0, 0, size, 0, in, edge, edge, edge),
                polygon(0, 0, edge, edge, edge, in, 0, size),
                polygon(size, 0, size, size, in, in, in, edge),
                polygon(0, size, edge, in, in, in, size, size),
        };
        Area outline = new Area(new RoundRectangle2D.Double(0, 0, size, size, arc(size), arc(size)));
        Shape[] faces = new Shape[sides.length];
        for (int i = 0; i < sides.length; i++) {
            Area face = new Area(sides[i]);
            face.intersect(outline);
            faces[i] = face;
        }
        return faces;
    }

    private static Polygon polygon(int... xy) {
        Polygon polygon = new Polygon();
        for (int i = 0; i < xy.length; i += 2) {
            polygon.addPoint(xy[i], xy[i + 1]);
        }
        return polygon;
    }

    private static String findFontFamily() {
        Set<String> installed = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        installed.addAll(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String family : FONT_CANDIDATES) {
            if (installed.contains(family)) {
                return family;
            }
        }
        return Font.SANS_SERIF;
    }
}
