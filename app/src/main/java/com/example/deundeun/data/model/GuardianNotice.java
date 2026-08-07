package com.example.deundeun.data.model;

/** 보호자에게 보낸 알림 한 건. */
public class GuardianNotice {

    private final String title;
    private final String description;
    private final String date;
    private final String time;

    public GuardianNotice(String title, String description, String date, String time) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }
}
