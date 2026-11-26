package blockmatch.client.ui;

import blockmatch.common.Board;
import blockmatch.common.GameConfig;
import blockmatch.common.PlaceResult;

import javax.swing.Timer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 보드 하나에서 실행 중인 줄 삭제 효과 모음. 게임 상태와는 무관하게 화면에만 그린다.
 * 효과가 있는 동안만 타이머를 돌려 다시 그리고, 모두 끝나면 멈춘다. 여러 효과가 겹쳐도 각자 시간대로 진행한다.
 * EDT에서만 쓴다.
 */
final class BoardEffects {
    private final List<ClearEffect> clears = new ArrayList<>();
    private final List<FloatingText> texts = new ArrayList<>();
    private final ParticleSystem particles = new ParticleSystem();
    private final Timer timer;
    private final Runnable repaint;

    BoardEffects(Runnable repaint) {
        this.repaint = repaint;
        timer = new Timer(EffectConfig.FRAME_MILLIS, event -> tick());
    }

    /**
     * 효과를 한 번 화면 밖에 그려 둔다. 처음 그릴 때의 초기화 때문에 첫 폭발이 끊겨 보이지 않게 한다.
     */
    static void warmUp() {
        Board cells = new Board();
        for (int col = 0; col < GameConfig.BOARD_SIZE; col++) {
            cells.set(0, col, 1);
        }
        int pitch = 48;
        BufferedImage image = new BufferedImage(pitch * GameConfig.BOARD_SIZE, pitch * GameConfig.BOARD_SIZE,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = Theme.smooth(image.getGraphics());
        ClearEffect clear = new ClearEffect(0, new PlaceResult(0, 1, 0, cells));
        ParticleSystem pieces = new ParticleSystem();
        pieces.burst(0, 0, Theme.blockColor(1), 1, 0);
        clear.paintFlash(g, EffectConfig.FLASH_MILLIS / 2, 0, 0, pitch, 3);
        clear.paintBeams(g, EffectConfig.FLASH_MILLIS, 0, 0, pitch);
        pieces.paint(g, EffectConfig.PARTICLE_MIN_LIFE / 2, 0, 0, pitch);
        new FloatingText(0, 4, 4, 100, 2, 2).paint(g, EffectConfig.TEXT_MILLIS / 2, 0, 0, pitch);
        g.dispose();
    }

    /** 지운 줄이 있는 배치의 효과를 시작한다. */
    void play(PlaceResult result, int combo) {
        int lines = result.lineCount();
        if (lines == 0) {
            return;
        }
        long now = now();
        clears.add(new ClearEffect(now, result));

        Board cleared = result.cleared();
        double sumX = 0;
        double sumY = 0;
        int cells = 0;
        // 지운 칸마다 한 번씩만 돌므로 가로줄과 세로줄이 겹친 칸에서도 조각은 한 번만 생긴다.
        for (int row = 0; row < GameConfig.BOARD_SIZE; row++) {
            for (int col = 0; col < GameConfig.BOARD_SIZE; col++) {
                int value = cleared.get(row, col);
                if (value != Board.EMPTY) {
                    particles.burst(row, col, Theme.blockColor(value), lines, now + EffectConfig.FLASH_MILLIS);
                    sumX += col + 0.5;
                    sumY += row + 0.5;
                    cells++;
                }
            }
        }
        if (cells > 0) {
            texts.forEach(text -> text.hurry(now));
            texts.add(new FloatingText(now + EffectConfig.FLASH_MILLIS, sumX / cells, sumY / cells,
                    result.gained(), lines, combo));
        }
        timer.start();
        repaint.run();
    }

    /** 실행 중인 효과를 모두 버리고 타이머를 멈춘다. */
    void clear() {
        timer.stop();
        clears.clear();
        texts.clear();
        particles.clear();
    }

    boolean isRunning() {
        return timer.isRunning();
    }

    int particleCount() {
        return particles.size();
    }

    /** 지금 보드를 밀어 그릴 거리(칸 단위). 그리기에만 쓰며 마우스 좌표 계산에는 쓰지 않는다. */
    double shakeX() {
        long now = now();
        return limit(clears.stream().mapToDouble(effect -> effect.shakeX(now)).sum());
    }

    double shakeY() {
        long now = now();
        return limit(clears.stream().mapToDouble(effect -> effect.shakeY(now)).sum());
    }

    /** 블록 위, 놓을 자리 미리보기 아래에 그릴 것: 반짝임과 줄을 지나가는 빛. */
    void paintUnder(Graphics2D g, int originX, int originY, int pitch, int gap) {
        long now = now();
        for (ClearEffect effect : clears) {
            effect.paintFlash(g, now, originX, originY, pitch, gap);
        }
        for (ClearEffect effect : clears) {
            effect.paintBeams(g, now, originX, originY, pitch);
        }
    }

    /** 맨 위에 그릴 것: 블록 조각과 점수 글자. */
    void paintOver(Graphics2D g, int originX, int originY, int pitch) {
        long now = now();
        particles.paint(g, now, originX, originY, pitch);
        for (FloatingText text : texts) {
            text.paint(g, now, originX, originY, pitch);
        }
    }

    private void tick() {
        long now = now();
        clears.removeIf(effect -> effect.isFinished(now));
        texts.removeIf(text -> text.isFinished(now));
        particles.removeDead(now);
        if (clears.isEmpty() && texts.isEmpty() && particles.isEmpty()) {
            timer.stop();
        }
        repaint.run();
    }

    private static double limit(double shake) {
        return Math.max(-EffectConfig.SHAKE_LIMIT, Math.min(EffectConfig.SHAKE_LIMIT, shake));
    }

    /** 시스템 시각이 바뀌어도 영향이 없는 경과 시간(ms). */
    private static long now() {
        return System.nanoTime() / 1_000_000;
    }
}
