package com.example.deundeun.data.network.dto.auth;

public class FindEmailRequest {

    private final String name;
    private final String email;

    public FindEmailRequest(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
