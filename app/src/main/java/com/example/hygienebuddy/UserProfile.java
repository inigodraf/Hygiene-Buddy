package com.example.hygienebuddy;

public class UserProfile {
    private int id;
    private String name;
    private int age;
    private String condition;
    private String imageUri;

    public UserProfile() {}

    public UserProfile(String name, int age, String condition, String imageUri) {
        this.name = name;
        this.age = age;
        this.condition = condition;
        this.imageUri = imageUri;
    }

    public UserProfile(int id, String name, int age, String condition, String imageUri) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.condition = condition;
        this.imageUri = imageUri;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getAgeDisplayText() {
        return age + " years old";
    }

    public boolean hasImage() {
        return imageUri != null && !imageUri.isEmpty();
    }
}
