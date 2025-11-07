package com.example.hygienebuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FragmentReportSummary extends Fragment {

    private ImageView btnBack, ivUserAvatar, btnPrevMonth, btnNextMonth;
    private TextView tvUserName, tvUserAge, tvUserConditions, tvCurrentMonth,
            tvTotalPoints, tvBadgesEarned, tvTaskCompletion;
    private MaterialButtonToggleGroup toggleDateRange;
    private MaterialButton btnWeek, btnMonth, btnDownloadReport;
    private GridLayout gridCalendar;

    private Calendar calendar;
    private boolean isWeekView = false;
    private int lastLoadedProfileId = -1;

    @SuppressLint("WrongViewCast")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_summary, container, false);

        // Initialize views
        btnBack = view.findViewById(R.id.btnBack);
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserAge = view.findViewById(R.id.tvUserAge);
        tvUserConditions = view.findViewById(R.id.tvUserConditions);
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        tvTotalPoints = view.findViewById(R.id.tvTotalPoints);
        tvBadgesEarned = view.findViewById(R.id.tvBadgesEarned);
        toggleDateRange = view.findViewById(R.id.toggleDateRange);
        btnWeek = view.findViewById(R.id.btnWeek);
        btnMonth = view.findViewById(R.id.btnMonth);
        btnDownloadReport = view.findViewById(R.id.btnDownloadReport);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        gridCalendar = view.findViewById(R.id.gridCalendar);

        // Try to find task completion TextView (may not exist in layout)
        try {
            tvTaskCompletion = view.findViewById(R.id.tvTaskCompletion);
        } catch (Exception e) {
            // TextView may not exist, we'll add it programmatically if needed
        }

        // Initialize calendar
        calendar = Calendar.getInstance();

        // Load initial data
        loadProfileData();
        loadSummaryData();
        updateCalendarView();

        // Back button
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Date range toggle
        toggleDateRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnWeek) {
                isWeekView = true;
                updateCalendarView();
            } else if (checkedId == R.id.btnMonth) {
                isWeekView = false;
                updateCalendarView();
            }
        });

        // Set default to month view
        btnMonth.setChecked(true);

        // Month/Week navigation
        btnPrevMonth.setOnClickListener(v -> {
            if (isWeekView) {
                calendar.add(Calendar.WEEK_OF_YEAR, -1);
            } else {
                calendar.add(Calendar.MONTH, -1);
            }
            updateCalendarView();
        });

        btnNextMonth.setOnClickListener(v -> {
            if (isWeekView) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                calendar.add(Calendar.MONTH, 1);
            }
            updateCalendarView();
        });

        // Download report
        btnDownloadReport.setOnClickListener(v -> exportReportToCSV());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when resuming (profile may have changed)
        loadProfileData();
        loadSummaryData();
        updateCalendarView();
    }

    /** Load profile data for currently selected profile */
    private void loadProfileData() {
        if (!isAdded() || getContext() == null) return;

        try {
            // Get current profile ID from SQLite
            AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            }

            // Check if profile changed
            if (currentProfileId != lastLoadedProfileId) {
                lastLoadedProfileId = currentProfileId;
            }

            if (currentProfileId > 0) {
                // Load profile from database
                UserProfileDatabaseHelper dbHelper = new UserProfileDatabaseHelper(requireContext());
                UserProfile profile = dbHelper.getProfileById(currentProfileId);

                if (profile != null) {
                    tvUserName.setText(profile.getName());
                    tvUserAge.setText("Age " + profile.getAge());
                    String conditions = profile.getCondition();
                    if (conditions != null && !conditions.trim().isEmpty()) {
                        tvUserConditions.setText(conditions);
                    } else {
                        tvUserConditions.setText("No conditions specified");
                    }

                    // Load profile image
                    if (profile.hasImage() && profile.getImageUri() != null) {
                        try {
                            ivUserAvatar.setImageURI(Uri.parse(profile.getImageUri()));
                        } catch (Exception e) {
                            ivUserAvatar.setImageResource(R.drawable.default_avatar);
                        }
                    } else {
                        ivUserAvatar.setImageResource(R.drawable.default_avatar);
                    }
                } else {
                    // Fallback to SQLite app settings
                    String name = appDataDb.getSetting("child_name", "No profile");
                    String age = appDataDb.getSetting("child_age", "");
                    String conditions = appDataDb.getSetting("child_conditions", "");

                    tvUserName.setText(name);
                    tvUserAge.setText(age != null && !age.isEmpty() ? "Age " + age : "");
                    tvUserConditions.setText(conditions != null && !conditions.trim().isEmpty() ? conditions : "No conditions specified");
                    ivUserAvatar.setImageResource(R.drawable.default_avatar);
                }
            } else {
                // No profile selected
                tvUserName.setText("No profile selected");
                tvUserAge.setText("");
                tvUserConditions.setText("Please select a profile");
                ivUserAvatar.setImageResource(R.drawable.default_avatar);
            }
        } catch (Exception e) {
            android.util.Log.e("ReportSummary", "Error loading profile data: " + e.getMessage(), e);
        }
    }

    /** Load summary data (points, badges, task completion) */
    private void loadSummaryData() {
        if (!isAdded() || getContext() == null) return;

        try {
            // Get current profile ID from SQLite
            AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            }

            if (currentProfileId <= 0) {
                tvTotalPoints.setText("0 XP");
                tvBadgesEarned.setText("0");
                if (tvTaskCompletion != null) {
                    tvTaskCompletion.setText("0%");
                }
                return;
            }

            // Calculate total points from badge progress (from SQLite)
            int handwashingProgress = appDataDb.getBadgeProgress(currentProfileId, "handwashing_hero");
            int toothbrushingProgress = appDataDb.getBadgeProgress(currentProfileId, "toothbrushing_champ");
            int totalCompletedTasks = handwashingProgress + toothbrushingProgress; // Calculate task completion percentage based on actual days and 2 tasks per day
            int totalPoints = totalCompletedTasks * 10; // 10 XP per task
            tvTotalPoints.setText(totalPoints + " XP");

            // Count earned badges (from SQLite)
            BadgeRepository badgeRepository = new BadgeRepository(requireContext());
            List<BadgeModel> allBadges = badgeRepository.getAllBadges();
            int earnedBadgesCount = 0;
            for (BadgeModel badge : allBadges) {
                String badgeKey = badge.getImageKey();
                boolean isUnlocked = appDataDb.isBadgeUnlocked(currentProfileId, badgeKey);
                if (isUnlocked) {
                    earnedBadgesCount++;
                }
            }
            tvBadgesEarned.setText(String.valueOf(earnedBadgesCount));

            // Get earliest task completion date to calculate total possible tasks
            String earliestDate = appDataDb.getEarliestTaskDate(currentProfileId);
            int totalPossibleTasks = 0;

            if (earliestDate != null && !earliestDate.isEmpty()) {
                try {
                    // Calculate days from earliest task date to today
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Calendar earliestCal = Calendar.getInstance();
                    earliestCal.setTime(dateFormat.parse(earliestDate));

                    Calendar todayCal = Calendar.getInstance();
                    todayCal.set(Calendar.HOUR_OF_DAY, 0);
                    todayCal.set(Calendar.MINUTE, 0);
                    todayCal.set(Calendar.SECOND, 0);
                    todayCal.set(Calendar.MILLISECOND, 0);

                    long diffInMillis = todayCal.getTimeInMillis() - earliestCal.getTimeInMillis();
                    long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

                    // Total possible tasks = (days + 1) * 2 (since there are 2 tasks per day: handwashing and toothbrushing)
                    // +1 because we include both the earliest day and today
                    totalPossibleTasks = (int) (diffInDays + 1) * 2;
                } catch (Exception e) {
                    android.util.Log.e("ReportSummary", "Error calculating days: " + e.getMessage(), e);
                    // Fallback: if we can't parse the date, use a reasonable default
                    totalPossibleTasks = Math.max(totalCompletedTasks, 40); // At least show current progress
                }
            } else {
                // No tasks completed yet - use a default or show 0%
                totalPossibleTasks = Math.max(totalCompletedTasks, 1); // Avoid division by zero
            }

            // Calculate percentage: (completed / possible) * 100
            int completionPercent = totalPossibleTasks > 0 ?
                    Math.min((int) ((totalCompletedTasks / (float) totalPossibleTasks) * 100), 100) : 0;

            if (tvTaskCompletion != null) {
                tvTaskCompletion.setText(completionPercent + "%");
            }

            android.util.Log.d("ReportSummary", "Loaded summary for profile ID: " + currentProfileId +
                    " - Points: " + totalPoints + ", Badges: " + earnedBadgesCount + ", Completion: " + completionPercent + "%");
        } catch (Exception e) {
            android.util.Log.e("ReportSummary", "Error loading summary data: " + e.getMessage(), e);
            tvTotalPoints.setText("0 XP");
            tvBadgesEarned.setText("0");
            if (tvTaskCompletion != null) {
                tvTaskCompletion.setText("0%");
            }
        }
    }

    /** Updates the calendar grid dynamically */
    private void updateCalendarView() {
        if (gridCalendar == null || !isAdded()) return;

        gridCalendar.removeAllViews();

        if (isWeekView) {
            updateWeekView();
        } else {
            updateMonthView();
        }
    }

    /** Update calendar to show week view */
    private void updateWeekView() {
        SimpleDateFormat weekFormat = new SimpleDateFormat("MMM d - MMM d, yyyy", Locale.getDefault());
        Calendar startOfWeek = (Calendar) calendar.clone();
        startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.getFirstDayOfWeek());
        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_WEEK, 6);
        tvCurrentMonth.setText(weekFormat.format(startOfWeek.getTime()) + " - " + weekFormat.format(endOfWeek.getTime()));

        // Add day headers
        String[] dayHeaders = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String header : dayHeaders) {
            TextView headerView = new TextView(getContext());
            headerView.setText(header);
            headerView.setGravity(android.view.Gravity.CENTER);
            headerView.setTextSize(12f);
            headerView.setPadding(8, 8, 8, 8);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                headerView.setTextColor(getResources().getColor(R.color.subtitle_text, null));
            } else {
                headerView.setTextColor(getResources().getColor(R.color.subtitle_text));
            }
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            headerView.setLayoutParams(params);
            gridCalendar.addView(headerView);
        }

        // Get streak days for current profile
        Set<String> streakDays = getStreakDays();

        // Add days of the week
        Calendar tempCal = (Calendar) startOfWeek.clone();
        for (int i = 0; i < 7; i++) {
            int day = tempCal.get(Calendar.DAY_OF_MONTH);
            String dateKey = formatDateKey(tempCal);
            boolean isStreakDay = streakDays.contains(dateKey);
            addCalendarDay(day, tempCal, isStreakDay);
            tempCal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    /** Update calendar to show month view */
    private void updateMonthView() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCurrentMonth.setText(monthFormat.format(calendar.getTime()));

        // Add day headers
        String[] dayHeaders = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String header : dayHeaders) {
            TextView headerView = new TextView(getContext());
            headerView.setText(header);
            headerView.setGravity(android.view.Gravity.CENTER);
            headerView.setTextSize(12f);
            headerView.setPadding(8, 8, 8, 8);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                headerView.setTextColor(getResources().getColor(R.color.subtitle_text, null));
            } else {
                headerView.setTextColor(getResources().getColor(R.color.subtitle_text));
            }
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            headerView.setLayoutParams(params);
            gridCalendar.addView(headerView);
        }

        Calendar tempCal = (Calendar) calendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1; // 0-based
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Get streak days for current profile
        Set<String> streakDays = getStreakDays();

        // Add empty cells for days before month starts
        for (int i = 0; i < firstDayOfWeek; i++) {
            addEmptyDay();
        }

        // Add days of the month
        for (int day = 1; day <= daysInMonth; day++) {
            tempCal.set(Calendar.DAY_OF_MONTH, day);
            String dateKey = formatDateKey(tempCal);
            boolean isStreakDay = streakDays.contains(dateKey);
            addCalendarDay(day, tempCal, isStreakDay);
        }
    }

    /** Get streak days for current profile */
    private Set<String> getStreakDays() {
        Set<String> streakDays = new HashSet<>();
        if (!isAdded() || getContext() == null) return streakDays;

        try {
            // Get current profile ID from SQLite
            AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            }

            if (currentProfileId > 0) {
                // Get streak days from SQLite
                streakDays = appDataDb.getStreakDays(currentProfileId);
            }
        } catch (Exception e) {
            android.util.Log.e("ReportSummary", "Error getting streak days: " + e.getMessage(), e);
        }

        return streakDays;
    }

    /** Format date as yyyy-MM-dd key */
    private String formatDateKey(Calendar cal) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(cal.getTime());
    }

    private void addEmptyDay() {
        View empty = new View(getContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        empty.setLayoutParams(params);
        gridCalendar.addView(empty);
    }

    private void addCalendarDay(int day, Calendar dayCalendar, boolean isStreakDay) {
        TextView tvDay = new TextView(getContext());
        tvDay.setText(String.valueOf(day));
        tvDay.setGravity(android.view.Gravity.CENTER);
        tvDay.setPadding(16, 16, 16, 16);
        tvDay.setTextSize(14f);

        Calendar today = Calendar.getInstance();
        boolean isToday = (day == today.get(Calendar.DAY_OF_MONTH)
                && dayCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && dayCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR));

        if (isToday) {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_day_today);
            tvDay.setTextColor(getResources().getColor(android.R.color.white));
        } else if (isStreakDay) {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_day_completed);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tvDay.setTextColor(getResources().getColor(R.color.black, null));
            } else {
                tvDay.setTextColor(getResources().getColor(R.color.black));
            }
        } else {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_day_normal);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tvDay.setTextColor(getResources().getColor(R.color.black, null));
            } else {
                tvDay.setTextColor(getResources().getColor(R.color.black));
            }
        }

        tvDay.setOnClickListener(v -> {
            String dateStr = formatDateKey(dayCalendar);
            Toast.makeText(getContext(), "Date: " + dateStr + (isStreakDay ? " (Streak achieved!)" : ""), Toast.LENGTH_SHORT).show();
        });

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        tvDay.setLayoutParams(params);

        gridCalendar.addView(tvDay);
    }

    /** Export report data to CSV file */
    private void exportReportToCSV() {
        if (!isAdded() || getContext() == null) return;

        try {
            // Get current profile ID from SQLite
            AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            }

            if (currentProfileId <= 0) {
                Toast.makeText(getContext(), "No profile selected. Please select a profile first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get profile data
            UserProfileDatabaseHelper dbHelper = new UserProfileDatabaseHelper(requireContext());
            UserProfile profile = dbHelper.getProfileById(currentProfileId);
            String profileName = profile != null ? profile.getName() : "Unknown";

            // Get summary data from SQLite
            int handwashingProgress = appDataDb.getBadgeProgress(currentProfileId, "handwashing_hero");
            int toothbrushingProgress = appDataDb.getBadgeProgress(currentProfileId, "toothbrushing_champ");
            int totalCompletedTasks = handwashingProgress + toothbrushingProgress;
            int totalPoints = totalCompletedTasks * 10;

            // Count earned badges from SQLite
            BadgeRepository badgeRepository = new BadgeRepository(requireContext());
            List<BadgeModel> allBadges = badgeRepository.getAllBadges();
            List<String> earnedBadges = new ArrayList<>();
            for (BadgeModel badge : allBadges) {
                String badgeKey = badge.getImageKey();
                boolean isUnlocked = appDataDb.isBadgeUnlocked(currentProfileId, badgeKey);
                if (isUnlocked) {
                    String earnedDate = appDataDb.getBadgeEarnedDate(currentProfileId, badgeKey);
                    if (earnedDate == null) earnedDate = "N/A";
                    earnedBadges.add(badge.getTitle() + " (" + earnedDate + ")");
                }
            }

            // Get streak days
            Set<String> streakDays = getStreakDays();
            List<String> sortedStreakDays = new ArrayList<>(streakDays);
            sortedStreakDays.sort(String::compareTo);

            // Create CSV content
            StringBuilder csv = new StringBuilder();
            csv.append("Hygiene Buddy - Report Summary\n");
            csv.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
            csv.append("Profile Information\n");
            csv.append("Name,").append(profileName).append("\n");
            if (profile != null) {
                csv.append("Age,").append(profile.getAge()).append("\n");
                csv.append("Conditions,").append(profile.getCondition() != null ? profile.getCondition() : "None").append("\n");
            }
            csv.append("\n");

            csv.append("Summary Statistics\n");
            csv.append("Total Points,").append(totalPoints).append("\n");
            csv.append("Total Badges Earned,").append(earnedBadges.size()).append("\n");
            csv.append("Handwashing Tasks Completed,").append(handwashingProgress).append("\n");
            csv.append("Toothbrushing Tasks Completed,").append(toothbrushingProgress).append("\n");
            csv.append("Total Tasks Completed,").append(totalCompletedTasks).append("\n");
            csv.append("\n");

            csv.append("Earned Badges\n");
            csv.append("Badge Name,Earned Date\n");
            for (String badge : earnedBadges) {
                String[] parts = badge.split(" \\(");
                if (parts.length == 2) {
                    csv.append("\"").append(parts[0]).append("\",\"").append(parts[1].replace(")", "")).append("\"\n");
                } else {
                    csv.append("\"").append(badge).append("\",\"N/A\"\n");
                }
            }
            csv.append("\n");

            csv.append("Streak Days (Days with both tasks completed)\n");
            csv.append("Date\n");
            for (String date : sortedStreakDays) {
                csv.append(date).append("\n");
            }

            // Save CSV file
            String fileName = "HygieneBuddy_Report_" + profileName.replace(" ", "_") + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";

            File csvFile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+): Use app-specific directory (no permission needed)
                File appDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (appDir == null) {
                    appDir = requireContext().getFilesDir();
                }
                csvFile = new File(appDir, fileName);
            } else {
                // Android 9 and below: Use public Downloads directory
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                csvFile = new File(downloadsDir, fileName);
            }

            FileWriter writer = new FileWriter(csvFile);
            writer.write(csv.toString());
            writer.close();

            String message = "Report exported: " + fileName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                message += "\nSaved to: " + csvFile.getParent();
            } else {
                message += "\nSaved to Downloads folder";
            }
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            android.util.Log.d("ReportSummary", "CSV exported to: " + csvFile.getAbsolutePath());

        } catch (IOException e) {
            android.util.Log.e("ReportSummary", "Error exporting CSV: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error exporting report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("ReportSummary", "Error exporting CSV: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error exporting report. Please check permissions.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ✅ Ensures navbar highlights correctly
        view.post(() -> BottomNavHelper.setupBottomNav(this, "report"));
    }
}
