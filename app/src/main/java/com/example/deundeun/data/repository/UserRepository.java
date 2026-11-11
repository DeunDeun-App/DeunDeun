package com.example.deundeun.data.repository;

import android.content.Context;

import com.example.deundeun.data.network.dto.user.NotificationResponse;
import com.example.deundeun.data.network.dto.user.ProfileResponse;
import com.example.deundeun.data.network.dto.user.ProfileUpdateRequest;
import com.example.deundeun.data.network.dto.user.SettingsRequest;
import com.example.deundeun.data.session.SessionManager;

import java.util.List;

public class UserRepository extends BaseRepository {

    private final SessionManager sessionManager;

    public UserRepository(Context context) {
        sessionManager = new SessionManager(context);
    }

    public void getProfile(Callback<Result<ProfileResponse>> callback) {
        enqueue(api.getProfile(sessionManager.getAuthorization()), callback);
    }

    public void updateProfile(ProfileUpdateRequest request,
                              Callback<Result<ProfileResponse>> callback) {
        enqueue(api.updateProfile(sessionManager.getAuthorization(), request), callback);
    }

    public void updateSettings(SettingsRequest request, Callback<Result<ProfileResponse>> callback) {
        enqueue(api.updateSettings(sessionManager.getAuthorization(), request), callback);
    }

    public void getNotifications(Callback<Result<List<NotificationResponse>>> callback) {
        enqueue(api.getNotifications(sessionManager.getAuthorization()), callback);
    }

    public void logout(Callback<Result<Void>> callback) {
        enqueue(api.logout(sessionManager.getAuthorization()), result -> {
            sessionManager.clear();
            callback.onResult(result);
        });
    }

    public void withdraw(Callback<Result<Void>> callback) {
        enqueue(api.withdraw(sessionManager.getAuthorization()), result -> {
            if (result.isSuccess()) {
                sessionManager.clear();
            }
            callback.onResult(result);
        });
    }
}
