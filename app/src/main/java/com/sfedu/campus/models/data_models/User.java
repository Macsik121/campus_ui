package com.sfedu.campus.models.data_models;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class User {
    @SerializedName("id")
    private UUID id;
    @SerializedName("full_name")
    private String full_name;
    @SerializedName("email")
    private String email;
    @SerializedName("avatar")
    private String avatar;
    @SerializedName("phone_number")
    private String phone_number;
    @SerializedName("role")
    private String role;

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return full_name;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getPhoneNumber() {
        return phone_number;
    }

    public String getRole() {
        return role;
    }

}
