package com.example.deundeun.data.network.dto.auth;

public class UserResponse {

    private long userId;
    private String loginId;
    private String email;
    private String userName;

    public long getUserId() { return userId; }
    public String getLoginId() { return loginId; }
    public String getEmail() { return email; }
    public String getUserName() { return userName; }
}
