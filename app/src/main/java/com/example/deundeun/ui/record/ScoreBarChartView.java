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
 * 훈련 종류 하나의 회차별 점수 막대 차트.
 *
 * 연습 한 번이 막대 하나이고 막대 위에 점수를 적는다. 가장 최근 막대만 진하게 칠해
 * "지금"이 어디인지 바로 보이게 한다. 0점과 100점이 섞여도 선처럼 지그재그가 생기지 않는다.
 */
public class ScoreBarChartView extends View {

    private static final int[] Y_TICKS = {0, 50, 100};
    private static final int Y_MAX = 100;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private final float maxBarWidth;
    private final float barRadius;
    private final float axisGap;
    private final float valueGap;
    private final int latestColor;
    private final int pastColor;
    private final int labelColor;
    private final int latestLabelColor;

    @Nullable
    private String descriptionTitle;
    private String[] xLabels = new String[0];
    private int[] scores = new int[0];

    public ScoreBarChartView(Context context) {
        this(context, null);
    }

    public ScoreBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        float scaled = getResources().getDisplayMetrics().scaledDensity;

        maxBarWidth = 28 * density;
        barRadius = 4 * density;
        axisGap = 10 * density;
        valueGap = 4 * density;

        latestColor = ContextCompat.getColor(context, R.color.orange_primary);
        pastColor = ContextCompat.getColor(context, R.color.orange_pale);
        labelColor = ContextCompat.getColor(context, R.color.text_muted);
        latestLabelColor = ContextCompat.getColor(context, R.color.orange_primary);

        gridPaint.setColor(ContextCompat.getColor(context, R.color.divider));
        gridPaint.setStrokeWidth(1 * density);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{4 * density, 4 * density}, 0));

        labelPaint.setColor(labelColor);
        labelPaint.setTextSize(11 * scaled);

        valuePaint.setColor(ContextCompat.getColor(context, R.color.text_pill));
        valuePaint.setTextSize(12 * scaled);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
    }

    /** 화면 낭독기가 그래프 앞에 읽을 이름 */
    public void setDescriptionTitle(String title) {
        this.descriptionTitle = title;
        setContentDescription(buildDescription());
    }

    /**
     * @param xLabels 회차 라벨 ("1회" … "최근")
     * @param scores  점수, 오래된 것부터
     */
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
        StringBuilder builder = new StringBuilder(descriptionTitle != null
                ? descriptionTitle : getContext().getString(R.string.record_tab_score));
        for (int i = 0; i < xLabels.length; i++) {
            builder.append(", ").append(xLabels[i]).append(' ').append(scores[i]).append("점");
        }
        return builder.toString();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (scores.length == 0) {
            return;
        }

        float labelHeight = labelPaint.getTextSize() + axisGap;
        float yLabelWidth = labelPaint.measureText(String.valueOf(Y_MAX)) + axisGap;
        float plotLeft = getPaddingLeft() + yLabelWidth;
        float plotRight = getWidth() - getPaddingRight();
        // 100점 막대 위에도 점수가 들어가도록 위쪽을 남긴다
        float plotTop = getPaddingTop() + valuePaint.getTextSize() + valueGap;
        // 받침(예: "최근"의 ㄴ)이 아래 경계에 잘리지 않도록 글자 아래 여백도 뺀다
        float plotBottom = getHeight() - getPaddingBottom() - labelHeight - labelPaint.descent();
        float plotHeight = plotBottom - plotTop;

        for (int tick : Y_TICKS) {
            float y = plotBottom - plotHeight * tick / Y_MAX;
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint);
            String text = String.valueOf(tick);
            labelPaint.setColor(labelColor);
            labelPaint.setFakeBoldText(false);
            canvas.drawText(text, plotLeft - axisGap - labelPaint.measureText(text),
                    y + labelPaint.getTextSize() / 3f, labelPaint);
        }

        float slot = (plotRight - plotLeft) / scores.length;
        float barWidth = Math.min(maxBarWidth, slot * 0.5f);
        int latest = scores.length - 1;
        for (int i = 0; i < scores.length; i++) {
            float center = plotLeft + slot * (i + 0.5f);
            int score = Math.max(0, Math.min(Y_MAX, scores[i]));
            float top = plotBottom - plotHeight * score / Y_MAX;

            if (score > 0) {
                barPaint.setColor(i == latest ? latestColor : pastColor);
                barRect.set(center - barWidth / 2f, top, center + barWidth / 2f, plotBottom);
                canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint);
            }
            canvas.drawText(String.valueOf(scores[i]), center, top - valueGap, valuePaint);

            String label = xLabels[i];
            labelPaint.setColor(i == latest ? latestLabelColor : labelColor);
            labelPaint.setFakeBoldText(i == latest);
            canvas.drawText(label, center - labelPaint.measureText(label) / 2f,
                    plotBottom + labelHeight, labelPaint);
        }
    }
}
