package com.example.deundeun.ui.simulation;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;

import java.util.Random;

/**
 * 지진 시뮬레이션 화면 효과.
 *
 * 카메라 화면 가장자리를 어둡게 하고, 흔들리는 동안에는 떨어지는 먼지를 그리며
 * {@link #setShakeTarget(View)} 로 받은 뷰를 흔든다.
 * 금 간 자국은 배경 사진에 있어서 따로 그리지 않는다.
 * 기기에서 애니메이션을 끈 사용자에게는 흔들림과 먼지를 보여주지 않는다.
 */
public class EarthquakeEffectView extends View {

    private static final int DUST_COUNT = 36;
    /** 흔들림 최대 폭. 6dp 는 어지럽다는 의견이 있어 줄였다 */
    private static final float SHAKE_DP = 2.5f;
    /** 흔들림 빠르기 배율. 1 이면 처음 값, 작을수록 느리게 흔들린다 */
    private static final float SHAKE_SPEED = 0.6f;
    /** 흔들림이 켜지고 꺼질 때 세기가 바뀌는 시간 */
    private static final float EASE_SECONDS = 0.3f;

    private final Paint dustPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vignettePaint = new Paint();

    private final float[] dustX = new float[DUST_COUNT];
    private final float[] dustY = new float[DUST_COUNT];
    private final float[] dustSize = new float[DUST_COUNT];
    private final float[] dustSpeed = new float[DUST_COUNT];
    private final Random dustRandom = new Random();

    private final float density;
    private final boolean motionEnabled;

    @Nullable
    private View shakeTarget;
    private boolean shaking;
    /** 0 이면 멈춤, 1 이면 최대 흔들림 */
    private float intensity;
    private long lastFrameNanos;
    private boolean frameScheduled;

    private final Choreographer.FrameCallback frameCallback = this::onFrame;

    public EarthquakeEffectView(Context context) {
        this(context, null);
    }

    public EarthquakeEffectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        motionEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || ValueAnimator.areAnimatorsEnabled();

        dustPaint.setColor(ContextCompat.getColor(context, R.color.quake_dust));

        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** 흔들 뷰. 보통 카메라 프리뷰를 감싼 레이아웃을 넘긴다 */
    public void setShakeTarget(@Nullable View target) {
        if (shakeTarget != null) resetTarget();
        shakeTarget = target;
    }

    /** 흔들림을 켜거나 끈다. 갑자기 멈추지 않고 서서히 잦아든다 */
    public void setShaking(boolean shaking) {
        this.shaking = shaking && motionEnabled;
        scheduleFrame();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        vignettePaint.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.75f,
                new int[]{0, 0, ContextCompat.getColor(getContext(), R.color.quake_vignette)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        for (int i = 0; i < DUST_COUNT; i++) {
            resetDust(i, w, h, true);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        scheduleFrame();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Choreographer.getInstance().removeFrameCallback(frameCallback);
        frameScheduled = false;
        resetTarget();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), vignettePaint);
        if (intensity > 0.01f) {
            dustPaint.setAlpha((int) (200 * intensity));
            for (int i = 0; i < DUST_COUNT; i++) {
                canvas.drawCircle(dustX[i], dustY[i], dustSize[i], dustPaint);
            }
        }
    }

    private void scheduleFrame() {
        if (frameScheduled || !isAttachedToWindow()) return;
        if (!shaking && intensity <= 0.01f) return;
        frameScheduled = true;
        lastFrameNanos = 0;
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    private void onFrame(long frameTimeNanos) {
        frameScheduled = false;
        float dt = lastFrameNanos == 0 ? 0f : (frameTimeNanos - lastFrameNanos) / 1e9f;
        lastFrameNanos = frameTimeNanos;

        float target = shaking ? 1f : 0f;
        intensity += (target - intensity) * Math.min(1f, dt / EASE_SECONDS);

        if (intensity <= 0.01f && !shaking) {
            intensity = 0f;
            resetTarget();
            invalidate();
            return;
        }

        // 서로 다른 주기의 사인을 섞어 불규칙하게 흔든다
        float t = frameTimeNanos / 1e9f * SHAKE_SPEED;
        float amplitude = SHAKE_DP * density * intensity;
        if (shakeTarget != null) {
            shakeTarget.setTranslationX(amplitude
                    * (float) (Math.sin(t * 37) + 0.6 * Math.sin(t * 61 + 1)) / 1.6f);
            shakeTarget.setTranslationY(amplitude
                    * (float) (Math.sin(t * 43 + 2) + 0.6 * Math.sin(t * 71)) / 1.6f);
        }

        int w = getWidth();
        int h = getHeight();
        for (int i = 0; i < DUST_COUNT; i++) {
            dustY[i] += dustSpeed[i] * dt * intensity;
            dustX[i] += (float) Math.sin(t * 5 + i) * 0.4f * density;
            if (dustY[i] > h + dustSize[i]) resetDust(i, w, h, false);
        }

        invalidate();
        frameScheduled = true;
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    private void resetDust(int i, int w, int h, boolean anywhere) {
        dustX[i] = dustRandom.nextFloat() * w;
        dustY[i] = anywhere ? dustRandom.nextFloat() * h : -dustRandom.nextFloat() * h * 0.2f;
        dustSize[i] = (1f + dustRandom.nextFloat() * 2.5f) * density;
        dustSpeed[i] = (60f + dustRandom.nextFloat() * 140f) * density;
    }

    private void resetTarget() {
        if (shakeTarget != null) {
            shakeTarget.setTranslationX(0f);
            shakeTarget.setTranslationY(0f);
        }
    }
}
