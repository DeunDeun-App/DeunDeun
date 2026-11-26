package com.example.deundeun.data.repository;

import android.content.Context;

import com.example.deundeun.data.network.dto.user.NotificationResponse;
import com.example.deundeun.data.network.dto.user.ProfileResponse;
import com.example.deundeun.data.network.dto.user.ProfileUpdateRequest;
import com.example.deundeun.data.network.dto.user.SettingsRequest;

import java.util.List;

public class UserRepository extends AuthorizedRepository {

    public UserRepository(Context context) {
        super(context);
    }

    public void getProfile(Callback<Result<ProfileResponse>> callback) {
        enqueue(api.getProfile(authorization()), callback);
    }

    public void updateProfile(ProfileUpdateRequest request,
                              Callback<Result<ProfileResponse>> callback) {
        enqueue(api.updateProfile(authorization(), request), callback);
    }

    public void updateSettings(SettingsRequest request, Callback<Result<ProfileResponse>> callback) {
        enqueue(api.updateSettings(authorization(), request), callback);
    }

    public void getNotifications(Callback<Result<List<NotificationResponse>>> callback) {
        enqueue(api.getNotifications(authorization()), callback);
    }

    public void logout(Callback<Result<Void>> callback) {
        enqueue(api.logout(authorization()), result -> {
            sessionManager.clear();
            callback.onResult(result);
        });
    }

    public void withdraw(Callback<Result<Void>> callback) {
        enqueue(api.withdraw(authorization()), result -> {
            if (result.isSuccess()) {
                sessionManager.clear();
            }
            callback.onResult(result);
        });
    }
}
