package com.example.deundeun.data.network.dto.auth;

public class LoginResponse {

    private String token;
    private UserResponse user;

    public String getToken() { return token; }
    public UserResponse getUser() { return user; }
}
