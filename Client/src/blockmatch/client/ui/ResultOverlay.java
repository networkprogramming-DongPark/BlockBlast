package blockmatch.client.ui;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * 게임이 끝나면 게임 화면(두 보드와 가운데 영역) 위에 덮어 그리는 결과 화면.
 *
 * 이기면 금빛 "WIN" 글자가 튀어나오며 빛줄기, 반짝이는 별, 색종이가 함께 나타나고,
 * 지면 화면이 어두워지며 비가 내리고 회색 "LOSE" 글자가 한 글자씩 힘없이 떨어진다. 비기면 은색 "DRAW"를 보여 준다.
 * 아래에는 결과 설명, 다시 하기 안내 문구, "메인으로 돌아가기"와 "다시 게임하기" 버튼이 있다.
 *
 * 효과의 모든 움직임은 보이기 시작한 뒤 지난 시간만으로 계산하므로, 몇 번을 다시 그려도 같은 시각에는 같은 모습이다.
 * 보이는 동안만 타이머를 돌린다. EDT에서만 쓴다.
 */
final class ResultOverlay extends JComponent {
    enum Mood { WIN, LOSE, DRAW }

    /** 큰 글자 아래 설명, 점수, 안내 문구, 버튼 사이의 간격. 안내 문구가 비어 있어도 버튼 자리는 그대로다. */
    private static final int DETAIL_GAP = 62;
    private static final int LINE_GAP = 24;
    private static final int STATUS_GAP = 34;
    private static final int BUTTON_GAP = 34;
    private static final int BUTTON_HEIGHT = 42;
    private static final int ARC = 24;

    private static final Color GOLD_LIGHT = new Color(0xFFF1B8);
    private static final Color GOLD = new Color(0xFFD25E);
    private static final Color GOLD_DEEP = new Color(0xE79A2B);
    private static final Color LOSE_LIGHT = new Color(0xA7AECA);
    private static final Color LOSE_DARK = new Color(0x4E5675);
    private static final Color RAIN = new Color(0x9AA6CC);
    private static final Color SILVER_LIGHT = new Color(0xF4F6FC);
    private static final Color SILVER = new Color(0xB9C1DA);

    /** 색종이 색: 블록 색과 금색. */
    private static final Color[] CONFETTI = {
            new Color(0x8399EF), new Color(0xDDB57C), new Color(0xA388DF), new Color(0xA4D297),
            new Color(0xE9D780), new Color(0xCA7D83), GOLD, Color.WHITE};

    private final RoundButton mainButton = new RoundButton("메인으로 돌아가기", false).withSize(170, BUTTON_HEIGHT);
    private final RoundButton rematchButton = new RoundButton("다시 게임하기", true).withSize(170, BUTTON_HEIGHT);
    private final Timer timer = new Timer(EffectConfig.FRAME_MILLIS, event -> repaint());

    private Mood mood = Mood.DRAW;
    private String detail = "";
    private String scores = "";
    private String status = "";
    private Color statusColor = Color.WHITE;
    private long shownAt;
    private BooleanSupplier onRematch = () -> false;
    private Runnable onMain = () -> { };

