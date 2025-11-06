package com.example.hygienebuddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.text.TextUtils;

public class BadgeThemeManager {

    public enum Theme {
        BUBBLE_QUEST("bubble"),
        CLEAN_HEROES("heroes");

        public final String suffix;
        Theme(String suffix) { this.suffix = suffix; }

        public static Theme fromString(String value) {
            if ("heroes".equalsIgnoreCase(value) || "clean_heroes".equalsIgnoreCase(value)) {
                return CLEAN_HEROES;
            }
            return BUBBLE_QUEST;
        }
    }

    private static final String PREFS_NAME = "hygiene_buddy_prefs";
    private static final String KEY_BADGE_THEME = "badge_theme"; // stores "bubble" or "heroes"
    public static final String ACTION_BADGE_THEME_CHANGED = "com.example.hygienebuddy.BADGE_THEME_CHANGED";

    public static Theme getCurrentTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_BADGE_THEME, "bubble");
        return Theme.fromString(raw);
    }

    public static void setCurrentTheme(Context context, Theme theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BADGE_THEME, theme.suffix).apply();
        // notify app-wide
        Intent intent = new Intent(ACTION_BADGE_THEME_CHANGED);
        context.sendBroadcast(intent);
    }

    public static int getBadgeIconRes(Context context, String badgeTitle) {
        Theme theme = getCurrentTheme(context);
        String key = normalizeKey(badgeTitle);
        int resId;

        // New naming: badge_<key>_<suffix> where suffix is _bq or _ch
        String suffix = (theme == Theme.CLEAN_HEROES) ? "ch" : "bq";
        resId = tryNames(context,
                "badge_" + key + "_" + suffix,
                // legacy fallbacks maintained for compatibility
                (theme == Theme.CLEAN_HEROES) ? "ch_badge_" + key : "bq_badge_" + key,
                (theme == Theme.CLEAN_HEROES) ? "badge_" + key + "_heroes" : "badge_" + key + "_bubble",
                // generic without suffix
                "badge_" + key
        );
        if (resId == 0) {
            // placeholder then final fallback
            resId = tryNames(context, "badge_placeholder");
        }
        if (resId == 0) {
            resId = R.drawable.ic_trophy;
        }
        return resId;
    }

    public static int getBadgeIconResByKey(Context context, String imageKey) {
        Theme theme = getCurrentTheme(context);
        String key = imageKey == null ? "" : imageKey.toLowerCase().replaceAll("[^a-z0-9_]", "");
        int resId;
        String suffix = (theme == Theme.CLEAN_HEROES) ? "ch" : "bq";
        resId = tryNames(context,
                "badge_" + key + "_" + suffix,
                // legacy fallbacks
                (theme == Theme.CLEAN_HEROES) ? "ch_badge_" + key : "bq_badge_" + key,
                (theme == Theme.CLEAN_HEROES) ? "badge_" + key + "_heroes" : "badge_" + key + "_bubble",
                // generic
                "badge_" + key
        );
        if (resId == 0) {
            resId = tryNames(context, "badge_placeholder");
        }
        if (resId == 0) {
            resId = R.drawable.ic_trophy;
        }
        return resId;
    }

    private static int tryNames(Context context, String... names) {
        for (String name : names) {
            int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
            if (id != 0) return id;
        }
        return 0;
    }

    private static String normalizeKey(String title) {
        if (title == null) return "";
        String onlyAlnum = title.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
        if (TextUtils.isEmpty(onlyAlnum)) return "";
        return onlyAlnum.replaceAll(" +", "_");
    }
}


