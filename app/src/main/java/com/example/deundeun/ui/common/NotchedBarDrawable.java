package com.example.deundeun.ui.common;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 하단탭 배경. 상단 중앙에 SOS 버튼이 앉을 반원 노치를 파낸다.
 *
 * <p>같은 모양의 벡터(Figma 31:39)를 배경으로 쓰면 뷰 크기에 맞춰 가로·세로가 각각
 * 늘어나 노치가 타원이 되고, 그 결과 노치가 SOS 버튼보다 커져 뒤 내용이 비친다.
 * 노치 반지름은 버튼 크기에서 나와야 하므로 그리는 시점에 직접 계산한다.
 */
public class NotchedBarDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float notchRadius;

    /**
     * @param color       바 색
     * @param notchRadius 노치 반지름(px). 시안 기준 SOS 반지름(36) + 여유 4 = 40dp.
     */
    public NotchedBarDrawable(@ColorInt int color, float notchRadius) {
        this.notchRadius = notchRadius;
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        rebuildPath(bounds);
    }

    private void rebuildPath(Rect bounds) {
        path.reset();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.addRect(bounds.left, bounds.top, bounds.right, bounds.bottom, Path.Direction.CW);
        // 상단 모서리 중앙에 원을 겹쳐 EVEN_ODD 로 뚫는다. 위쪽 절반은 바 밖이라 반원만 남는다.
        path.addCircle(bounds.centerX(), bounds.top, notchRadius, Path.Direction.CW);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        // 노치가 뚫려 있으므로 불투명하지 않다.
        return paint.getColor() == Color.TRANSPARENT
                ? PixelFormat.TRANSPARENT
                : PixelFormat.TRANSLUCENT;
    }
}
