package blockmatch.client.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 보드 하나의 블록 조각들을 만들고, 그리고, 수명이 끝나면 없앤다. EDT에서만 쓴다. */
final class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    /**
     * (row, col) 칸의 블록을 조각낸다. 조각은 처음에 칸 전체를 채우듯 놓였다가 바깥으로 흩어진다.
     *
     * @param lines 이번에 지운 줄 수. 많을수록 조각이 많고 빠르다.
     * @param birth 조각이 생기는 시각(ms)
     */
    void burst(int row, int col, Color color, int lines, long birth) {
        int min = Math.min(EffectConfig.PARTICLES_MAX,
                EffectConfig.PARTICLES_MIN + (lines - 1) * EffectConfig.PARTICLES_PER_EXTRA_LINE);
        int count = min + random.nextInt(EffectConfig.PARTICLES_MAX - min + 1);
        count = Math.min(count, EffectConfig.PARTICLES_LIMIT - particles.size());
        double boost = Math.min(EffectConfig.SPEED_BOOST_LIMIT, 1 + (lines - 1) * EffectConfig.SPEED_PER_EXTRA_LINE);
        // 블록 칸의 다섯 면과 비슷한 밝기들. 원래 색을 알아볼 수 있게 가운데 색을 두 번 넣는다.
        Color[] shades = {color, color, Theme.mix(color, Color.WHITE, 0.40), Theme.mix(color, Color.WHITE, 0.20),
                Theme.mix(color, Color.BLACK, 0.18)};

        int side = (int) Math.ceil(Math.sqrt(count));
        for (int i = 0; i < count; i++) {
            double inX = (i % side + 0.5 + between(-0.3, 0.3)) / side;
            double inY = (i / side + 0.5 + between(-0.3, 0.3)) / side;
            double direction = Math.atan2(inY - 0.5, inX - 0.5) + between(-0.6, 0.6);
            double speed = between(EffectConfig.PARTICLE_MIN_SPEED, EffectConfig.PARTICLE_MAX_SPEED) * boost;
            particles.add(new Particle(
                    birth,
                    (int) between(EffectConfig.PARTICLE_MIN_LIFE, EffectConfig.PARTICLE_MAX_LIFE),
                    col + inX,
                    row + inY,
                    Math.cos(direction) * speed,
                    Math.sin(direction) * speed - between(0, EffectConfig.PARTICLE_LIFT) * boost,
                    between(-0.5, 0.5),
                    between(-EffectConfig.PARTICLE_MAX_SPIN, EffectConfig.PARTICLE_MAX_SPIN),
                    between(EffectConfig.PARTICLE_MIN_SIZE, EffectConfig.PARTICLE_MAX_SIZE),
                    shades[random.nextInt(shades.length)],
                    shard()));
        }
    }

    void removeDead(long now) {
        particles.removeIf(particle -> particle.isDead(now));
    }

    void paint(Graphics2D graphics, long now, double originX, double originY, int pitch) {
        Graphics2D g = (Graphics2D) graphics.create();
        AffineTransform base = g.getTransform();
        for (Particle particle : particles) {
            particle.paint(g, now, originX, originY, pitch);
            g.setTransform(base);
        }
        g.dispose();
    }

    int size() {
        return particles.size();
    }

    boolean isEmpty() {
        return particles.isEmpty();
    }

    void clear() {
        particles.clear();
    }

    /** 모서리가 조금씩 어긋난 사각형. 둥근 점이 아니라 깨진 조각처럼 보이게 한다. */
    private Shape shard() {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(-0.5 + between(0, 0.3), -0.5 + between(0, 0.2));
        path.lineTo(0.5 - between(0, 0.2), -0.5 + between(0, 0.3));
        path.lineTo(0.5 - between(0, 0.3), 0.5 - between(0, 0.2));
        path.lineTo(-0.5 + between(0, 0.2), 0.5 - between(0, 0.3));
        path.closePath();
        return path;
    }

    private double between(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}
