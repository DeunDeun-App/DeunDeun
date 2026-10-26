package com.example.deundeun.data.network.dto.auth;

public class ResetTokenRequest {

    private final String name;
    private final String email;

    public ResetTokenRequest(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
