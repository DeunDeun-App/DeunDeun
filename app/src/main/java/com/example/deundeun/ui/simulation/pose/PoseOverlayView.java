package com.example.deundeun.ui.simulation.pose;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.Collections;
import java.util.List;

/**
 * 카메라 프리뷰 위에 인식된 관절을 그린다.
 * PreviewView 기본값(FILL_CENTER)과 같은 방식으로 늘려서 프리뷰와 겹치게 했다.
 * 손 모델 결과가 있으면(화재) 손은 손 모델로 그리고, 포즈 모델의 대략적인 손 점은 뺀다.
 */
public class PoseOverlayView extends View {

    /** 포즈 뼈대. 얼굴 세부 점은 빼고 상체·손·하체만 */
    private static final int[][] POSE_CONNECTIONS = {
            {11, 12}, {11, 13}, {13, 15}, {12, 14}, {14, 16},
            // 손: 손목에서 새끼(17/18)·검지(19/20)·엄지(21/22)로
            {15, 17}, {15, 19}, {17, 19}, {15, 21},
            {16, 18}, {16, 20}, {18, 20}, {16, 22},
            {11, 23}, {12, 24}, {23, 24},
            {23, 25}, {25, 27}, {24, 26}, {26, 28}
    };

    /** 손 모델 21개 점의 뼈대 */
    private static final int[][] HAND_CONNECTIONS = {
            {0, 1}, {1, 2}, {2, 3}, {3, 4},
            {0, 5}, {5, 6}, {6, 7}, {7, 8},
            {9, 10}, {10, 11}, {11, 12},
            {13, 14}, {14, 15}, {15, 16},
            {0, 17}, {17, 18}, {18, 19}, {19, 20},
            {5, 9}, {9, 13}, {13, 17}
    };

    private static final int FIRST_POSE_HAND_POINT = 17;
    private static final int LAST_POSE_HAND_POINT = 22;
    private static final int WRIST_FIRST = 15;
    /** 이보다 덜 보이는 점은 MediaPipe 가 짐작한 위치라 그리지 않는다 */
    private static final float MIN_VISIBILITY = 0.5f;
    /** 얼굴 앞의 손은 가시성이 낮게 나와서 기준을 낮춘다 */
    private static final float MIN_HAND_VISIBILITY = 0.1f;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<NormalizedLandmark> pose = Collections.emptyList();
    private List<List<NormalizedLandmark>> hands = null;
    private int imageWidth = 1;
    private int imageHeight = 1;

    private float scale;
    private float offsetX;
    private float offsetY;

    public PoseOverlayView(Context context) {
        this(context, null);
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4 * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        pointPaint.setColor(ContextCompat.getColor(context, R.color.white));
        setAccent(ContextCompat.getColor(context, R.color.orange_primary));

        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setAccent(@ColorInt int color) {
        linePaint.setColor(color);
        invalidate();
    }

    /**
     * @param hands 손 모델 결과. 손 모델을 쓰지 않으면 null (포즈 모델의 손 점을 그린다)
     */
    public void setLandmarks(@Nullable List<NormalizedLandmark> pose,
                             @Nullable List<List<NormalizedLandmark>> hands,
                             int imageWidth, int imageHeight) {
        this.pose = pose != null ? pose : Collections.emptyList();
        this.hands = hands;
        this.imageWidth = Math.max(1, imageWidth);
        this.imageHeight = Math.max(1, imageHeight);
        invalidate();
    }

    public void clear() {
        setLandmarks(null, null, imageWidth, imageHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        scale = Math.max((float) getWidth() / imageWidth, (float) getHeight() / imageHeight);
        offsetX = (getWidth() - imageWidth * scale) / 2f;
        offsetY = (getHeight() - imageHeight * scale) / 2f;
        float radius = linePaint.getStrokeWidth();

        if (pose.size() >= 33) {
            for (int[] c : POSE_CONNECTIONS) {
                if (!poseShown(c[0]) || !poseShown(c[1])) continue;
                drawLine(canvas, pose.get(c[0]), pose.get(c[1]));
            }
            for (int i = 11; i <= 28; i++) {
                if (!poseShown(i)) continue;
                drawPoint(canvas, pose.get(i), radius);
            }
        }

        if (hands != null) {
            for (List<NormalizedLandmark> hand : hands) {
                if (hand.size() < 21) continue;
                for (int[] c : HAND_CONNECTIONS) {
                    drawLine(canvas, hand.get(c[0]), hand.get(c[1]));
                }
                for (NormalizedLandmark p : hand) {
                    drawPoint(canvas, p, radius * 0.6f);
                }
            }
        }
    }

    private boolean poseShown(int index) {
        boolean handPoint = index >= FIRST_POSE_HAND_POINT && index <= LAST_POSE_HAND_POINT;
        // 손 모델이 손을 그리면 포즈 모델의 대략적인 손 점은 겹치지 않게 뺀다
        if (handPoint && hands != null) return false;
        float min = index >= WRIST_FIRST && index <= LAST_POSE_HAND_POINT
                ? MIN_HAND_VISIBILITY : MIN_VISIBILITY;
        return pose.get(index).visibility().orElse(0f) >= min;
    }

    private void drawLine(Canvas canvas, NormalizedLandmark a, NormalizedLandmark b) {
        canvas.drawLine(toX(a), toY(a), toX(b), toY(b), linePaint);
    }

    private void drawPoint(Canvas canvas, NormalizedLandmark p, float radius) {
        canvas.drawCircle(toX(p), toY(p), radius, pointPaint);
    }

    private float toX(NormalizedLandmark p) {
        return offsetX + p.x() * imageWidth * scale;
    }

    private float toY(NormalizedLandmark p) {
        return offsetY + p.y() * imageHeight * scale;
    }
}
