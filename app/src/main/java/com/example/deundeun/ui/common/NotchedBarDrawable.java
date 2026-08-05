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
 * 하단탭 배경
 */
public class NotchedBarDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float notchRadius;

    /**
     * @param color       바 색
     * @param notchRadius
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
        return paint.getColor() == Color.TRANSPARENT
                ? PixelFormat.TRANSPARENT
                : PixelFormat.TRANSLUCENT;
    }
}
