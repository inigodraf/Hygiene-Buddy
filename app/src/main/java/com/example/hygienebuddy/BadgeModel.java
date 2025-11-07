package com.example.hygienebuddy;

public class BadgeModel {
    private String title;
    private String description;
    private boolean isEarned;
    private String earnedDate;
    private int progress;
    private int goal;
    private String imageKey; // optional explicit key to resolve drawable

    public BadgeModel(String title, String description, boolean isEarned, String earnedDate, int progress, int goal, String imageKey) {
        this.title = title;
        this.description = description;
        this.isEarned = isEarned;
        this.earnedDate = earnedDate;
        this.progress = progress;
        this.goal = goal;
        this.imageKey = imageKey;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isEarned() { return isEarned; }
    public String getEarnedDate() { return earnedDate; }
    public int getProgress() { return progress; }
    public int getGoal() { return goal; }
    public String getImageKey() { return imageKey; }
}
