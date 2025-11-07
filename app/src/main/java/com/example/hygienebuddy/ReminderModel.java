package com.example.hygienebuddy;

public class ReminderModel {
    private int id;
    private String taskName;
    private String time;
    private String frequency; // "once", "daily", "weekly", "custom", "multiple"
    private boolean isActive;
    private int customInterval; // For custom interval (e.g., every 2 days, 3 days)
    private String daysOfWeek; // For weekly reminders (e.g., "Mon,Wed,Fri")
    private String timesPerDay; // For multiple times per day (JSON array)
    private Integer profileId; // Profile ID associated with this reminder (nullable, defaults to 0)

    public ReminderModel() {}

    public ReminderModel(int id, String taskName, String time, String frequency, boolean isActive) {
        this.id = id;
        this.taskName = taskName;
        this.time = time;
        this.frequency = frequency;
        this.isActive = isActive;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getCustomInterval() { return customInterval; }
    public void setCustomInterval(int customInterval) { this.customInterval = customInterval; }

    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public String getTimesPerDay() { return timesPerDay; }
    public void setTimesPerDay(String timesPerDay) { this.timesPerDay = timesPerDay; }

    public Integer getProfileId() { return profileId; }
    public void setProfileId(Integer profileId) { this.profileId = profileId; }
}
