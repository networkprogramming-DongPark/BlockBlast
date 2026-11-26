package blockmatch.client.ui;

import java.awt.Color;

/**
 * 줄 삭제 효과의 시간, 개수, 세기를 모아 둔 곳. 효과의 느낌은 여기 값만 바꿔 조정한다.
 * 시간은 밀리초, 길이는 보드 칸 크기를 1로 한 값이라 화면 크기가 바뀌어도 같은 모양으로 보인다.
 */
final class EffectConfig {
    /** 화면을 다시 그리는 간격. 약 60FPS. */
    static final int FRAME_MILLIS = 16;

    /** 반짝임. 이 시간이 지나면 블록이 부서진다. */
    static final int FLASH_MILLIS = 100;
    /** 부서진 뒤에도 흰빛이 남아 사라지는 시간. */
    static final int FLASH_FADE_MILLIS = 40;
    static final float FLASH_WHITE_ALPHA = 0.65f;
    /** 반짝일 때 블록이 커지는 비율. */
    static final double FLASH_GROW = 0.08;

    /** 파티클. 반짝임이 끝날 때 생기므로 전체 길이는 FLASH_MILLIS + 수명이다(600ms 이내). */
    static final int PARTICLES_MIN = 8;
    static final int PARTICLES_MAX = 15;
    /** 한 줄을 더 지울 때마다 칸당 최소 개수를 이만큼 늘린다. */
    static final int PARTICLES_PER_EXTRA_LINE = 2;
    static final int PARTICLES_LIMIT = 1500;
    static final int PARTICLE_MIN_LIFE = 320;
    static final int PARTICLE_MAX_LIFE = 480;
    static final double PARTICLE_MIN_SIZE = 0.15;
    static final double PARTICLE_MAX_SIZE = 0.32;
    /** 수명이 끝날 때의 크기 비율. */
    static final double PARTICLE_END_SCALE = 0.35;
    /** 처음 속력(칸/초). */
    static final double PARTICLE_MIN_SPEED = 2.5;
    static final double PARTICLE_MAX_SPEED = 6.5;
    /** 처음에 위로 튀는 속력의 최댓값(칸/초). */
    static final double PARTICLE_LIFT = 2.8;
    /** 감속 계수(1/초)와 중력(칸/초²). */
    static final double PARTICLE_DRAG = 4.5;
    static final double PARTICLE_GRAVITY = 24.0;
    static final double PARTICLE_MAX_SPIN = 12.0;
    /** 한 줄을 더 지울 때마다 속력을 이만큼 키운다. */
    static final double SPEED_PER_EXTRA_LINE = 0.12;
    static final double SPEED_BOOST_LIMIT = 1.4;

    /** 줄을 지나가는 빛. */
    static final int BEAM_DELAY_MILLIS = 40;
    static final int BEAM_MILLIS = 260;
    static final double BEAM_LENGTH = 3.0;
    static final int BEAM_ALPHA = 150;

    /** 화면 흔들림. 세기는 지운 줄이 1개, 2개, 3개 이상일 때의 값이다. */
    static final int SHAKE_MILLIS = 130;
    static final double[] SHAKE_AMPLITUDE = {0.045, 0.085, 0.125};
    static final double SHAKE_LIMIT = 0.14;

    /** 점수와 콤보 글자. */
    static final int TEXT_MILLIS = 900;
    static final double TEXT_RISE = 0.85;
    /** 새 글자가 뜨면 이전 글자는 이 시간 안에 사라진다. */
    static final int TEXT_HURRY_MILLIS = 150;
    static final Color TEXT_POINTS = new Color(0xFFE48A);
    static final Color TEXT_MULTI = new Color(0xBDE7FF);
    static final Color TEXT_COMBO = new Color(0xFFB9A0);
    static final Color TEXT_OUTLINE = new Color(0x171B3A);

    /** 효과음. 상대 보드의 소리는 이만큼(dB) 작게 낸다. */
    static final float OPPONENT_SOUND_GAIN = -10f;

    private EffectConfig() {
    }
}
