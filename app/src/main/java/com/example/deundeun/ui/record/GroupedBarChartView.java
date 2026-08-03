package com.example.deundeun.ui.record;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;

/**
 * Figma 67:961 — 훈련 이력의 주차별 횟수 막대 차트.
 *
 * <p>가로축은 주 시작일, 세로축은 훈련 횟수. 계열 2개(시뮬레이션 / 119 연습)를
 * 한 주차 안에 나란히 그린다. 값이 0인 계열은 막대를 그리지 않는다.
 */
public class GroupedBarChartView extends View {

    private static final int Y_TICK_COUNT = 5;   // 0, 2, 4, 6, 8
    private static final int Y_STEP = 2;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private String[] xLabels = new String[0];
    private int[] primary = new int[0];
    private int[] secondary = new int[0];

    private float barWidth;
    private float barRadius;
    private float axisGap;

    public GroupedBarChartView(Context context) {
        this(context, null);
    }

    public GroupedBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;

        barWidth = 20 * density;
        barRadius = 3 * density;
        axisGap = 10 * density;

        // 세로축 맨 위 눈금 라벨이 잘리지 않도록 위쪽을 비워 둔다.
        if (getPaddingTop() == 0) {
            setPadding(getPaddingLeft(), (int) (8 * density), getPaddingRight(), getPaddingBottom());
        }

        gridPaint.setColor(ContextCompat.getColor(context, R.color.divider));
        gridPaint.setStrokeWidth(1 * density);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{4 * density, 4 * density}, 0));

        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_muted));
        labelPaint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
    }

    /**
     * @param xLabels   주차 라벨
     * @param primary   시뮬레이션 횟수
     * @param secondary 119 연습 횟수 (없으면 0으로 채운 배열)
     */
    public void setData(String[] xLabels, int[] primary, int[] secondary) {
        if (xLabels.length != primary.length || xLabels.length != secondary.length) {
            throw new IllegalArgumentException("라벨과 두 계열의 길이가 같아야 합니다.");
        }
        this.xLabels = xLabels;
        this.primary = primary;
        this.secondary = secondary;
        invalidate();
        setContentDescription(buildDescription());
    }

    private String buildDescription() {
        StringBuilder builder = new StringBuilder(getContext().getString(R.string.record_tab_history));
        for (int i = 0; i < xLabels.length; i++) {
            builder.append(", ").append(xLabels[i]).append(' ')
                    .append(primary[i] + secondary[i]).append("회");
        }
        return builder.toString();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (xLabels.length == 0) {
            return;
        }

        float labelHeight = labelPaint.getTextSize() + axisGap;
        float yLabelWidth = labelPaint.measureText("8") + axisGap;
        float plotLeft = getPaddingLeft() + yLabelWidth;
        float plotRight = getWidth() - getPaddingRight();
        float plotTop = getPaddingTop();
        float plotBottom = getHeight() - getPaddingBottom() - labelHeight;
        float plotHeight = plotBottom - plotTop;

        int maxValue = (Y_TICK_COUNT - 1) * Y_STEP;

        // 가로 격자 + 세로축 라벨
        for (int tick = 0; tick < Y_TICK_COUNT; tick++) {
            int value = tick * Y_STEP;
            float y = plotBottom - plotHeight * value / maxValue;
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint);

            String text = String.valueOf(value);
            canvas.drawText(text,
                    plotLeft - axisGap - labelPaint.measureText(text),
                    y + labelPaint.getTextSize() / 3f,
                    labelPaint);
        }

        // 주차별 막대
        float slot = (plotRight - plotLeft) / xLabels.length;
        for (int i = 0; i < xLabels.length; i++) {
            float center = plotLeft + slot * (i + 0.5f);
            boolean grouped = secondary[i] > 0;

            if (grouped) {
                drawBar(canvas, center - barWidth * 0.55f, plotBottom, plotHeight, maxValue,
                        primary[i], R.color.orange_primary);
                drawBar(canvas, center + barWidth * 0.55f, plotBottom, plotHeight, maxValue,
                        secondary[i], R.color.orange_pale);
            } else {
                drawBar(canvas, center, plotBottom, plotHeight, maxValue,
                        primary[i], R.color.orange_primary);
            }

            String label = xLabels[i];
            canvas.drawText(label, center - labelPaint.measureText(label) / 2f,
                    plotBottom + labelHeight, labelPaint);
        }
    }

    private void drawBar(Canvas canvas, float centerX, float baseline, float plotHeight,
                         int maxValue, int value, int colorRes) {
        if (value <= 0) {
            return;
        }
        barPaint.setColor(ContextCompat.getColor(getContext(), colorRes));
        float height = plotHeight * value / maxValue;
        barRect.set(centerX - barWidth / 2f, baseline - height, centerX + barWidth / 2f, baseline);
        canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint);
    }
}
