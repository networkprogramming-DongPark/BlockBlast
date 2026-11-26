package blockmatch.client.ui;

import blockmatch.common.Board;
import blockmatch.common.GameConfig;
import blockmatch.common.PlaceResult;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 줄 삭제 한 번의 반짝임, 줄을 지나가는 빛, 화면 흔들림. 블록 조각은 ParticleSystem이 맡는다.
 * 모두 시작 시각에서 지난 시간으로 계산한다.
 */
final class ClearEffect {
    private static final int COUNT = GameConfig.BOARD_SIZE;
    private static final float[] BEAM_STOPS = {0f, 0.65f, 1f};

    private final long start;
    private final PlaceResult result;

    ClearEffect(long start, PlaceResult result) {
        this.start = start;
        this.result = result;
    }

    boolean isFinished(long now) {
        int longest = Math.max(EffectConfig.FLASH_MILLIS + EffectConfig.FLASH_FADE_MILLIS,
                Math.max(EffectConfig.BEAM_DELAY_MILLIS + EffectConfig.BEAM_MILLIS,
                        EffectConfig.FLASH_MILLIS + EffectConfig.SHAKE_MILLIS));
        return now - start >= longest;
    }

    /**
     * 지워진 칸을 원래 색으로 그리고 흰빛을 덧씌운다. 부서진 뒤에는 흰빛만 잠깐 남는다.
     * 게임 상태의 보드에서는 이미 지워진 칸이므로 여기서 그려야 보인다.
     */
    void paintFlash(Graphics2D graphics, long now, int originX, int originY, int pitch, int gap) {
        long age = now - start;
        if (age >= EffectConfig.FLASH_MILLIS + EffectConfig.FLASH_FADE_MILLIS) {
            return;
        }
        boolean burst = age >= EffectConfig.FLASH_MILLIS;
        double rise = Math.min(1, age / (EffectConfig.FLASH_MILLIS * 0.6));
        double fade = burst ? 1 - (age - EffectConfig.FLASH_MILLIS) / (double) EffectConfig.FLASH_FADE_MILLIS : 1;
        float white = (float) Math.max(0, Math.min(1, EffectConfig.FLASH_WHITE_ALPHA * rise * fade));
        int size = pitch - gap;
        int grow = burst ? 0 : (int) Math.round(size * EffectConfig.FLASH_GROW * rise / 2);

        Graphics2D g = (Graphics2D) graphics.create();
        Board cleared = result.cleared();
        for (int row = 0; row < COUNT; row++) {
            for (int col = 0; col < COUNT; col++) {
                int value = cleared.get(row, col);
                if (value == Board.EMPTY) {
                    continue;
                }
                int x = originX + col * pitch + gap / 2 - grow;
                int y = originY + row * pitch + gap / 2 - grow;
                int grown = size + grow * 2;
                g.setComposite(AlphaComposite.SrcOver);
                if (!burst) {
                    Theme.paintBlockCell(g, x, y, grown, Theme.blockColor(value));
                }
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, white));
                g.setColor(Color.WHITE);
                double arc = grown / 5.0;
                g.fill(new RoundRectangle2D.Double(x, y, grown, grown, arc, arc));
            }
        }
        g.dispose();
    }

    /** 지운 가로줄은 왼쪽에서 오른쪽으로, 세로줄은 위에서 아래로 빛이 지나간다. 여러 줄은 동시에 움직인다. */
    void paintBeams(Graphics2D graphics, long now, int originX, int originY, int pitch) {
        long age = now - start - EffectConfig.BEAM_DELAY_MILLIS;
        if (age < 0 || age >= EffectConfig.BEAM_MILLIS) {
            return;
        }
        double progress = age / (double) EffectConfig.BEAM_MILLIS;
        double eased = 1 - (1 - progress) * (1 - progress);
        double length = EffectConfig.BEAM_LENGTH * pitch;
        double grid = pitch * (double) COUNT;
        double head = eased * (grid + length);
        int alpha = (int) Math.round(EffectConfig.BEAM_ALPHA * (1 - progress * progress));
        Color[] colors = {new Color(255, 255, 255, 0), new Color(255, 255, 255, alpha), new Color(255, 255, 255, 0)};

        Graphics2D g = (Graphics2D) graphics.create();
        g.clip(new Rectangle2D.Double(originX, originY, grid, grid));
        for (int line = 0; line < COUNT; line++) {
            if ((result.fullRows() & 1 << line) != 0) {
                double x = originX + head - length;
                double y = originY + line * pitch;
                g.setPaint(new LinearGradientPaint(new Point2D.Double(x, y), new Point2D.Double(x + length, y),
                        BEAM_STOPS, colors));
                g.fill(new Rectangle2D.Double(x, y + pitch * 0.08, length, pitch * 0.84));
            }
            if ((result.fullCols() & 1 << line) != 0) {
                double x = originX + line * pitch;
                double y = originY + head - length;
                g.setPaint(new LinearGradientPaint(new Point2D.Double(x, y), new Point2D.Double(x, y + length),
                        BEAM_STOPS, colors));
                g.fill(new Rectangle2D.Double(x + pitch * 0.08, y, pitch * 0.84, length));
            }
        }
        g.dispose();
    }

    /** 부서지는 순간부터 SHAKE_MILLIS 동안의 가로 흔들림(칸 단위). 처음과 끝은 0이다. */
    double shakeX(long now) {
        return shake(now, 3, 1.0);
    }

    double shakeY(long now) {
        return shake(now, 4, 0.6);
    }

    private double shake(long now, int waves, double strength) {
        long age = now - start - EffectConfig.FLASH_MILLIS;
        if (age < 0 || age >= EffectConfig.SHAKE_MILLIS) {
            return 0;
        }
        double progress = age / (double) EffectConfig.SHAKE_MILLIS;
        int level = Math.min(result.lineCount(), EffectConfig.SHAKE_AMPLITUDE.length) - 1;
        return EffectConfig.SHAKE_AMPLITUDE[level] * strength * (1 - progress)
                * Math.sin(progress * Math.PI * 2 * waves);
    }
}
