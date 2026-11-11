package com.example.deundeun.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public final class DialogHelper {

    private DialogHelper() {
    }

    public static Dialog show(Context context, View content, float widthRatio, boolean cancelable) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(content);
        dialog.setCancelable(cancelable);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * widthRatio),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();
        return dialog;
    }
}
