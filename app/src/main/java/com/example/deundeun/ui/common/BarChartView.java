package com.example.deundeun.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;

/**
 * 홈과 기록탭이 함께 쓰는 막대 그래프.
 *
 * 칸마다 막대 하나를 그리고 막대 위에 값을 적는다. 강조한 칸은 막대를 진한 주황,
 * 아래 라벨을 주황 굵은 글씨로, 나머지는 연한 주황 막대와 회색 라벨로 그린다.
 * 값이 0 인 칸은 막대를 그리지 않는다.
 *
 * 세로 눈금은 0 · 절반 · 최대 세 줄만 둔다. 최대는 {@link #setFixedMax} 로 고정하거나,
 * 고정하지 않으면 가장 큰 값이 들어가는 깔끔한 수(10, 20, 30, 60 …)로 정한다.
 */
public class BarChartView extends View {

    /** 최대를 고정하지 않았을 때 고르는 눈금 최대. 절반도 정수가 되도록 짝수만 둔다 */
    private static final int[] NICE_MAX = {10, 20, 30, 40, 60, 80, 100, 120, 180, 240, 300};

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private final float maxBarWidth;
    private final float barRadius;
    private final float axisGap;
    private final float valueGap;
    private final int strongColor;
    private final int weakColor;
    private final int labelColor;
    private final int strongLabelColor;

    private String[] xLabels = new String[0];
    private int[] values = new int[0];
    private boolean[] highlighted = new boolean[0];
    /** 0 이면 값에 맞춰 정한다 */
    private int fixedMax;
    /** 막대 위 값 뒤에 붙일 단위. 예: "분" */
    private String valueSuffix = "";
    @Nullable
    private String descriptionTitle;
    private String descriptionUnit = "";

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        float scaled = getResources().getDisplayMetrics().scaledDensity;

        maxBarWidth = 28 * density;
        barRadius = 4 * density;
        axisGap = 10 * density;
        valueGap = 4 * density;

        strongColor = ContextCompat.getColor(context, R.color.orange_primary);
        weakColor = ContextCompat.getColor(context, R.color.orange_pale);
        labelColor = ContextCompat.getColor(context, R.color.text_muted);
        strongLabelColor = ContextCompat.getColor(context, R.color.orange_primary);

        gridPaint.setColor(ContextCompat.getColor(context, R.color.divider));
        gridPaint.setStrokeWidth(1 * density);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{4 * density, 4 * density}, 0));

        labelPaint.setTextSize(11 * scaled);

        valuePaint.setColor(ContextCompat.getColor(context, R.color.text_pill));
        valuePaint.setTextSize(12 * scaled);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
    }

    /** 세로 눈금 최대를 고정한다. 점수처럼 범위가 정해진 값에 쓴다 (예: 100) */
    public void setFixedMax(int max) {
        this.fixedMax = max;
        invalidate();
    }

    /** 막대 위 값 뒤에 붙일 단위. 예: "분" */
    public void setValueSuffix(@NonNull String suffix) {
        this.valueSuffix = suffix;
        invalidate();
    }

    /**
     * 화면 낭독기가 읽을 이름과 값의 단위
     *
     * @param unit 예: "점", "분"
     */
    public void setDescription(String title, String unit) {
        this.descriptionTitle = title;
        this.descriptionUnit = unit;
        setContentDescription(buildDescription());
    }

    /**
     * @param highlighted 칸마다 강조 여부. 강조한 칸은 진한 주황 막대와 주황 라벨
     */
    public void setData(String[] xLabels, int[] values, boolean[] highlighted) {
        if (xLabels.length != values.length || xLabels.length != highlighted.length) {
            throw new IllegalArgumentException("라벨, 값, 강조 여부의 길이가 같아야 합니다.");
        }
        this.xLabels = xLabels;
        this.values = values;
        this.highlighted = highlighted;
        invalidate();
        setContentDescription(buildDescription());
    }

    private String buildDescription() {
        StringBuilder builder = new StringBuilder(descriptionTitle != null ? descriptionTitle : "");
        for (int i = 0; i < xLabels.length; i++) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(xLabels[i]).append(' ').append(values[i]).append(descriptionUnit);
        }
        return builder.toString();
    }

    private int yMax() {
        if (fixedMax > 0) {
            return fixedMax;
        }
        int max = 0;
        for (int value : values) {
            max = Math.max(max, value);
        }
        for (int nice : NICE_MAX) {
            if (max <= nice) {
                return nice;
            }
        }
        // 아주 크면 60 단위로 올린다
        return ((max + 59) / 60) * 60;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.length == 0) {
            return;
        }

        int yMax = yMax();
        int[] ticks = {0, yMax / 2, yMax};

        float labelHeight = labelPaint.getTextSize() + axisGap;
        float yLabelWidth = labelPaint.measureText(String.valueOf(yMax)) + axisGap;
        float plotLeft = getPaddingLeft() + yLabelWidth;
        float plotRight = getWidth() - getPaddingRight();
        // 가장 높은 막대 위에도 값이 들어가도록 위쪽을 남긴다
        float plotTop = getPaddingTop() + valuePaint.getTextSize() + valueGap;
        // 받침(예: "최근"의 ㄴ)이 아래 경계에 잘리지 않도록 글자 아래 여백도 뺀다
        float plotBottom = getHeight() - getPaddingBottom() - labelHeight - labelPaint.descent();
        float plotHeight = plotBottom - plotTop;

        labelPaint.setColor(labelColor);
        labelPaint.setFakeBoldText(false);
        for (int tick : ticks) {
            float y = plotBottom - plotHeight * tick / yMax;
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint);
            String text = String.valueOf(tick);
            canvas.drawText(text, plotLeft - axisGap - labelPaint.measureText(text),
                    y + labelPaint.getTextSize() / 3f, labelPaint);
        }

        float slot = (plotRight - plotLeft) / values.length;
        float barWidth = Math.min(maxBarWidth, slot * 0.5f);
        for (int i = 0; i < values.length; i++) {
            float center = plotLeft + slot * (i + 0.5f);
            int value = Math.max(0, Math.min(yMax, values[i]));
            float top = plotBottom - plotHeight * value / yMax;

            if (value > 0) {
                barPaint.setColor(highlighted[i] ? strongColor : weakColor);
                barRect.set(center - barWidth / 2f, top, center + barWidth / 2f, plotBottom);
                canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint);
                canvas.drawText(values[i] + valueSuffix, center, top - valueGap, valuePaint);
            } else if (valueSuffix.isEmpty()) {
                // 점수처럼 0 도 의미가 있는 값은 막대 없이 0 만 적는다
                canvas.drawText("0", center, top - valueGap, valuePaint);
            }

            String label = xLabels[i];
            labelPaint.setColor(highlighted[i] ? strongLabelColor : labelColor);
            labelPaint.setFakeBoldText(highlighted[i]);
            canvas.drawText(label, center - labelPaint.measureText(label) / 2f,
                    plotBottom + labelHeight, labelPaint);
        }
    }
}
