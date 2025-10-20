package com.example.hygienebuddy;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class BottomNavHelper {

    public static void setupBottomNav(Fragment fragment, String currentPage) {
        Activity activity = fragment.requireActivity();

        // Find nav buttons
        LinearLayout navHome = activity.findViewById(R.id.navHome);
        LinearLayout navTasks = activity.findViewById(R.id.navTasks);
        LinearLayout navBadges = activity.findViewById(R.id.navBadges);
        LinearLayout navReport = activity.findViewById(R.id.navReport);
        LinearLayout navSettings = activity.findViewById(R.id.navSettings);

        // Find icons
        ImageView iconHome = activity.findViewById(R.id.iconHome);
        ImageView iconTasks = activity.findViewById(R.id.iconTasks);
        ImageView iconBadges = activity.findViewById(R.id.iconBadges);
        ImageView iconReport = activity.findViewById(R.id.iconReport);
        ImageView iconSettings = activity.findViewById(R.id.iconSettings);

        // Find text labels
        TextView txtHome = activity.findViewById(R.id.txtHome);
        TextView txtTasks = activity.findViewById(R.id.txtTasks);
        TextView txtBadges = activity.findViewById(R.id.txtBadges);
        TextView txtReport = activity.findViewById(R.id.txtReport);
        TextView txtSettings = activity.findViewById(R.id.txtSettings);

        // Reset icons and labels
        resetIcon(iconHome, txtHome, R.drawable.ic_home);
        resetIcon(iconTasks, txtTasks, R.drawable.ic_checklist);
        resetIcon(iconBadges, txtBadges, R.drawable.ic_trophy);
        resetIcon(iconReport, txtReport, R.drawable.ic_data);
        resetIcon(iconSettings, txtSettings, R.drawable.ic_setting);

        // Highlight current page
        switch (currentPage) {
            case "home": highlight(iconHome, txtHome, R.drawable.ic_home_filled); break;
            case "tasks": highlight(iconTasks, txtTasks, R.drawable.ic_checklist_filled); break;
            case "badges": highlight(iconBadges, txtBadges, R.drawable.ic_trophy_filled); break;
            case "report": highlight(iconReport, txtReport, R.drawable.ic_data_filled); break;
            case "settings": highlight(iconSettings, txtSettings, R.drawable.ic_setting_filled); break;
        }

        // Navigation click listeners (manual fragment switching)
        navHome.setOnClickListener(v -> navigate(fragment, currentPage, "home", new HomeDashboardFragment()));
        navTasks.setOnClickListener(v -> navigate(fragment, currentPage, "tasks", new FragmentTasks()));
        navBadges.setOnClickListener(v -> navigate(fragment, currentPage, "badges", new FragmentBadges()));
        navReport.setOnClickListener(v -> navigate(fragment, currentPage, "report", new FragmentReportSummary()));
        navSettings.setOnClickListener(v -> navigate(fragment, currentPage, "settings", new SettingsFragment()));
    }

    private static void navigate(Fragment fragment, String currentPage, String targetPage, Fragment destination) {
        if (currentPage.equals(targetPage)) return;

        FragmentActivity activity = fragment.requireActivity();
        if (activity.findViewById(R.id.fragment_container) == null) {
            Log.e("BottomNavHelper", "Navigation failed: fragment_container not found");
            return;
        }

        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        ft.replace(R.id.fragment_container, destination);
        ft.commit(); // No back stack for bottom nav
    }


    private static void resetIcon(ImageView icon, TextView label, int drawableId) {
        icon.setImageResource(drawableId);
        label.setTextColor(Color.parseColor("#6C7A89")); // gray
    }

    private static void highlight(ImageView icon, TextView label, int drawableId) {
        icon.setImageResource(drawableId);
        label.setTextColor(Color.parseColor("#000000")); // black
    }
}
