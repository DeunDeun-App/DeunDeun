package com.example.deundeun.data.network.dto.user;

public class NotificationResponse {

    private String type;
    private String title;
    private String body;
    private String createdAt;

    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getCreatedAt() { return createdAt; }
}
