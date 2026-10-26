package com.example.deundeun.data.network.dto.auth;

public class PasswordResetRequest {

    private final String resetToken;
    private final String newPassword;

    public PasswordResetRequest(String resetToken, String newPassword) {
        this.resetToken = resetToken;
        this.newPassword = newPassword;
    }
}
