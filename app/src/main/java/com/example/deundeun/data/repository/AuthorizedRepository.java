package com.example.deundeun.data.repository;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.deundeun.data.session.SessionManager;

public abstract class AuthorizedRepository extends BaseRepository {

    protected final SessionManager sessionManager;

    protected AuthorizedRepository(Context context) {
        sessionManager = new SessionManager(context);
    }

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    @Nullable
    protected String authorization() {
        return sessionManager.getAuthorization();
    }
}
