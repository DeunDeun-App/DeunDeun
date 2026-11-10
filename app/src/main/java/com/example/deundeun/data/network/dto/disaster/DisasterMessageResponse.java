package com.example.deundeun.data.network.dto.disaster;

public class DisasterMessageResponse {

    private String id;
    private String region;
    private String text;
    private String step;
    private String sentAt;

    public String getId() { return id; }
    public String getRegion() { return region; }
    public String getText() { return text; }
    public String getStep() { return step; }
    public String getSentAt() { return sentAt; }
}
