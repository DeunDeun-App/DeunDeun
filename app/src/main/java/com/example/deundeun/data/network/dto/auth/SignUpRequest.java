package com.example.deundeun.data.network.dto.auth;

public class SignUpRequest {

    private final String name;
    private final String email;
    private final String password;

    public SignUpRequest(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
}
