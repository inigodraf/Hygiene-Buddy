package com.example.hygienebuddy;

import android.net.Uri;

public class ProfileModel {
    private String name;
    private int age;
    private String condition;
    private Uri imageUri;       // To store photo from gallery or camera
    private int avatarResId;    // Optional fallback drawable resource

    // --- Constructors ---
    public ProfileModel(String name, int age, String condition, Uri imageUri) {
        this.name = name;
        this.age = age;
        this.condition = condition;
        this.imageUri = imageUri;
    }

    // Fallback constructor (e.g., for default avatars)
    public ProfileModel(String name, int avatarResId) {
        this.name = name;
        this.avatarResId = avatarResId;
    }

    // --- Getters and Setters ---
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public int getAvatarResId() {
        return avatarResId;
    }

    public void setAvatarResId(int avatarResId) {
        this.avatarResId = avatarResId;
    }
}
