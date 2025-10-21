package com.example.hygienebuddy;

/**
 * Model class representing a single step in a hygiene task
 */
public class TaskStep {
    private int stepNumber;
    private String instruction;
    private int imageResId;
    private int minutes;
    private int seconds;
    private boolean hasVideo;
    private String taskType;

    public TaskStep(int stepNumber, String instruction, int imageResId, int minutes, int seconds, String taskType) {
        this.stepNumber = stepNumber;
        this.instruction = instruction;
        this.imageResId = imageResId;
        this.minutes = minutes;
        this.seconds = seconds;
        this.taskType = taskType;
        this.hasVideo = false;
    }

    // Getters and Setters
    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public boolean hasVideo() {
        return hasVideo;
    }

    public void setHasVideo(boolean hasVideo) {
        this.hasVideo = hasVideo;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    /**
     * Gets the unique key for this step's video storage
     */
    public String getVideoKey() {
        return taskType.toLowerCase() + "_step_" + stepNumber;
    }

    /**
     * Gets the display text for this step
     */
    public String getDisplayText() {
        return "Step " + stepNumber + ": " + instruction;
    }

    /**
     * Gets the total duration in milliseconds
     */
    public long getDurationMillis() {
        return (minutes * 60L + seconds) * 1000;
    }
}
