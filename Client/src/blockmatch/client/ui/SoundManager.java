package blockmatch.client.ui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 효과음. 음원 파일 없이 프로그램에서 직접 파형을 만든다.
 * 소리 장치를 여는 일은 시간이 걸릴 수 있어 전용 데몬 스레드에서 하므로 화면과 게임 진행을 막지 않는다.
 * 소리 장치가 없거나 열 수 없으면 소리만 내지 않는다.
 */
final class SoundManager {
    enum Sound { PLACE, BURST, MULTI, VICTORY, DEFEAT }

    private static final float SAMPLE_RATE = 44_100f;
    /** 같은 소리를 겹쳐 낼 수 있는 수. */
    private static final int VOICES = 3;
    /** 아주 짧은 소리는 일부 장치에서 들리지 않으므로 뒤에 무음을 채운다. */
    private static final double MIN_SECONDS = 0.25;

    private static final ScheduledExecutorService THREAD = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "game-sound");
        thread.setDaemon(true);
        return thread;
    });
    /** 아래 값은 THREAD에서만 읽고 쓴다. */
    private static final Map<Sound, Clip[]> CLIPS = new EnumMap<>(Sound.class);
    private static final Map<Sound, Integer> NEXT = new EnumMap<>(Sound.class);
    private static boolean unavailable;

    private SoundManager() {
    }

    /** 첫 소리가 늦지 않도록 미리 만들어 둔다. */
    static void preload() {
        THREAD.execute(SoundManager::load);
    }

    /**
     * delayMillis 뒤에 소리를 낸다. 바로 돌아온다.
     *
     * @param gain 음량 조절(dB). 0이면 원래 크기, 음수면 작게
     */
    static void play(Sound sound, float gain, int delayMillis) {
        THREAD.schedule(() -> start(sound, gain), delayMillis, TimeUnit.MILLISECONDS);
    }

    private static void load() {
        if (unavailable || !CLIPS.isEmpty()) {
            return;
        }
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            for (Sound sound : Sound.values()) {
                byte[] data = toBytes(synthesize(sound));
                Clip[] voices = new Clip[VOICES];
                for (int i = 0; i < VOICES; i++) {
                    voices[i] = AudioSystem.getClip();
                    voices[i].open(format, data, 0, data.length);
                }
                CLIPS.put(sound, voices);
                NEXT.put(sound, 0);
            }
        } catch (Exception e) {
            unavailable = true;
            System.out.println("효과음을 사용할 수 없습니다: " + e.getMessage());
        }
    }

    private static void start(Sound sound, float gain) {
        load();
        Clip[] voices = CLIPS.get(sound);
        if (unavailable || voices == null) {
            return;
        }
        try {
            int index = NEXT.get(sound);
            NEXT.put(sound, (index + 1) % VOICES);
            Clip clip = voices[index];
            clip.stop();
            clip.setFramePosition(0);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), gain)));
            }
            clip.start();
        } catch (RuntimeException e) {
            System.out.println("효과음 재생 실패: " + e.getMessage());
        }
    }

    /** -1~1 범위의 파형. 소리 장치 없이도 만들 수 있다. */
    static double[] synthesize(Sound sound) {
        Random random = new Random(7);
        return switch (sound) {
            // 나무 블록을 내려놓는 짧은 "톡".
            case PLACE -> {
                double[] wave = silence(0.09);
                addSweep(wave, 0, 430, 190, 0.45, 0.022);
                addNoise(wave, random, 0.12, 0.004, 0.5);
                yield wave;
            }
            // 부서지는 잡음, 낮은 울림, 작은 파편 소리.
            case BURST -> {
                double[] wave = silence(0.30);
                addNoise(wave, random, 0.38, 0.055, 0.35);
                addSweep(wave, 0, 170, 60, 0.35, 0.07);
                for (int i = 0; i < 7; i++) {
                    double at = 0.015 + random.nextDouble() * 0.13;
                    double pitch = 1800 + random.nextDouble() * 1800;
                    addSweep(wave, at, pitch, pitch * 0.9, 0.09, 0.015);
                }
                yield wave;
            }
            // 여러 줄을 지웠을 때 올라가는 네 음.
            case MULTI -> {
                double[] wave = silence(0.50);
                double[] notes = {1046.5, 1318.5, 1568.0, 2093.0};
                for (int i = 0; i < notes.length; i++) {
                    addSweep(wave, i * 0.065, notes[i], notes[i], 0.22, 0.09);
                }
                yield wave;
            }
            // 이겼을 때: 빠르게 올라가는 네 음 뒤에 밝은 화음이 길게 울리고, 높은 반짝임이 흩어진다.
            case VICTORY -> {
                double[] wave = silence(1.6);
                double[] run = {523.3, 659.3, 784.0, 1046.5};
                for (int i = 0; i < run.length; i++) {
                    addSweep(wave, i * 0.09, run[i], run[i], 0.20, 0.12);
                }
                for (double note : run) {
                    addSweep(wave, 0.40, note, note, 0.11, 0.55);
                    addSweep(wave, 0.40, note * 2, note * 2, 0.03, 0.30);
                }
                for (int i = 0; i < 6; i++) {
                    double pitch = 2600 + random.nextDouble() * 1800;
                    addSweep(wave, 0.45 + i * 0.11, pitch, pitch, 0.05, 0.05);
                }
                yield wave;
            }
            // 졌을 때: 낮게 반음씩 내려가는 세 음과 마지막에 길게 늘어지며 처지는 음.
            case DEFEAT -> {
                double[] wave = silence(1.9);
                double[] notes = {392.0, 370.0, 349.2};
                for (int i = 0; i < notes.length; i++) {
                    addSweep(wave, i * 0.32, notes[i], notes[i], 0.20, 0.22);
                    addSweep(wave, i * 0.32, notes[i] / 2, notes[i] / 2, 0.10, 0.22);
                }
                addSweep(wave, 0.96, 330.0, 247.0, 0.22, 0.60);
                addSweep(wave, 0.96, 165.0, 123.5, 0.12, 0.60);
                yield wave;
            }
        };
    }

    private static double[] silence(double seconds) {
        return new double[(int) (SAMPLE_RATE * Math.max(seconds, MIN_SECONDS))];
    }

    /** at초부터 높이가 from에서 to로 바뀌며 decay초 단위로 작아지는 사인파를 더한다. */
    private static void addSweep(double[] wave, double at, double from, double to, double volume, double decay) {
        int first = (int) (at * SAMPLE_RATE);
        double phase = 0;
        for (int i = first; i < wave.length; i++) {
            double t = (i - first) / (double) SAMPLE_RATE;
            double frequency = to + (from - to) * Math.exp(-t / decay);
            phase += 2 * Math.PI * frequency / SAMPLE_RATE;
            double attack = Math.min(1, t / 0.002);
            wave[i] += Math.sin(phase) * volume * attack * Math.exp(-t / decay);
        }
    }

    /** 낮은 음 위주로 거른 잡음을 더한다. smoothing이 1에 가까울수록 날카롭다. */
    private static void addNoise(double[] wave, Random random, double volume, double decay, double smoothing) {
        double previous = 0;
        for (int i = 0; i < wave.length; i++) {
            double t = i / (double) SAMPLE_RATE;
            previous += smoothing * (random.nextDouble() * 2 - 1 - previous);
            wave[i] += previous * volume * Math.min(1, t / 0.001) * Math.exp(-t / decay);
        }
    }

    /** 16비트 리틀 엔디언으로 바꾼다. 끝에서 딸깍거리지 않도록 마지막 5ms는 줄인다. */
    private static byte[] toBytes(double[] wave) {
        int fade = (int) (SAMPLE_RATE * 0.005);
        byte[] data = new byte[wave.length * 2];
        for (int i = 0; i < wave.length; i++) {
            double out = Math.min(1, (wave.length - 1 - i) / (double) fade);
            int value = (int) Math.round(Math.max(-1, Math.min(1, wave[i] * out)) * Short.MAX_VALUE);
            data[i * 2] = (byte) value;
            data[i * 2 + 1] = (byte) (value >> 8);
        }
        return data;
    }
}
