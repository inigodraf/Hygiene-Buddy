package com.example.hygienebuddy;

import android.app.Activity;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class BottomNavHelper {

    public static void setupBottomNav(Fragment fragment, String currentPage) {

        Activity activity = fragment.requireActivity();

        // Navigation containers
        LinearLayout navHome = activity.findViewById(R.id.navHome);
        LinearLayout navTasks = activity.findViewById(R.id.navTasks);
        LinearLayout navBadges = activity.findViewById(R.id.navBadges);
        LinearLayout navReport = activity.findViewById(R.id.navReport);
        LinearLayout navSettings = activity.findViewById(R.id.navSettings);

        // Icons
        ImageView iconHome = activity.findViewById(R.id.iconHome);
        ImageView iconTasks = activity.findViewById(R.id.iconTasks);
        ImageView iconBadges = activity.findViewById(R.id.iconBadges);
        ImageView iconReport = activity.findViewById(R.id.iconReport);
        ImageView iconSettings = activity.findViewById(R.id.iconSettings);

        // Labels
        TextView txtHome = activity.findViewById(R.id.txtHome);
        TextView txtTasks = activity.findViewById(R.id.txtTasks);
        TextView txtBadges = activity.findViewById(R.id.txtBadges);
        TextView txtReport = activity.findViewById(R.id.txtReport);
        TextView txtSettings = activity.findViewById(R.id.txtSettings);

        // Reset all icons and labels
        resetIcons(iconHome, txtHome, R.drawable.ic_home, fragment);
        resetIcons(iconTasks, txtTasks, R.drawable.ic_checklist, fragment);
        resetIcons(iconBadges, txtBadges, R.drawable.ic_trophy, fragment);
        resetIcons(iconReport, txtReport, R.drawable.ic_data, fragment);
        resetIcons(iconSettings, txtSettings, R.drawable.ic_setting, fragment);

        // Highlight current page
        switch (currentPage) {
            case "home": highlight(iconHome, txtHome, R.drawable.ic_home_filled); break;
            case "tasks": highlight(iconTasks, txtTasks, R.drawable.ic_checklist_filled); break;
            case "badges": highlight(iconBadges, txtBadges, R.drawable.ic_trophy_filled); break;
            case "report": highlight(iconReport, txtReport, R.drawable.ic_data_filled); break;
            case "settings": highlight(iconSettings, txtSettings, R.drawable.ic_setting_filled); break;
        }

        // Navigation click listeners using NavController
        navHome.setOnClickListener(v -> {
            if (!currentPage.equals("home")) Navigation.findNavController(v)
                    .navigate(R.id.homeDashboardFragment);
        });
        navTasks.setOnClickListener(v -> {
            if (!currentPage.equals("tasks")) Navigation.findNavController(v)
                    .navigate(R.id.fragmentTasks);
        });
        navBadges.setOnClickListener(v -> {
            if (!currentPage.equals("badges")) Navigation.findNavController(v)
                    .navigate(R.id.fragmentBadges);
        });
        navReport.setOnClickListener(v -> {
            if (!currentPage.equals("report")) Navigation.findNavController(v)
                    .navigate(R.id.fragmentReportSummary);
        });
        navSettings.setOnClickListener(v -> {
            if (!currentPage.equals("settings")) Navigation.findNavController(v)
                    .navigate(R.id.settingsFragment);
        });
    }

    private static void resetIcons(ImageView icon, TextView label, int drawableId, Fragment fragment) {
        icon.setImageResource(drawableId);
        label.setTextColor(Color.parseColor("#6C7A89")); // gray
    }

    private static void highlight(ImageView icon, TextView label, int drawableId) {
        icon.setImageResource(drawableId);
        label.setTextColor(Color.parseColor("#000000")); // black
    }
}
