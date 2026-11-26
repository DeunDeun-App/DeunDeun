package com.example.deundeun.data.network.dto.user;

public class SettingsRequest {

    private final Boolean shareLocation;
    private final Boolean sosAlert;

    public SettingsRequest(Boolean shareLocation, Boolean sosAlert) {
        this.shareLocation = shareLocation;
        this.sosAlert = sosAlert;
    }
}
