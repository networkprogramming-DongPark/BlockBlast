package blockmatch.client.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * 부서진 블록 조각 하나. 처음 위치와 속도만 갖고, 지금 위치는 지난 시간으로 바로 계산한다.
 * 그래서 화면을 몇 번 그리든 같은 시각에는 같은 자리에 있다.
 * 위치와 크기는 칸 단위(보드 격자의 왼쪽 위가 0, 0)다.
 *
 * @param birth 조각이 생기는 시각(ms)
 * @param life  수명(ms)
 * @param shape 한 변이 1이고 중심이 (0, 0)인 조각 모양
 */
record Particle(long birth, int life, double x, double y, double vx, double vy, double angle, double spin,
                double size, Color color, Shape shape) {

    boolean isDead(long now) {
        return now - birth >= life;
    }

    /** 속력이 EffectConfig.PARTICLE_DRAG로 줄고 PARTICLE_GRAVITY로 떨어지는 움직임. */
    double xAt(double seconds) {
        return x + vx * damped(seconds);
    }

    double yAt(double seconds) {
        double terminal = EffectConfig.PARTICLE_GRAVITY / EffectConfig.PARTICLE_DRAG;
        return y + (vy - terminal) * damped(seconds) + terminal * seconds;
    }

    /**
     * 아직 생기기 전이거나 수명이 끝났으면 그리지 않는다. 조각이 많아 Graphics를 복사하지 않고 함께 쓰므로,
     * 부른 쪽이 변환을 되돌려야 한다.
     */
    void paint(Graphics2D g, long now, double originX, double originY, int pitch) {
        long age = now - birth;
        if (age < 0 || age >= life) {
            return;
        }
        double progress = age / (double) life;
        double seconds = age / 1000.0;
        double scale = size * pitch * (1 - (1 - EffectConfig.PARTICLE_END_SCALE) * progress);
        // 처음에는 또렷하게 두고 뒤로 갈수록 빠르게 흐려진다.
        float alpha = (float) Math.max(0, Math.min(1, 1 - progress * progress * progress));

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.translate(originX + xAt(seconds) * pitch, originY + yAt(seconds) * pitch);
        g.rotate(angle + spin * seconds);
        g.scale(scale, scale);
        g.setColor(color);
        g.fill(shape);
    }

    private static double damped(double seconds) {
        return (1 - Math.exp(-EffectConfig.PARTICLE_DRAG * seconds)) / EffectConfig.PARTICLE_DRAG;
    }
}
