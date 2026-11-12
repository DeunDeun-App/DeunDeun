package com.example.deundeun.ui.common;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class KeyboardUtil {

    private KeyboardUtil() {
    }

    public static void fitContentAboveKeyboard(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), ime.bottom);
            return windowInsets;
        });
    }

    public static void hide(Activity activity) {
        View focused = activity.getCurrentFocus();
        View target = focused != null ? focused : activity.getWindow().getDecorView();
        InputMethodManager imm =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
        if (focused != null) {
            focused.clearFocus();
        }
    }
}
