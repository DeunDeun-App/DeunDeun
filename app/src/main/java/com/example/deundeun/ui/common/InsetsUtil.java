package com.example.deundeun.ui.common;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class InsetsUtil {

    private InsetsUtil() {
    }

    public static void applyTopInset(View view) {
        final int basePadding = view.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), basePadding + bars.top,
                    v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });
    }

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

    public static void applyBottomInset(View view) {
        final int basePadding = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), basePadding + bars.bottom);
            return windowInsets;
        });
    }

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
