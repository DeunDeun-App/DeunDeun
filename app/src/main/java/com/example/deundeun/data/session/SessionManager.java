package com.example.deundeun.data.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class SessionManager {

    private static final String PREFS_NAME = "deundeun_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_LOGIN_ID = "login_id";
    private static final String KEY_USER_NAME = "user_name";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void save(String token, String loginId, String userName) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_LOGIN_ID, loginId)
                .putString(KEY_USER_NAME, userName)
                .apply();
    }

    public void updateUser(String loginId, String userName) {
        prefs.edit()
                .putString(KEY_LOGIN_ID, loginId)
                .putString(KEY_USER_NAME, userName)
                .apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    @Nullable
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    @Nullable
    public String getAuthorization() {
        String token = getToken();
        return token == null ? null : "Bearer " + token;
    }

    @Nullable
    public String getLoginId() {
        return prefs.getString(KEY_LOGIN_ID, null);
    }

    @Nullable
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
