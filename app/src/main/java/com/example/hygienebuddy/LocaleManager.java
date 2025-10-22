package com.example.hygienebuddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleManager {

    private static final String PREFS_NAME = "hb_prefs";
    private static final String KEY_LANG = "lang_code";
    private static final String DEFAULT_LANG = "en";

    public static void setLanguage(Context context, String languageCode) {
        getPrefs(context).edit().putString(KEY_LANG, languageCode).apply();
    }

    public static String getLanguage(Context context) {
        return getPrefs(context).getString(KEY_LANG, DEFAULT_LANG);
    }

    public static Context getLocalizedContext(Context context) {
        String lang = getLanguage(context);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            // Deprecated but safe for older devices
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }

    public static Resources getLocalizedResources(Context context) {
        return getLocalizedContext(context).getResources();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}

