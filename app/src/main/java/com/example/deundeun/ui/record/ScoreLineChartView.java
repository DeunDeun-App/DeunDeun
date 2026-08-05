package com.example.deundeun.ui.record;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;

/**
 * 119 점수 변화 꺾은선 차트.
 */
public class ScoreLineChartView extends View {

    private static final int[] Y_TICKS = {50, 60, 80, 100};
    private static final int Y_MIN = 50;
    private static final int Y_MAX = 100;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private String[] xLabels = new String[0];
    private int[] scores = new int[0];

    private final float axisGap;

    public ScoreLineChartView(Context context) {
        this(context, null);
    }

    public ScoreLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        axisGap = 10 * density;

        if (getPaddingTop() == 0) {
            setPadding(getPaddingLeft(), (int) (8 * density), getPaddingRight(), getPaddingBottom());
        }

        linePaint.setColor(ContextCompat.getColor(context, R.color.orange_primary));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setColor(ContextCompat.getColor(context, R.color.orange_fill));
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(ContextCompat.getColor(context, R.color.divider));
        gridPaint.setStrokeWidth(1 * density);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{4 * density, 4 * density}, 0));

        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_muted));
        labelPaint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
    }

    public void setData(String[] xLabels, int[] scores) {
        if (xLabels.length != scores.length) {
            throw new IllegalArgumentException("라벨과 점수의 길이가 같아야 합니다.");
        }
        this.xLabels = xLabels;
        this.scores = scores;
        invalidate();
        setContentDescription(buildDescription());
    }

    private String buildDescription() {
        StringBuilder builder = new StringBuilder(getContext().getString(R.string.record_tab_score));
        for (int i = 0; i < xLabels.length; i++) {
            builder.append(", ").append(xLabels[i]).append(' ').append(scores[i]).append("점");
        }
        return builder.toString();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (scores.length < 2) {
            return;
        }

        float labelHeight = labelPaint.getTextSize() + axisGap;
        float yLabelWidth = labelPaint.measureText("100") + axisGap;
        float plotLeft = getPaddingLeft() + yLabelWidth;
        float plotRight = getWidth() - getPaddingRight();
        float plotTop = getPaddingTop();
        float plotBottom = getHeight() - getPaddingBottom() - labelHeight;

        for (int tick : Y_TICKS) {
            float y = yFor(tick, plotTop, plotBottom);
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint);
            String text = String.valueOf(tick);
            canvas.drawText(text,
                    plotLeft - axisGap - labelPaint.measureText(text),
                    y + labelPaint.getTextSize() / 3f,
                    labelPaint);
        }

        float step = (plotRight - plotLeft) / (scores.length - 1);
        float[] xs = new float[scores.length];
        float[] ys = new float[scores.length];
        for (int i = 0; i < scores.length; i++) {
            xs[i] = plotLeft + step * i;
            ys[i] = yFor(scores[i], plotTop, plotBottom);
        }

        buildSmoothPath(xs, ys);

        fillPath.set(linePath);
        fillPath.lineTo(xs[xs.length - 1], plotBottom);
        fillPath.lineTo(xs[0], plotBottom);
        fillPath.close();
        canvas.drawPath(fillPath, fillPaint);

        canvas.drawPath(linePath, linePaint);

        for (int i = 0; i < xLabels.length; i++) {
            String label = xLabels[i];
            float textWidth = labelPaint.measureText(label);
            float labelX = Math.max(getPaddingLeft(),
                    Math.min(xs[i] - textWidth / 2f, getWidth() - getPaddingRight() - textWidth));
            canvas.drawText(label, labelX, plotBottom + labelHeight, labelPaint);
        }
    }

    private float yFor(int score, float plotTop, float plotBottom) {
        float ratio = (float) (score - Y_MIN) / (Y_MAX - Y_MIN);
        return plotBottom - (plotBottom - plotTop) * ratio;
    }

    private void buildSmoothPath(float[] xs, float[] ys) {
        linePath.reset();
        linePath.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2f;
            linePath.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i]);
        }
    }
}
