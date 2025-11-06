package com.example.hygienebuddy;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BadgeManager {

    private final BadgeRepository repository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public BadgeManager(Context context) {
        this.repository = new BadgeRepository(context.getApplicationContext());
    }

    // Record completion of a specific task type (e.g., "handwashing" or "toothbrushing")
    public void recordTaskCompletion(String taskType) {
        String today = dateFormat.format(new Date());

        // Clean Habit Starter: first ever completion of any hygiene task
        unlockIfNotUnlocked("clean_habit_starter", today);

        if ("handwashing".equalsIgnoreCase(taskType)) {
            incrementProgressAndMaybeUnlock("handwashing_hero", 10, today);
        } else if ("toothbrushing".equalsIgnoreCase(taskType)) {
            incrementProgressAndMaybeUnlock("toothbrushing_champ", 10, today);
        }
    }

    // Record days where ALL daily tasks were completed consecutively
    public void recordAllDailyTasksConsecutiveDays(int consecutiveDays) {
        String today = dateFormat.format(new Date());
        updateProgress("routine_master", consecutiveDays);
        if (consecutiveDays >= 7) {
            unlockIfNotUnlocked("routine_master", today);
        }
    }

    // Update daily streak count; unlock milestones and update progress bars for streak badges
    public void updateDailyStreak(int streakDays) {
        String today = dateFormat.format(new Date());

        // Update progress for known streak milestones (progress shown against goal)
        setProgressWithGoal("streak_1", streakDays, 1);
        setProgressWithGoal("streak_3", streakDays, 3);
        setProgressWithGoal("streak_7", streakDays, 7);
        setProgressWithGoal("streak_14", streakDays, 14);
        setProgressWithGoal("streak_30", streakDays, 30);
        setProgressWithGoal("streak_60", streakDays, 60);
        setProgressWithGoal("streak_100", streakDays, 100);

        // Unlock when thresholds reached
        if (streakDays >= 1) unlockIfNotUnlocked("streak_1", today);
        if (streakDays >= 3) unlockIfNotUnlocked("streak_3", today);
        if (streakDays >= 7) unlockIfNotUnlocked("streak_7", today);
        if (streakDays >= 14) unlockIfNotUnlocked("streak_14", today);
        if (streakDays >= 30) unlockIfNotUnlocked("streak_30", today);
        if (streakDays >= 60) unlockIfNotUnlocked("streak_60", today);
        if (streakDays >= 100) unlockIfNotUnlocked("streak_100", today);

        /*
        // Super Streak (30+ days)
        if (streakDays >= 30) {
            updateProgress("super_streak", streakDays);
            unlockIfNotUnlocked("super_streak", today);
        }
        */
    }


    private void incrementProgressAndMaybeUnlock(String badgeKey, int goal, String date) {
        // Read current progress by loading all and finding key (simple for now)
        int current = 0;
        for (BadgeModel b : repository.getAllBadges()) {
            if (badgeKey.equals(b.getImageKey())) {
                current = b.getProgress();
                break;
            }
        }
        int next = current + 1;
        repository.updateProgress(badgeKey, next);
        if (next >= goal) {
            repository.unlockBadge(badgeKey, date);
        }
    }

    private void updateProgress(String badgeKey, int progress) {
        repository.updateProgress(badgeKey, progress);
    }

    private void setProgressWithGoal(String badgeKey, int progress, int goal) {
        int clamped = Math.min(progress, goal);
        repository.updateProgress(badgeKey, clamped);
    }

    private void unlockIfNotUnlocked(String badgeKey, String date) {
        for (BadgeModel b : repository.getAllBadges()) {
            if (badgeKey.equals(b.getImageKey())) {
                if (!b.isEarned()) {
                    repository.unlockBadge(badgeKey, date);
                }
                return;
            }
        }
        // If badge not in DB yet, nothing to do (seed should have created it)
    }
}


