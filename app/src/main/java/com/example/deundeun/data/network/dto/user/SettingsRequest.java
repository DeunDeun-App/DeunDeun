package com.example.deundeun.data.network.dto.user;

public class SettingsRequest {

    private final Boolean shareLocation;
    private final Boolean shareResult;
    private final Boolean sosAlert;

    public SettingsRequest(Boolean shareLocation, Boolean shareResult, Boolean sosAlert) {
        this.shareLocation = shareLocation;
        this.shareResult = shareResult;
        this.sosAlert = sosAlert;
    }
}
