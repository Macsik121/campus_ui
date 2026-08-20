package com.sfedu.campus.models.server_requests;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    public RegisterRequest(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
}
