package com.example.hygienebuddy;

import android.content.Context;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class BadgeManager {

    private final BadgeRepository repository;
    private final Context context;
    private final AppDataDatabaseHelper appDataDb;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public BadgeManager(Context context) {
        this.repository = new BadgeRepository(context.getApplicationContext());
        this.context = context.getApplicationContext();
        this.appDataDb = new AppDataDatabaseHelper(context.getApplicationContext());
    }

    // Record completion of a specific task type (e.g., "handwashing" or "toothbrushing")
    public void recordTaskCompletion(String taskType) {
        String today = dateFormat.format(new Date());

        // Get current profile ID from SQLite
        int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
        if (currentProfileId == -1) {
            currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
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
        // Add task completion to SQLite database
        appDataDb.addTaskCompletion(profileId, date, taskType.toLowerCase());

        // Get all completed tasks for this date
        Set<String> completedTasks = appDataDb.getTaskCompletionsForDate(profileId, date);

        // Check if both tasks are completed for this day - if so, mark day as streak-complete
        if (completedTasks.contains("handwashing") && completedTasks.contains("toothbrushing")) {
            // Both tasks completed - mark this day as streak-complete
            appDataDb.addStreakDay(profileId, date);

            // Calculate current consecutive streak and update badge progress/unlocks
            int currentStreak = calculateCurrentStreak(profileId);
            updateDailyStreak(currentStreak);

            // Also update routine_master badge (7+ consecutive days)
            recordAllDailyTasksConsecutiveDays(currentStreak);
        }
    }

    /** Calculate current consecutive streak from today backwards (profile-scoped) */
    private int calculateCurrentStreak(int profileId) {
        Set<String> streakDays = appDataDb.getStreakDays(profileId);
        if (streakDays == null || streakDays.isEmpty()) {
            return 0;
        }

        try {
            java.util.Calendar today = java.util.Calendar.getInstance();
            int streak = 0;

            while (true) {
                String dateKey = dateFormat.format(today.getTime());
                if (streakDays.contains(dateKey)) {
                    streak++;
                    today.add(java.util.Calendar.DAY_OF_YEAR, -1);
                } else {
                    break;
                }
            }

            return streak;
        } catch (Exception e) {
            android.util.Log.e("BadgeManager", "Error calculating current streak: " + e.getMessage(), e);
            return 0;
        }
    }

    // Record days where ALL daily tasks were completed consecutively
    public void recordAllDailyTasksConsecutiveDays(int consecutiveDays) {
        String today = dateFormat.format(new Date());

        // Get current profile ID from SQLite
        int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
        if (currentProfileId == -1) {
            currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
        }

        updateProgress("routine_master", consecutiveDays, currentProfileId);
        if (consecutiveDays >= 7) {
            unlockIfNotUnlocked("routine_master", today, currentProfileId);
        }
    }

    // Update daily streak count; unlock milestones and update progress bars for streak badges
    public void updateDailyStreak(int streakDays) {
        String today = dateFormat.format(new Date());

        // Get current profile ID from SQLite
        int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
        if (currentProfileId == -1) {
            currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
        }

        if (currentProfileId <= 0) {
            android.util.Log.w("BadgeManager", "No profile ID available for streak update");
            return;
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

        android.util.Log.d("BadgeManager", "Updated streak badges for profile " + currentProfileId + " - Current streak: " + streakDays);
    }


    private void incrementProgressAndMaybeUnlock(String badgeKey, int goal, String date, int profileId) {
        // Get profile-scoped progress from SQLite
        int current = appDataDb.getBadgeProgress(profileId, badgeKey);
        int next = current + 1;
        appDataDb.setBadgeProgress(profileId, badgeKey, next);
        repository.updateProgress(badgeKey, next);

        if (next >= goal) {
            appDataDb.setBadgeUnlocked(profileId, badgeKey, true, date);
            repository.unlockBadge(badgeKey, date);
            showBadgeUnlockToast(badgeKey);
        }
    }

    private void updateProgress(String badgeKey, int progress, int profileId) {
        appDataDb.setBadgeProgress(profileId, badgeKey, progress);
        repository.updateProgress(badgeKey, progress);
    }

    private void setProgressWithGoal(String badgeKey, int progress, int goal, int profileId) {
        int clamped = Math.min(progress, goal);
        updateProgress(badgeKey, clamped, profileId);
    }

    private void unlockIfNotUnlocked(String badgeKey, String date, int profileId) {
        if (!appDataDb.isBadgeUnlocked(profileId, badgeKey)) {
            appDataDb.setBadgeUnlocked(profileId, badgeKey, true, date);
            repository.unlockBadge(badgeKey, date);
            showBadgeUnlockToast(badgeKey);
        }
    }

    /** Show Toast notification when a badge is unlocked */
    private void showBadgeUnlockToast(String badgeKey) {
        try {
            // Get badge name from repository
            BadgeModel badge = repository.getBadgeByKey(badgeKey);
            String badgeName = "Badge";

            if (badge != null && badge.getTitle() != null) {
                badgeName = badge.getTitle();
            } else {
                // Fallback: format badge key to a readable name
                badgeName = formatBadgeKeyToName(badgeKey);
            }

            // Show Toast on main thread
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            String finalBadgeName = badgeName;
            mainHandler.post(() -> {
                Toast.makeText(context, "🎉 " + finalBadgeName + " Unlocked!", Toast.LENGTH_LONG).show();
            });

            android.util.Log.d("BadgeManager", "Badge unlocked: " + badgeName + " (key: " + badgeKey + ")");
        } catch (Exception e) {
            android.util.Log.e("BadgeManager", "Error showing badge unlock toast: " + e.getMessage(), e);
            // Fallback: show generic message
            try {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> {
                    Toast.makeText(context, "🎉 Badge Unlocked!", Toast.LENGTH_LONG).show();
                });
            } catch (Exception ex) {
                android.util.Log.e("BadgeManager", "Error showing fallback toast: " + ex.getMessage(), ex);
            }
        }
    }

    /** Format badge key to a readable name (fallback) */
    private String formatBadgeKeyToName(String badgeKey) {
        if (badgeKey == null || badgeKey.isEmpty()) {
            return "Badge";
        }

        // Replace underscores with spaces and capitalize words
        String formatted = badgeKey.replace("_", " ");
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }

        return result.length() > 0 ? result.toString() : "Badge";
    }
}


