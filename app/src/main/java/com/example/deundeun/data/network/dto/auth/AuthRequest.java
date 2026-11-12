package com.example.deundeun.data.network.dto.auth;

public class AuthRequest {

    private final String loginId;
    private final String name;
    private final String email;
    private final String password;
    private final String code;
    private final String resetToken;

    public AuthRequest(String loginId, String name, String email, String password, String code,
                       String resetToken) {
        this.loginId = loginId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.code = code;
        this.resetToken = resetToken;
    }
}
