package com.example.hygienebuddy;

import android.content.Context;

import java.util.List;

public class BadgeRepository {

    private final BadgeDatabaseHelper db;

    public BadgeRepository(Context context) {
        this.db = new BadgeDatabaseHelper(context.getApplicationContext());
        this.db.seedIfEmpty();
    }

    public List<BadgeModel> getAllBadges() {
        return db.getAllBadges();
    }

    public List<BadgeModel> getEarnedBadges() {
        return db.getEarnedBadges();
    }

    public void unlockBadge(String badgeKey, String earnedDate) {
        db.unlockBadgeByKey(badgeKey, earnedDate);
    }

    public void updateProgress(String badgeKey, int progress) {
        db.updateProgressByKey(badgeKey, progress);
    }

    /** Get a badge by its key */
    public BadgeModel getBadgeByKey(String badgeKey) {
        return db.getBadgeByKey(badgeKey);
    }
}


