package com.example.deundeun.ui.common;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * targetSdk 35+ 에서는 시스템이 edge-to-edge 를 강제하므로 화면이 상태바·내비게이션바
 * 뒤까지 그려진다. 인셋을 패딩으로 바꿔주는 헬퍼.
 */
public final class InsetsUtil {

    private InsetsUtil() {
    }

    /** 시스템 바 상단 인셋만큼 위쪽 패딩을 더한다. 원래 패딩은 유지된다. */
    public static void applyTopInset(View view) {
        final int basePadding = view.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), basePadding + bars.top,
                    v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });
    }

    /**
     * 위아래 인셋을 한 번에 더한다.
     *
     * <p>한 뷰에 {@link #applyTopInset}과 {@link #applyBottomInset}을 잇달아 부르면 안 된다.
     * 뷰당 인셋 리스너는 하나뿐이라 나중 것이 앞의 것을 덮어써서 위쪽 인셋이 사라진다.
     * 한 뷰에 둘 다 필요하면 이 메서드를 쓴다.
     */
    public static void applyVerticalInsets(View view) {
        final int baseTop = view.getPaddingTop();
        final int baseBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), baseTop + bars.top,
                    v.getPaddingRight(), baseBottom + bars.bottom);
            return windowInsets;
        });
    }

    /** 시스템 바 하단 인셋만큼 아래쪽 패딩을 더한다. 스크롤 뷰에는 clipToPadding=false 를 함께 쓴다. */
    public static void applyBottomInset(View view) {
        final int basePadding = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), basePadding + bars.bottom);
            return windowInsets;
        });
    }

    /**
     * 하단탭 전용. 내비게이션바 인셋만큼 <b>높이를 늘리고</b> 같은 크기의 아래 패딩을 준다.
     * 패딩만 주면 배경 벡터의 노치가 위로 밀려 SOS 버튼과 어긋난다.
     */
    public static void applyBottomBarInset(View bar) {
        final int baseHeight = bar.getLayoutParams().height;
        final int basePadding = bar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bar, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.getLayoutParams().height = baseHeight + bars.bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), basePadding + bars.bottom);
            v.requestLayout();
            return windowInsets;
        });
    }
}