    ResultOverlay() {
        setLayout(null);
        setOpaque(false);
        setVisible(false);
        // 아래의 보드와 트레이가 마우스를 받지 않도록 결과 화면이 모두 받는다.
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });
        mainButton.addActionListener(event -> onMain.run());
        rematchButton.addActionListener(event -> {
            if (onRematch.getAsBoolean()) {
                // 서버의 확인(REMATCH)을 기다리지 않고 바로 눌린 상태로 바꿔 두 번 누르지 않게 한다.
                showWaiting();
            } else {
                setStatus("서버에 연결되어 있지 않아요.", Theme.RED);
            }
        });
        add(mainButton);
        add(rematchButton);
    }

    /**
     * 결과 화면을 띄우고 효과를 처음부터 시작한다.
     *
     * @param onRematch 다시 게임하기를 누르면 부른다. 서버로 보냈으면 true를 돌려줘야 한다.
     * @param onMain 메인으로 돌아가기를 누르면 부른다.
     */
    void open(Mood mood, String detail, String scores, BooleanSupplier onRematch, Runnable onMain) {
        this.mood = mood;
        this.detail = detail;
        this.scores = scores;
        this.onRematch = onRematch;
        this.onMain = onMain;
        rematchButton.setText("다시 게임하기");
        rematchButton.setEnabled(true);
        setStatus("", Color.WHITE);
        shownAt = now();
        setVisible(true);
        revalidate();
        timer.start();
        repaint();
        rematchButton.requestFocusInWindow();
    }

    /** 새 게임이 시작되면 닫는다. */
    void close() {
        timer.stop();
        setVisible(false);
    }

    Mood mood() {
        return mood;
    }

    /** 내가 다시 하기를 눌렀고 상대를 기다린다. */
    void showWaiting() {
        rematchButton.setEnabled(false);
        rematchButton.setText("기다리는 중");
        setStatus("상대방을 기다리는 중...", new Color(0xBFD0FF));
    }

    /** 상대가 먼저 다시 하기를 눌렀다. 나도 누르면 바로 새 게임이 시작된다. */
    void showInvite(String name) {
        if (rematchButton.isEnabled()) {
            setStatus(name + "님: 한판 더해요!", GOLD);
        }
    }

    /** 상대가 나갔다. 다시 하기는 할 수 없고 메인으로 돌아가기만 남는다. */
    void showOpponentLeft() {
        rematchButton.setEnabled(false);
        rematchButton.setText("다시 게임하기");
        setStatus("상대방이 나갔습니다.", new Color(0xFF8E9C));
    }

    String statusText() {
        return status;
    }

    private void setStatus(String text, Color color) {
        status = text;
        statusColor = color;
        repaint();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    // ---------------------------------------------------------------- 배치

    /** 큰 글자 크기. 게임 화면 크기에 맞춘다. */
    private float titleSize() {
        return (float) Math.max(60, Math.min(170, Math.min(getWidth() * 0.2, getHeight() * 0.3)));
    }

    /** 큰 글자의 기준선. 글자, 설명, 버튼 묶음이 세로 가운데에 오도록 한다. */
    private int baseline() {
        float size = titleSize();
        int block = Math.round(size * 0.74f) + DETAIL_GAP + LINE_GAP + STATUS_GAP + BUTTON_GAP + BUTTON_HEIGHT;
        return (getHeight() - block) / 2 + Math.round(size * 0.74f);
    }

    @Override
    public void doLayout() {
        int y = baseline() + DETAIL_GAP + LINE_GAP + STATUS_GAP + BUTTON_GAP - 12;
        int width = mainButton.getPreferredSize().width;
        int gap = 12;
        int x = (getWidth() - width * 2 - gap) / 2;
        mainButton.setBounds(x, y, width, BUTTON_HEIGHT);
        rematchButton.setBounds(x + width + gap, y, width, BUTTON_HEIGHT);
    }

    // ---------------------------------------------------------------- 그리기

    @Override
    protected void paintComponent(Graphics graphics) {
        double t = (now() - shownAt) / 1000.0;
        Graphics2D g = Theme.smooth(graphics);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        Shape area = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), ARC, ARC);
        g.clip(area);
        switch (mood) {
            case WIN -> paintWin(g, t);
            case LOSE -> paintLose(g, t);
            case DRAW -> paintDraw(g, t);
        }
        paintTexts(g, t);
        g.dispose();
    }

    private void paintWin(Graphics2D g, double t) {
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = baseline() - titleSize() * 0.36f;

        // 남색으로 살짝 어둡게 한 뒤 가운데에 금빛이 번진다.
        fade(g, ease(t / 0.35), () -> {
            g.setColor(new Color(0x10, 0x16, 0x3C, 165));
            g.fillRect(0, 0, w, h);
            float pulse = (float) (0.8 + 0.2 * Math.sin(t * 2.4));
            float radius = Math.max(w, h) * 0.55f;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), radius, new float[] {0f, 0.45f, 1f},
                    new Color[] {alpha(GOLD, (int) (120 * pulse)), alpha(GOLD_DEEP, (int) (35 * pulse)), alpha(GOLD, 0)}));
            g.fillRect(0, 0, w, h);
        });

        // 천천히 도는 빛줄기.
        fade(g, ease((t - 0.15) / 0.5) * 0.9f, () -> {
            Graphics2D rays = (Graphics2D) g.create();
            rays.translate(cx, cy);
            rays.rotate(t * 0.25);
            float length = Math.max(w, h);
            int count = 14;
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                Path2D ray = new Path2D.Double();
                ray.moveTo(0, 0);
                ray.lineTo(Math.cos(angle - 0.07) * length, Math.sin(angle - 0.07) * length);
                ray.lineTo(Math.cos(angle + 0.07) * length, Math.sin(angle + 0.07) * length);
                ray.closePath();
                rays.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), length * 0.6f, new float[] {0f, 1f},
                        new Color[] {alpha(GOLD_LIGHT, 70), alpha(GOLD, 0)}));
                rays.fill(ray);
            }
            rays.dispose();
        });

        // 글자 뒤에서 터지는 색종이와 위에서 계속 내리는 색종이.
        paintConfettiBurst(g, t, cx, cy);
        paintConfettiRain(g, t);

        // 튀어나오며 커지는 금빛 글자. 자리를 잡은 뒤에는 천천히 숨 쉬듯 커졌다 작아진다.
        double pop = t < 0.55 ? 0.25 + 0.75 * backOut(t / 0.55) : 1 + 0.035 * Math.sin((t - 0.55) * 3.2);
        Shape text = titleShape("WIN", cx, baseline(), pop);
        paintGlow(g, text, GOLD, 1.0);
        Rectangle2D box = text.getBounds2D();
        g.setPaint(new GradientPaint(0, (float) box.getMinY(), GOLD_LIGHT, 0, (float) box.getMaxY(), GOLD_DEEP));
        g.fill(text);
        g.setStroke(new BasicStroke(Math.max(2f, titleSize() / 45f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x8A5A12));
        g.draw(text);
        paintShine(g, text, t);

        // 글자 둘레에서 반짝이는 별.
        paintSparkles(g, t, box);
    }

    private void paintLose(Graphics2D g, double t) {
        int w = getWidth();
        int h = getHeight();

        // 천천히 어두워지고 가장자리는 더 어둡다.
        float dark = ease(t / 1.2);
        g.setColor(new Color(0x05, 0x07, 0x14, (int) (200 * dark)));
        g.fillRect(0, 0, w, h);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h * 0.45f), Math.max(w, h) * 0.75f,
                new float[] {0f, 0.55f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, (int) (60 * dark)), new Color(0, 0, 0, (int) (190 * dark))},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillRect(0, 0, w, h);

        // 차가운 비.
        paintRain(g, t, dark);

        // 바닥에 깔리는 푸른 안개.
        g.setPaint(new GradientPaint(0, h * 0.7f, new Color(0x5A, 0x66, 0x90, 0), 0, h,
                new Color(0x5A, 0x66, 0x90, (int) (70 * dark))));
        g.fillRect(0, (int) (h * 0.7f), w, h);

        // 한 글자씩 힘없이 떨어져 기울어진 채 멈추고, 그 뒤로는 아주 조금씩 처진다.
        String word = "LOSE";
        double[] tilt = {-0.10, 0.07, -0.05, 0.16};
        double[] sink = {0.00, 0.03, 0.01, 0.07};
        float size = titleSize();
        FontRenderContext frc = g.getFontRenderContext();
        Font font = Theme.font(Font.BOLD, size);
        GlyphVector glyphs = font.createGlyphVector(frc, word);
        double total = glyphs.getLogicalBounds().getWidth() + size * 0.06 * (word.length() - 1);
        double x = (w - total) / 2;
        int base = baseline();
        Rectangle2D whole = null;
        for (int i = 0; i < word.length(); i++) {
            double start = 0.25 + i * 0.18;
            double p = clamp((t - start) / 0.9);
            if (p <= 0) {
                x += glyphs.getGlyphMetrics(i).getAdvance() + size * 0.06;
                continue;
            }
            double drop = (1 - bounceOut(p)) * -size * 0.9;
            double droop = sink[i] * size * ease(p) + Math.sin(t * 0.9 + i) * size * 0.008;
            Shape letter = font.createGlyphVector(frc, String.valueOf(word.charAt(i))).getOutline();
            Rectangle2D lb = letter.getBounds2D();
            AffineTransform at = new AffineTransform();
            at.translate(x, base + drop + droop);
            at.rotate(tilt[i] * ease(p), lb.getCenterX(), lb.getCenterY());
            Shape placed = at.createTransformedShape(letter);
            fade(g, (float) Math.min(1, p * 2.5), () -> {
                paintGlow(g, placed, new Color(0x1A, 0x20, 0x40), 0.8);
                Rectangle2D box = placed.getBounds2D();
                g.setPaint(new GradientPaint(0, (float) box.getMinY(), LOSE_LIGHT, 0, (float) box.getMaxY(), LOSE_DARK));
                g.fill(placed);
                g.setStroke(new BasicStroke(Math.max(2f, size / 50f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(0x1C, 0x21, 0x3A));
                g.draw(placed);
            });
            whole = whole == null ? placed.getBounds2D() : whole.createUnion(placed.getBounds2D());
            x += glyphs.getGlyphMetrics(i).getAdvance() + size * 0.06;
        }

        // 글자 아래로 떨어지는 물방울.
        if (whole != null && t > 1.4) {
            paintDrips(g, t - 1.4, whole);
        }
    }

    private void paintDraw(Graphics2D g, double t) {
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = baseline() - titleSize() * 0.36f;
        fade(g, ease(t / 0.35), () -> {
            g.setColor(new Color(0x10, 0x16, 0x3C, 175));
            g.fillRect(0, 0, w, h);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), Math.max(w, h) * 0.45f,
                    new float[] {0f, 1f}, new Color[] {alpha(SILVER, 70), alpha(SILVER, 0)}));
            g.fillRect(0, 0, w, h);
        });
        double pop = t < 0.5 ? 0.4 + 0.6 * backOut(t / 0.5) : 1 + 0.02 * Math.sin((t - 0.5) * 2.4);
        Shape text = titleShape("DRAW", cx, baseline(), pop);
        paintGlow(g, text, SILVER, 0.7);
        Rectangle2D box = text.getBounds2D();
        g.setPaint(new GradientPaint(0, (float) box.getMinY(), SILVER_LIGHT, 0, (float) box.getMaxY(), SILVER));
        g.fill(text);
        g.setStroke(new BasicStroke(Math.max(2f, titleSize() / 45f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x3A, 0x42, 0x66));
        g.draw(text);
        paintShine(g, text, t);
    }

    /** 결과 설명, 점수, 다시 하기 안내 문구. 효과가 어느 정도 나온 뒤에 나타난다. */
    private void paintTexts(Graphics2D g, double t) {
        float appear = ease((t - (mood == Mood.LOSE ? 1.1 : 0.45)) / 0.4);
        if (appear <= 0) {
            return;
        }
        int y = baseline() + DETAIL_GAP;
        Color soft = mood == Mood.LOSE ? new Color(0xB3B9D1) : new Color(0xE6EAF8);
        fade(g, appear, () -> {
            centered(g, detail, Theme.font(Font.PLAIN, 15f), soft, y);
            centered(g, scores, Theme.font(Font.BOLD, 15f), Color.WHITE, y + LINE_GAP);
            if (!status.isEmpty()) {
                centered(g, status, Theme.font(Font.BOLD, 16f), statusColor, y + LINE_GAP + STATUS_GAP - 4);
            }
        });
    }

    // ---------------------------------------------------------------- 효과 조각

    /** 처음 1.6초 동안 글자 뒤에서 사방으로 터져 떨어지는 색종이. */
    private void paintConfettiBurst(Graphics2D g, double t, float cx, float cy) {
        if (t > 2.2) {
            return;
        }
        Random random = new Random(11);
        float unit = titleSize() / 100f;
        for (int i = 0; i < 70; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = (180 + random.nextDouble() * 420) * unit;
            double life = 1.3 + random.nextDouble() * 0.9;
            double spin = (random.nextDouble() - 0.5) * 14;
            Color color = CONFETTI[random.nextInt(CONFETTI.length)];
            double s = t - 0.12;
            if (s < 0 || s > life) {
                continue;
            }
            double drag = (1 - Math.exp(-2.2 * s)) / 2.2;
            double x = cx + Math.cos(angle) * speed * drag;
            double y = cy + Math.sin(angle) * speed * drag + 160 * unit * s * s;
            float a = (float) clamp(1 - Math.pow(s / life, 3));
            paintConfettiPiece(g, x, y, spin * s + angle, 7 * unit, color, a, s);
        }
    }

    /** 위에서 계속 내리는 색종이. */
    private void paintConfettiRain(Graphics2D g, double t) {
        int w = getWidth();
        int h = getHeight();
        Random random = new Random(23);
        float unit = titleSize() / 100f;
        float appear = ease((t - 0.3) / 0.6);
        for (int i = 0; i < 55; i++) {
            double x0 = random.nextDouble() * w;
            double speed = 60 + random.nextDouble() * 70;
            double offset = random.nextDouble() * (h + 40);
            double sway = 10 + random.nextDouble() * 18;
            double phase = random.nextDouble() * Math.PI * 2;
            Color color = CONFETTI[random.nextInt(CONFETTI.length)];
            double y = (offset + speed * t) % (h + 40) - 20;
            double x = x0 + Math.sin(t * 2 + phase) * sway;
            paintConfettiPiece(g, x, y, t * 3 + phase, 6 * unit, color, appear * 0.9f, t + phase);
        }
    }

    /** 뒤집히며 반짝이는 작은 종이 한 장. flip은 뒤집히는 정도를 정하는 시간 값이다. */
    private static void paintConfettiPiece(Graphics2D g, double x, double y, double angle, double size, Color color,
                                           float alpha, double flip) {
        if (alpha <= 0) {
            return;
        }
        Graphics2D piece = (Graphics2D) g.create();
        piece.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        piece.translate(x, y);
        piece.rotate(angle);
        double squash = Math.abs(Math.cos(flip * 5));
        piece.scale(1, Math.max(0.15, squash));
        piece.setColor(squash > 0.85 ? Theme.mix(color, Color.WHITE, 0.45) : color);
        piece.fill(new Rectangle2D.Double(-size / 2, -size * 0.32, size, size * 0.64));
        piece.dispose();
    }

    /** 글자 둘레에서 커졌다 작아지며 반짝이는 네 갈래 별. */
    private void paintSparkles(Graphics2D g, double t, Rectangle2D box) {
        if (t < 0.35) {
            return;
        }
        Random random = new Random(5);
        double margin = titleSize() * 0.45;
        for (int i = 0; i < 18; i++) {
            double x = box.getMinX() - margin + random.nextDouble() * (box.getWidth() + margin * 2);
            double y = box.getMinY() - margin * 0.6 + random.nextDouble() * (box.getHeight() + margin * 1.2);
            double period = 1.1 + random.nextDouble() * 1.1;
            double phase = random.nextDouble();
            double size = titleSize() * (0.06 + random.nextDouble() * 0.07);
            double wave = Math.sin(((t - 0.35) / period + phase) * Math.PI * 2);
            if (wave <= 0) {
                continue;
            }
            double scale = wave * wave;
            Graphics2D star = (Graphics2D) g.create();
            star.translate(x, y);
            star.rotate(t * 0.8 + i);
            star.scale(scale, scale);
            star.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), (float) size, new float[] {0f, 1f},
                    new Color[] {alpha(Color.WHITE, 230), alpha(GOLD, 0)}));
            star.fill(new Ellipse2D.Double(-size, -size, size * 2, size * 2));
            star.setColor(new Color(255, 250, 225, 240));
            star.fill(fourPointStar(size));
            star.dispose();
        }
    }

    /** 글자 위를 비스듬히 훑고 지나가는 흰 빛. 2.4초마다 한 번 지나간다. */
    private void paintShine(Graphics2D g, Shape text, double t) {
        double cycle = (t - 0.6) % 2.4;
        if (t < 0.6 || cycle > 0.9) {
            return;
        }
        Rectangle2D box = text.getBounds2D();
        double progress = cycle / 0.9;
        double band = box.getHeight() * 0.55;
        double x = box.getMinX() - band * 2 + (box.getWidth() + band * 4) * progress;
        Graphics2D shine = (Graphics2D) g.create();
        shine.clip(text);
        shine.rotate(-0.35, box.getCenterX(), box.getCenterY());
        shine.setPaint(new GradientPaint((float) (x - band / 2), 0, new Color(255, 255, 255, 0),
                (float) x, 0, new Color(255, 255, 255, 200), true));
        shine.fill(new Rectangle2D.Double(x - band / 2, box.getMinY() - box.getHeight(), band, box.getHeight() * 3));
        shine.dispose();
    }

    /** 글자 바깥으로 번지는 빛. 굵기가 다른 반투명 테두리를 여러 번 겹쳐 그린다. */
    private void paintGlow(Graphics2D g, Shape text, Color color, double strength) {
        float size = titleSize();
        for (int i = 5; i >= 1; i--) {
            g.setStroke(new BasicStroke(size * 0.045f * i, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(alpha(color, (int) (26 * strength)));
            g.draw(text);
        }
    }

    /** 비스듬히 떨어지는 빗줄기. */
    private void paintRain(Graphics2D g, double t, float strength) {
        int w = getWidth();
        int h = getHeight();
        Random random = new Random(31);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 130; i++) {
            double x0 = random.nextDouble() * (w + 120);
            double speed = 520 + random.nextDouble() * 320;
            double length = 14 + random.nextDouble() * 18;
            double offset = random.nextDouble() * (h + 60);
            int a = (int) ((45 + random.nextInt(60)) * strength);
            double y = (offset + speed * t) % (h + 60) - 30;
            double x = x0 - (y + 30) * 0.18;
            g.setColor(alpha(RAIN, a));
            g.draw(new java.awt.geom.Line2D.Double(x, y, x - length * 0.18, y + length));
        }
    }

    /** 글자 아래에서 맺혔다가 떨어지는 물방울 몇 개. */
    private void paintDrips(Graphics2D g, double t, Rectangle2D box) {
        Random random = new Random(41);
        float size = titleSize();
        for (int i = 0; i < 5; i++) {
            double x = box.getMinX() + box.getWidth() * (0.12 + 0.19 * i + random.nextDouble() * 0.05);
            double period = 2.0 + random.nextDouble() * 1.2;
            double s = (t + random.nextDouble() * period) % period;
            double top = box.getMaxY() - size * 0.04;
            double r = size * 0.028;
            double y;
            double stretch;
            if (s < 0.9) {
                // 맺히며 길어진다.
                y = top + r * s;
                stretch = 1 + s * 0.8;
            } else {
                double fall = s - 0.9;
                y = top + r + 420 * fall * fall;
                stretch = 1.6;
            }
            float a = (float) clamp(1 - (y - top) / (getHeight() - top));
            Graphics2D drop = (Graphics2D) g.create();
            drop.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            drop.setColor(new Color(0x8E99C2));
            drop.fill(new Ellipse2D.Double(x - r, y - r * stretch, r * 2, r * 2 * stretch));
            drop.dispose();
        }
    }

    // ---------------------------------------------------------------- 도구

    /** 가운데 정렬한 큰 글자의 외곽선. scale은 글자 가운데를 기준으로 키운다. */
    private Shape titleShape(String text, float cx, int baseline, double scale) {
        Font font = Theme.font(Font.BOLD, titleSize());
        GlyphVector glyphs = font.createGlyphVector(new FontRenderContext(null, true, true), text);
        Shape outline = glyphs.getOutline();
        Rectangle2D bounds = outline.getBounds2D();
        AffineTransform at = new AffineTransform();
        double centerY = baseline - bounds.getHeight() / 2;
        at.translate(cx, centerY);
        at.scale(scale, scale);
        at.translate(-bounds.getCenterX(), -bounds.getCenterY());
        return at.createTransformedShape(outline);
    }

    private static Shape fourPointStar(double size) {
        Path2D star = new Path2D.Double();
        double inner = size * 0.18;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI / 4 * i - Math.PI / 2;
            double radius = i % 2 == 0 ? size : inner;
            double x = Math.cos(angle) * radius;
            double y = Math.sin(angle) * radius;
            if (i == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.closePath();
        return star;
    }

    private void centered(Graphics2D g, String text, Font font, Color color, int baseline) {
        if (text == null || text.isEmpty()) {
            return;
        }
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        int x = (getWidth() - metrics.stringWidth(text)) / 2;
        g.setColor(new Color(0, 0, 0, 110));
        g.drawString(text, x + 1, baseline + 1);
        g.setColor(color);
        g.drawString(text, x, baseline);
    }

    /** alpha만큼 투명하게 그린다. */
    private static void fade(Graphics2D g, float alpha, Runnable paint) {
        if (alpha <= 0) {
            return;
        }
        java.awt.Composite before = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
        paint.run();
        g.setComposite(before);
    }

    private static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /** 빨리 시작해 천천히 멈춘다. */
    private static float ease(double p) {
        double x = clamp(p);
        return (float) (1 - Math.pow(1 - x, 3));
    }

    /** 목표를 조금 넘었다가 돌아온다. 튀어나오는 느낌. */
    private static double backOut(double p) {
        double x = clamp(p) - 1;
        double k = 1.9;
        return 1 + (k + 1) * x * x * x + k * x * x;
    }

    /** 떨어져 바닥에 한두 번 작게 튄다. */
    private static double bounceOut(double p) {
        double x = clamp(p);
        if (x < 1 / 2.75) {
            return 7.5625 * x * x;
        } else if (x < 2 / 2.75) {
            x -= 1.5 / 2.75;
            return 7.5625 * x * x + 0.75;
        } else if (x < 2.5 / 2.75) {
            x -= 2.25 / 2.75;
            return 7.5625 * x * x + 0.9375;
        }
        x -= 2.625 / 2.75;
        return 7.5625 * x * x + 0.984375;
    }

    private static long now() {
        return System.nanoTime() / 1_000_000;
    }
}
