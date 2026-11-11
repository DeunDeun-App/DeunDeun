package com.example.deundeun.ui.simulation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;

/**
 * 지진 시뮬레이션의 가상 배경.
 *
 * 실제 방 대신 지진이 난 사무실 사진({@code R.drawable.bg_quake_room})을 깔고,
 * 카메라 프레임에서 오려낸 사람을 그 위에 그린 뒤, 사진 속 책상 부분만 한 번 더 그린다.
 * 그러면 사람이 책상 상판과 다리 뒤, 즉 책상 밑에 들어간 것처럼 보인다.
 *
 * 사람은 PreviewView 기본값(FILL_CENTER)과 같은 방식으로 늘려서 관절 오버레이와 겹친다.
 * 사람 프레임은 자세 인식이 끝날 때마다 바뀌므로 카메라 영상보다 끊겨 보일 수 있다.
 */
public class VirtualBackgroundView extends View {

    /**
     * 사진 속 책상 영역. 사진 너비·높이에 대한 비율 {왼쪽, 위, 오른쪽, 아래}.
     * bg_quake_room(941x1672) 기준으로 잡았으니 사진을 바꾸면 이 값도 다시 맞춘다.
     */
    private static final float[][] DESK_REGIONS = {
            // 상판과 그 밑 철제 가로대
            {0f, 830f / 1672, 1f, 972f / 1672},
            // 왼쪽 다리
            {0f, 885f / 1672, 75f / 941, 1425f / 1672},
            // 오른쪽 다리
            {868f / 941, 885f / 1672, 1f, 1425f / 1672},
    };

    private final Paint personPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF personRect = new RectF();
    private final Path deskPath = new Path();
    @Nullable
    private final Drawable background;

    @Nullable
    private Bitmap person;

    public VirtualBackgroundView(Context context) {
        this(context, null);
    }

    public VirtualBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        background = ContextCompat.getDrawable(context, R.drawable.bg_quake_room);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** 사람만 남기고 투명하게 만든 프레임. null 이면 배경만 그린다 */
    public void setPerson(@Nullable Bitmap person) {
        this.person = person;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (background == null) return;
        // 사진 비율을 지키면서 화면을 꽉 채운다. 세로로 남는 부분은 위쪽을 잘라
        // 사진 아래 끝을 화면 아래에 맞춘다. 가운데 맞춤보다 책상이 화면에서 더 높이 온다
        int iw = Math.max(1, background.getIntrinsicWidth());
        int ih = Math.max(1, background.getIntrinsicHeight());
        float scale = Math.max((float) w / iw, (float) h / ih);
        int dw = Math.round(iw * scale);
        int dh = Math.round(ih * scale);
        int left = (w - dw) / 2;
        int top = h - dh;
        background.setBounds(left, top, left + dw, top + dh);

        // 책상 영역도 사진과 같은 비율로 화면 좌표에 옮긴다
        Rect b = background.getBounds();
        deskPath.reset();
        for (float[] r : DESK_REGIONS) {
            deskPath.addRect(b.left + r[0] * b.width(), b.top + r[1] * b.height(),
                    b.left + r[2] * b.width(), b.top + r[3] * b.height(), Path.Direction.CW);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (background != null) {
            background.draw(canvas);
        }
        if (person == null) return;

        float scale = Math.max((float) getWidth() / person.getWidth(),
                (float) getHeight() / person.getHeight());
        float dw = person.getWidth() * scale;
        float dh = person.getHeight() * scale;
        float left = (getWidth() - dw) / 2f;
        float top = (getHeight() - dh) / 2f;
        personRect.set(left, top, left + dw, top + dh);
        canvas.drawBitmap(person, null, personRect, personPaint);

        // 사진 속 책상을 사람 위에 다시 그려 책상이 사람 앞에 오게 한다
        if (background != null) {
            canvas.save();
            canvas.clipPath(deskPath);
            background.draw(canvas);
            canvas.restore();
        }
    }
}
