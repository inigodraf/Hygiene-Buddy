package com.example.hygienebuddy;

public class BadgeModel {
    private String title;
    private String description;
    private boolean isEarned;
    private String earnedDate;
    private int progress;
    private int goal;

    public BadgeModel(String title, String description, boolean isEarned, String earnedDate, int progress, int goal) {
        this.title = title;
        this.description = description;
        this.isEarned = isEarned;
        this.earnedDate = earnedDate;
        this.progress = progress;
        this.goal = goal;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isEarned() { return isEarned; }
    public String getEarnedDate() { return earnedDate; }
    public int getProgress() { return progress; }
    public int getGoal() { return goal; }
}
