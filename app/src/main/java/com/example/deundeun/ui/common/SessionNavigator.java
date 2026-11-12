package com.example.deundeun.ui.common;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.deundeun.data.session.SessionManager;
import com.example.deundeun.ui.auth.LoginActivity;

public final class SessionNavigator {

    private SessionNavigator() {
    }

    public static void toLogin(Activity activity, @Nullable @StringRes Integer messageRes) {
        new SessionManager(activity).clear();
        if (messageRes != null) {
            Toast.makeText(activity, messageRes, Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
