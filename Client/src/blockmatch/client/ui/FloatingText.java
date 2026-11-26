package blockmatch.client.ui;

import blockmatch.common.GameConfig;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 지운 자리 근처에서 위로 떠오르며 사라지는 점수 글자. 위에서부터 동시 삭제 이름(DOUBLE 등), 획득 점수, 콤보를 쓴다.
 * 동시 삭제는 한 번에 지운 줄 수이고, 콤보는 연속으로 줄을 지운 배치 횟수라 서로 따로 표시한다.
 */
final class FloatingText {
    private static final String[] MULTI_NAMES = {"DOUBLE!", "TRIPLE!", "QUADRA!", "AMAZING!"};
    private static final int POP_MILLIS = 160;

    private final long start;
    /** 격자 기준 가운데 위치(칸 단위). */
    private final double x;
    private final double y;
    private final List<Line> lines = new ArrayList<>();
    private long end;
    /**
     * 외곽선 글자는 매 프레임 만들기에 느리므로 한 번 그려 두고 옮기고 늘려서 쓴다.
     * 칸 크기나 화면 배율이 바뀌면 다시 만든다.
     */
    private List<BufferedImage> images;
    private int imagePitch;
    private double imageDensity;

    /** @param size 칸 크기를 1로 한 글자 크기 */
    private record Line(String text, double size, Color color) {
    }

    FloatingText(long start, double x, double y, int gained, int clearedLines, int combo) {
        this.start = start;
        this.x = x;
        this.y = y;
        end = start + EffectConfig.TEXT_MILLIS;
        if (clearedLines >= 2) {
            String name = MULTI_NAMES[Math.min(clearedLines - 2, MULTI_NAMES.length - 1)];
            lines.add(new Line(name, 0.72, EffectConfig.TEXT_MULTI));
        }
        lines.add(new Line(String.format("+%,d", gained), 0.66, EffectConfig.TEXT_POINTS));
        if (combo >= 2) {
            lines.add(new Line("COMBO ×" + combo, 0.50, EffectConfig.TEXT_COMBO));
        }
    }

    boolean isFinished(long now) {
        return now >= end;
    }

    /** 새 글자와 겹치지 않도록 곧 사라지게 한다. */
    void hurry(long now) {
        end = Math.min(end, now + EffectConfig.TEXT_HURRY_MILLIS);
    }

    /** 글자가 격자 밖으로 나가 잘리지 않도록 위치를 안쪽으로 당겨서 그린다. */
    void paint(Graphics2D graphics, long now, int originX, int originY, int pitch) {
        long age = now - start;
        if (age < 0 || now >= end) {
            return;
        }
        double progress = Math.min(1, age / (double) EffectConfig.TEXT_MILLIS);
        double rise = EffectConfig.TEXT_RISE * (1 - Math.pow(1 - progress, 3));
        // 작게 나타나 살짝 커졌다가 제 크기로 돌아온다.
        double pop = Math.min(1, age / (double) POP_MILLIS);
        double scale = 0.6 + 0.4 * pop + 0.18 * Math.sin(pop * Math.PI);
        double fadeFrom = end - EffectConfig.TEXT_MILLIS * 0.35;
        double alpha = now < fadeFrom ? 1 : (end - now) / (end - fadeFrom);
        if (end - now < EffectConfig.TEXT_HURRY_MILLIS) {
            alpha = Math.min(alpha, (end - now) / (double) EffectConfig.TEXT_HURRY_MILLIS);
        }

        Graphics2D g = (Graphics2D) graphics.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, Math.min(1, alpha))));

        // 화면 배율(125% 등)만큼 크게 그려 두어야 흐려지지 않는다.
        double density = Math.max(1, Math.abs(g.getTransform().getScaleX()));
        if (images == null || imagePitch != pitch || imageDensity != density) {
            images = lines.stream().map(line -> render(line, pitch, density)).toList();
            imagePitch = pitch;
            imageDensity = density;
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double height = lines.stream().mapToDouble(line -> line.size() * 1.1).sum();
        double grid = GameConfig.BOARD_SIZE;
        double top = Math.max(EffectConfig.TEXT_RISE + 0.1, Math.min(grid - height - 0.1, y - height / 2)) - rise;
        double lineTop = originY + top * pitch;
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            BufferedImage image = images.get(i);
            double half = image.getWidth() / density / 2;
            double center = originX + Math.max(half, Math.min(grid * pitch - half, x * pitch));
            AffineTransform place = AffineTransform.getTranslateInstance(center, lineTop + line.size() * 1.1 * pitch / 2);
            place.scale(scale / density, scale / density);
            place.translate(-image.getWidth() / 2.0, -image.getHeight() / 2.0);
            g.drawImage(image, place, null);
            lineTop += line.size() * 1.1 * pitch;
        }
        g.dispose();
    }

    private static BufferedImage render(Line line, int pitch, double density) {
        float fontSize = (float) Math.max(8, line.size() * pitch);
        float stroke = Math.max(2f, fontSize / 6);
        Font font = Theme.font(Font.BOLD, fontSize);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measure = Theme.smooth(probe.getGraphics());
        GlyphVector glyphs = font.createGlyphVector(measure.getFontRenderContext(), line.text());
        measure.dispose();
        Shape outline = glyphs.getOutline();
        Rectangle2D bounds = outline.getBounds2D();

        BufferedImage image = new BufferedImage((int) Math.ceil((bounds.getWidth() + stroke * 2) * density),
                (int) Math.ceil((bounds.getHeight() + stroke * 2) * density), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = Theme.smooth(image.getGraphics());
        g.scale(density, density);
        g.translate(stroke - bounds.getX(), stroke - bounds.getY());
        g.setColor(EffectConfig.TEXT_OUTLINE);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(outline);
        g.setColor(line.color());
        g.fill(outline);
        g.dispose();
        return image;
    }
}
