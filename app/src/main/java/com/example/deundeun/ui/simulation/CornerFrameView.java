package com.example.deundeun.ui.simulation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;

/**
 * Figma 74:42 — 카메라 프리뷰 위에 얹는 네 모서리 프레임.
 *
 * <p>사용자가 어디를 비춰야 하는지 알려주는 안내선이라 뷰 크기에 맞춰 그린다.
 * 이미지로 넣으면 화면 비율이 달라질 때 모서리가 늘어난다.
 */
public class CornerFrameView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private final float armLength;

    public CornerFrameView(Context context) {
        this(context, null);
    }

    public CornerFrameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;

        armLength = 28 * density;

        paint.setColor(ContextCompat.getColor(context, R.color.white));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3 * density);
        paint.setStrokeCap(Paint.Cap.SQUARE);

        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = getPaddingLeft();
        float top = getPaddingTop();
        float right = getWidth() - getPaddingRight();
        float bottom = getHeight() - getPaddingBottom();

        path.reset();

        // 좌상
        path.moveTo(left, top + armLength);
        path.lineTo(left, top);
        path.lineTo(left + armLength, top);
        // 우상
        path.moveTo(right - armLength, top);
        path.lineTo(right, top);
        path.lineTo(right, top + armLength);
        // 우하
        path.moveTo(right, bottom - armLength);
        path.lineTo(right, bottom);
        path.lineTo(right - armLength, bottom);
        // 좌하
        path.moveTo(left + armLength, bottom);
        path.lineTo(left, bottom);
        path.lineTo(left, bottom - armLength);

        canvas.drawPath(path, paint);
    }
}
