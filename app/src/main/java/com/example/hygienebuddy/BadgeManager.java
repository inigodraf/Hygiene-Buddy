package com.example.hygienebuddy;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class BadgeManager {

    private final BadgeRepository repository;
    private final Context context;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public BadgeManager(Context context) {
        this.repository = new BadgeRepository(context.getApplicationContext());
        this.context = context.getApplicationContext();
    }

    // Record completion of a specific task type (e.g., "handwashing" or "toothbrushing")
    public void recordTaskCompletion(String taskType) {
        String today = dateFormat.format(new Date());

        // Get current profile ID
        SharedPreferences sharedPref = context.getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
        int currentProfileId = sharedPref.getInt("current_profile_id", -1);
        if (currentProfileId == -1) {
            currentProfileId = sharedPref.getInt("selected_profile_id", -1);
        }

        // Record task completion for this specific day (profile-scoped)
        if (currentProfileId > 0) {
            recordTaskCompletionForDate(currentProfileId, today, taskType);
        }

        // Clean Habit Starter: first ever completion of any hygiene task (profile-scoped)
        unlockIfNotUnlocked("clean_habit_starter", today, currentProfileId);

        if ("handwashing".equalsIgnoreCase(taskType)) {
            incrementProgressAndMaybeUnlock("handwashing_hero", 10, today, currentProfileId);
        } else if ("toothbrushing".equalsIgnoreCase(taskType)) {
            incrementProgressAndMaybeUnlock("toothbrushing_champ", 10, today, currentProfileId);
        }
    }

    /** Record task completion for a specific date (profile-scoped) */
    private void recordTaskCompletionForDate(int profileId, String date, String taskType) {
        SharedPreferences sharedPref = context.getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);

        // Store which tasks were completed on this date
        // Format: "task_completions_{date}_profile_{profileId}" = "handwashing,toothbrushing"
        String taskCompletionsKey = "task_completions_" + date + "_profile_" + profileId;
        String existingTasks = sharedPref.getString(taskCompletionsKey, "");

        Set<String> completedTasks = new HashSet<>();
        if (!existingTasks.isEmpty()) {
            String[] tasks = existingTasks.split(",");
            for (String task : tasks) {
                if (!task.trim().isEmpty()) {
                    completedTasks.add(task.trim().toLowerCase());
                }
            }
        }

        // Add the current task
        completedTasks.add(taskType.toLowerCase());

        // Save back as comma-separated string
        StringBuilder sb = new StringBuilder();
        for (String task : completedTasks) {
            if (sb.length() > 0) sb.append(",");
            sb.append(task);
        }

        sharedPref.edit().putString(taskCompletionsKey, sb.toString()).apply();

        // Check if both tasks are completed for this day - if so, mark day as streak-complete
        if (completedTasks.contains("handwashing") && completedTasks.contains("toothbrushing")) {
            // Both tasks completed - mark this day as streak-complete
            String completedDaysKey = "completed_days_profile_" + profileId;
            String existingDays = sharedPref.getString(completedDaysKey, "");

            Set<String> completedDays = new HashSet<>();
            if (!existingDays.isEmpty()) {
                String[] days = existingDays.split(",");
                for (String day : days) {
                    if (!day.trim().isEmpty()) {
                        completedDays.add(day.trim());
                    }
                }
            }

            completedDays.add(date);

            // Save back as comma-separated string
            StringBuilder daysSb = new StringBuilder();
            for (String day : completedDays) {
                if (daysSb.length() > 0) daysSb.append(",");
                daysSb.append(day);
            }

            sharedPref.edit().putString(completedDaysKey, daysSb.toString()).apply();
        }
    }

    // Record days where ALL daily tasks were completed consecutively
    public void recordAllDailyTasksConsecutiveDays(int consecutiveDays) {
        String today = dateFormat.format(new Date());

        // Get current profile ID
        SharedPreferences sharedPref = context.getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
        int currentProfileId = sharedPref.getInt("current_profile_id", -1);
        if (currentProfileId == -1) {
            currentProfileId = sharedPref.getInt("selected_profile_id", -1);
        }

        updateProgress("routine_master", consecutiveDays, currentProfileId);
        if (consecutiveDays >= 7) {
            unlockIfNotUnlocked("routine_master", today, currentProfileId);
        }
    }

    // Update daily streak count; unlock milestones and update progress bars for streak badges
    public void updateDailyStreak(int streakDays) {
        String today = dateFormat.format(new Date());

        // Get current profile ID
        SharedPreferences sharedPref = context.getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
        int currentProfileId = sharedPref.getInt("current_profile_id", -1);
        if (currentProfileId == -1) {
            currentProfileId = sharedPref.getInt("selected_profile_id", -1);
        }

        // Update progress for known streak milestones (progress shown against goal)
        setProgressWithGoal("streak_1", streakDays, 1, currentProfileId);
        setProgressWithGoal("streak_3", streakDays, 3, currentProfileId);
        setProgressWithGoal("streak_7", streakDays, 7, currentProfileId);
        setProgressWithGoal("streak_14", streakDays, 14, currentProfileId);
        setProgressWithGoal("streak_30", streakDays, 30, currentProfileId);
        setProgressWithGoal("streak_60", streakDays, 60, currentProfileId);
        setProgressWithGoal("streak_100", streakDays, 100, currentProfileId);

        // Unlock when thresholds reached
        if (streakDays >= 1) unlockIfNotUnlocked("streak_1", today, currentProfileId);
        if (streakDays >= 3) unlockIfNotUnlocked("streak_3", today, currentProfileId);
        if (streakDays >= 7) unlockIfNotUnlocked("streak_7", today, currentProfileId);
        if (streakDays >= 14) unlockIfNotUnlocked("streak_14", today, currentProfileId);
        if (streakDays >= 30) unlockIfNotUnlocked("streak_30", today, currentProfileId);
        if (streakDays >= 60) unlockIfNotUnlocked("streak_60", today, currentProfileId);
        if (streakDays >= 100) unlockIfNotUnlocked("streak_100", today, currentProfileId);
    }


    private void incrementProgressAndMaybeUnlock(String badgeKey, int goal, String date, int profileId) {
        // Get profile-scoped progress from SharedPreferences
        SharedPreferences sharedPref = context.getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
        String progressKey = "badge_progress_" + badgeKey + "_profile_" + profileId;

        // Read current progress (profile-scoped)
        int current = sharedPref.getInt(progressKey, 0);
        int next = current + 1;

        // Save profile-scoped progress
        sharedPref.edit().putInt(progressKey, next).apply();

        // Also update badge database (for backward compatibility)
        repository.updateProgress(badgeKey, next);

        if (next >= goal) {
            // Save profile-scoped unlock status
            String unlockKey = "badge_unlocked_" + badgeKey + "_profile_" + profileId;
            sharedPref.edit().putBoolean(unlockKey, true).putString("badge_earned_date_" + badgeKey + "_profile_" + profileId, date).apply();

            // Also update badge database
            repository.unlockBadge(badgeKey, date);
        }
    }

    private void updateProgress(String badgeKey, int progress, int profileId) {
        // Save profile-scoped progress
        SharedPreferences sharedPref = context.getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
        String progressKey = "badge_progress_" + badgeKey + "_profile_" + profileId;
        sharedPref.edit().putInt(progressKey, progress).apply();

        // Also update badge database (for backward compatibility)
        repository.updateProgress(badgeKey, progress);
    }

    private void setProgressWithGoal(String badgeKey, int progress, int goal, int profileId) {
        int clamped = Math.min(progress, goal);
        updateProgress(badgeKey, clamped, profileId);
    }

    private void unlockIfNotUnlocked(String badgeKey, String date, int profileId) {
        // Check profile-scoped unlock status
        SharedPreferences sharedPref = context.getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
        String unlockKey = "badge_unlocked_" + badgeKey + "_profile_" + profileId;

        if (!sharedPref.getBoolean(unlockKey, false)) {
            // Save profile-scoped unlock
            sharedPref.edit()
                    .putBoolean(unlockKey, true)
                    .putString("badge_earned_date_" + badgeKey + "_profile_" + profileId, date)
                    .apply();

            // Also update badge database (for backward compatibility)
            repository.unlockBadge(badgeKey, date);
        }
    }
}


