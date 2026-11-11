package com.example.deundeun.ui.call;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;

/**
 * 음성 인식 중 파동. 둥근 막대들이 소리 크기에 맞춰 오르내린다.
 *
 * 조용할 때는 점으로 가만히 있고, 말할 때만 소리 크기만큼 오르내린다.
 * {@link #setLevel(float)} 로 0 ~ 1 사이 소리 크기를 넣는다.
 */
public class WaveformView extends View {

    private static final int BAR_COUNT = 7;
    /** 가운데가 높고 양 끝이 낮은 모양. */
    private static final float[] BAR_WEIGHTS = {0.45f, 0.7f, 0.9f, 1f, 0.9f, 0.7f, 0.45f};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private final float barWidth;
    private final float barGap;

    /** 들어온 소리 크기. */
    private float target;
    /** 화면에 그리는 크기. 막대가 튀지 않도록 target 을 천천히 따라간다. */
    private float shown;
    private boolean running;

    public WaveformView(Context context) {
        this(context, null);
    }

    public WaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        barWidth = 5f * density;
        barGap = 5f * density;
        paint.setColor(ContextCompat.getColor(context, R.color.orange_primary));
    }

    public void setLevel(float level) {
        target = Math.max(0f, Math.min(1f, level));
    }

    public void start() {
        target = 0f;
        shown = 0f;
        running = true;
        invalidate();
    }

    public void stop() {
        running = false;
        target = 0f;
        shown = 0f;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        running = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 커질 때는 빠르게, 작아질 때는 천천히
        shown += (target - shown) * (target > shown ? 0.45f : 0.12f);

        float time = SystemClock.uptimeMillis() / 1000f;
        float height = getHeight();
        float minHeight = barWidth;
        float totalWidth = BAR_COUNT * barWidth + (BAR_COUNT - 1) * barGap;
        float left = (getWidth() - totalWidth) / 2f;
        float centerY = height / 2f;

        for (int i = 0; i < BAR_COUNT; i++) {
            // 막대마다 박자를 어긋나게 해서 물결처럼 보이게 한다. 조용하면 점으로 가만히 있는다
            float wave = 0.5f + 0.5f * (float) Math.sin(time * 9f + i * 0.9f);
            float level = running ? shown : 0f;
            float amount = level * BAR_WEIGHTS[i] * (0.55f + 0.45f * wave);
            float barHeight = minHeight + (height - minHeight) * amount;

            float x = left + i * (barWidth + barGap);
            bar.set(x, centerY - barHeight / 2f, x + barWidth, centerY + barHeight / 2f);
            canvas.drawRoundRect(bar, barWidth / 2f, barWidth / 2f, paint);
        }

        if (running) {
            postInvalidateOnAnimation();
        }
    }
}
