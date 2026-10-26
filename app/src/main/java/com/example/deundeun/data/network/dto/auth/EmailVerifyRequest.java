package com.example.deundeun.data.network.dto.auth;

public class EmailVerifyRequest {

    private final String email;
    private final String code;

    public EmailVerifyRequest(String email, String code) {
        this.email = email;
        this.code = code;
    }
}
