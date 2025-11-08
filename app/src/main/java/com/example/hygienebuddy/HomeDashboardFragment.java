package com.example.hygienebuddy;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeDashboardFragment extends Fragment {

    // Header
    private ImageView ivReminder;
    private TextView tvAppTitle;

    // Child profile
    private ImageView ivChildProfile;
    private TextView tvChildName, tvChildDetails;

    // Progress and points
    private ProgressBar progressTasks;
    private TextView tvTaskProgress, tvPoints;

    // Streak section
    private LinearLayout layoutStreakDays;
    private TextView tvCompletedTotal, tvLongestStreak, tvViewAllStreak;

    // Upcoming tasks section
    private LinearLayout layoutUpcomingTasks;

    // Today’s date
    private TextView tvTodayDate;

    // Containers for clickable navigation
    private LinearLayout layoutChildProfile, layoutTaskProgress, layoutPoints, layoutWeeklyStreak;

    // Track last loaded profile ID to detect profile changes
    private int lastLoadedProfileId = -1;

    // Database helper for app data
    private AppDataDatabaseHelper appDataDb;

    public HomeDashboardFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home_dashboard, container, false);
        appDataDb = new AppDataDatabaseHelper(requireContext());
        bindViews(view);
        setTodayDate();
        lastLoadedProfileId = -1;
        setupListeners(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.post(() -> BottomNavHelper.setupBottomNav(this, "home"));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDashboardData();
    }

    /** Refresh all dashboard data based on current profile */
    private void refreshDashboardData() {
        if (!isAdded() || appDataDb == null) return;
        int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
        if (currentProfileId == -1) {
            currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
        }

        boolean profileChanged = (currentProfileId != lastLoadedProfileId);
        boolean isFirstLoad = (lastLoadedProfileId == -1);

        if (profileChanged || isFirstLoad) {
            android.util.Log.d("HomeDashboard", "Profile " + (isFirstLoad ? "initialized" : "changed") + " from " + lastLoadedProfileId + " to " + currentProfileId);
            lastLoadedProfileId = currentProfileId;

            View rootView = getView();
            if (rootView != null && !isFirstLoad) {
                rootView.animate()
                        .alpha(0.3f)
                        .setDuration(150)
                        .withEndAction(() -> {
                            loadAllDashboardData();
                            rootView.animate()
                                    .alpha(1.0f)
                                    .setDuration(200)
                                    .start();
                        })
                        .start();
            } else {
                loadAllDashboardData();
            }
        } else {
            loadAllDashboardData();
        }
    }

    /** Load all dashboard data (called after animation if profile changed) */
    private void loadAllDashboardData() {
        if (!isAdded()) return;
        clearDashboardData();
        loadChildProfile();
        if (layoutUpcomingTasks != null) {
            loadUpcomingTasks();
        }
        loadTaskProgress();
        loadStreakData();
    }

    /** Clear all dashboard data to prevent showing stale data */
    private void clearDashboardData() {
        if (layoutUpcomingTasks != null) {
            layoutUpcomingTasks.removeAllViews();
        }

        if (layoutStreakDays != null) {
            layoutStreakDays.removeAllViews();
        }

        if (progressTasks != null) {
            progressTasks.setProgress(0);
        }
        if (tvTaskProgress != null) {
            tvTaskProgress.setText("");
        }
        if (tvPoints != null) {
            tvPoints.setText("");
        }
        if (tvCompletedTotal != null) {
            tvCompletedTotal.setText("");
        }
        if (tvLongestStreak != null) {
            tvLongestStreak.setText("");
        }
    }

    /** Bind all view IDs */
    private void bindViews(View view) {
        ivReminder = view.findViewById(R.id.ivReminder);
        tvAppTitle = view.findViewById(R.id.tvAppTitle);
        ivChildProfile = view.findViewById(R.id.ivChildProfile);
        tvChildName = view.findViewById(R.id.tvChildName);
        tvChildDetails = view.findViewById(R.id.tvChildDetails);
        progressTasks = view.findViewById(R.id.progressTasks);
        tvTaskProgress = view.findViewById(R.id.tvTaskProgress);
        tvPoints = view.findViewById(R.id.tvPoints);
        layoutStreakDays = view.findViewById(R.id.layoutStreakDays);
        tvCompletedTotal = view.findViewById(R.id.tvCompletedTotal);
        tvLongestStreak = view.findViewById(R.id.tvLongestStreak);
        tvViewAllStreak = view.findViewById(R.id.tvViewAllStreak);
        layoutUpcomingTasks = view.findViewById(R.id.layoutUpcomingTasks);
        tvTodayDate = view.findViewById(R.id.tvTodayDate);

        layoutChildProfile = view.findViewById(R.id.layoutChildProfile);
        layoutTaskProgress = view.findViewById(R.id.layoutTaskProgress);
        layoutPoints = view.findViewById(R.id.layoutPoints);
        layoutWeeklyStreak = view.findViewById(R.id.layoutWeeklyStreak);
    }

    /** Display today’s date dynamically */
    private void setTodayDate() {
        String today = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date());
        tvTodayDate.setText("TODAY - " + today);
    }

    /** Load task progress and points for current profile */
    private void loadTaskProgress() {
        if (progressTasks == null || tvTaskProgress == null || tvPoints == null || !isAdded()) {
            return;
        }

        try {
            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            }

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Set<String> completedTasks = appDataDb.getTaskCompletionsForDate(currentProfileId, today);

            int completedCount = 0;
            if (completedTasks.contains("handwashing")) completedCount++;
            if (completedTasks.contains("toothbrushing")) completedCount++;

            int totalTasks = 2; // 2 tasks per day: handwashing and toothbrushing

            int progressPercent = totalTasks > 0 ? (int) ((completedCount / (float) totalTasks) * 100) : 0;
            progressPercent = Math.min(progressPercent, 100); // Cap at 100%

            progressTasks.setProgress(progressPercent);
            tvTaskProgress.setText(progressPercent + "% completed");

            // Calculate total XP from badge progress (for points display) from SQLite
            int handwashingProgress = appDataDb.getBadgeProgress(currentProfileId, "handwashing_hero");
            int toothbrushingProgress = appDataDb.getBadgeProgress(currentProfileId, "toothbrushing_champ");
            int totalCompletedTasks = handwashingProgress + toothbrushingProgress;
            tvPoints.setText((totalCompletedTasks * 10) + " XP");

            android.util.Log.d("HomeDashboard", "Loaded task progress for profile ID: " + currentProfileId + " - Today: " + completedCount + "/" + totalTasks + " tasks completed (" + progressPercent + "%)");
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error loading task progress: " + e.getMessage(), e);

            if (progressTasks != null) progressTasks.setProgress(0);
            if (tvTaskProgress != null) tvTaskProgress.setText("0% completed");
            if (tvPoints != null) tvPoints.setText("0 XP");
        }
    }

    /** Mock: loads example progress and points (kept for backward compatibility) */
    private void loadMockTaskProgress() {
        loadTaskProgress();
    }

    /** Load streak data for current profile */
    private void loadStreakData() {
        if (layoutStreakDays == null || tvCompletedTotal == null || tvLongestStreak == null || !isAdded()) {
            return;
        }

        try {
            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            }

            layoutStreakDays.removeAllViews();

            Set<String> streakCompletedDays = appDataDb.getStreakDays(currentProfileId);

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            ArrayList<StreakDayInfo> streakDays = new ArrayList<>();

            for (int i = 6; i >= 0; i--) {
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, -i);
                String dateKey = dateFormat.format(cal.getTime());
                String dayName = dayNameFormat.format(cal.getTime());

                // Check if BOTH tasks were completed on this day
                boolean bothTasksCompleted = streakCompletedDays.contains(dateKey);

                streakDays.add(new StreakDayInfo(dayName, bothTasksCompleted));
            }

            int iconSize = (int) android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
            int horizontalMargin = (int) android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            int verticalMargin = (int) android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

            for (int i = 0; i < streakDays.size(); i++) {
                StreakDayInfo dayInfo = streakDays.get(i);

                LinearLayout dayContainer = new LinearLayout(getContext());
                dayContainer.setOrientation(LinearLayout.VERTICAL);
                dayContainer.setGravity(android.view.Gravity.CENTER);

                LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                );

                int leftMargin = (i == 0) ? 0 : horizontalMargin / 2;
                int rightMargin = (i == streakDays.size() - 1) ? 0 : horizontalMargin / 2;
                containerParams.setMargins(leftMargin, verticalMargin, rightMargin, verticalMargin);
                dayContainer.setLayoutParams(containerParams);

                TextView dayLabel = new TextView(getContext());
                dayLabel.setText(dayInfo.dayName);
                dayLabel.setTextSize(12);
                dayLabel.setGravity(android.view.Gravity.CENTER);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    dayLabel.setTextColor(getResources().getColor(R.color.subtitle_text, null));
                } else {
                    dayLabel.setTextColor(getResources().getColor(R.color.subtitle_text));
                }
                dayLabel.setPadding(0, 0, 0, 8);

                ImageView dayView = new ImageView(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(iconSize, iconSize);
                dayView.setLayoutParams(lp);
                dayView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                dayView.setImageResource(dayInfo.completed ? R.drawable.ic_streak_filled : R.drawable.ic_streak_empty);

                dayContainer.addView(dayLabel);
                dayContainer.addView(dayView);
                layoutStreakDays.addView(dayContainer);
            }

            int totalCompleted = streakCompletedDays.size();
            int longestStreak = calculateLongestStreak(streakCompletedDays);

            tvCompletedTotal.setText("Completed Total: " + totalCompleted + " day" + (totalCompleted != 1 ? "s" : ""));
            tvLongestStreak.setText("Longest Streak: " + longestStreak + " day" + (longestStreak != 1 ? "s" : ""));

            android.util.Log.d("HomeDashboard", "Loaded streak data for profile ID: " + currentProfileId + " - Total: " + totalCompleted + ", Longest: " + longestStreak);
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error loading streak data: " + e.getMessage(), e);
            tvCompletedTotal.setText("Completed Total: 0 days");
            tvLongestStreak.setText("Longest Streak: 0 days");
        }
    }

    /** Helper class for streak day information */
    private static class StreakDayInfo {
        String dayName;
        boolean completed;

        StreakDayInfo(String dayName, boolean completed) {
            this.dayName = dayName;
            this.completed = completed;
        }
    }

    /** Calculate longest consecutive streak from completed days */
    private int calculateLongestStreak(Set<String> completedDays) {
        if (completedDays == null || completedDays.isEmpty()) {
            return 0;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();

            List<String> sortedDates = new ArrayList<>(completedDays);
            sortedDates.sort(String::compareTo);

            if (sortedDates.isEmpty()) return 0;

            int longestStreak = 1;
            int currentStreak = 1;

            for (int i = 1; i < sortedDates.size(); i++) {
                cal.setTime(dateFormat.parse(sortedDates.get(i - 1)));
                cal.add(Calendar.DAY_OF_YEAR, 1);
                String expectedNextDate = dateFormat.format(cal.getTime());

                if (expectedNextDate.equals(sortedDates.get(i))) {
                    currentStreak++;
                    longestStreak = Math.max(longestStreak, currentStreak);
                } else {
                    currentStreak = 1;
                }
            }
            return longestStreak;
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error calculating streak: " + e.getMessage(), e);
            return 0;
        }
    }

    /** Mock: loads example streak icons and stats (kept for backward compatibility) */
    private void loadMockStreakData() {
        loadStreakData();
    }

    private void loadChildProfile() {
        if (tvChildName == null || tvChildDetails == null || !isAdded()) {
            return;
        }

        try {
            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            }

            UserProfileDatabaseHelper profileDb = new UserProfileDatabaseHelper(requireContext());
            UserProfile profile = null;
            if (currentProfileId > 0) {
                profile = profileDb.getProfileById(currentProfileId);
            }

            String name = null;
            String age = null;
            String conditions = null;
            String imageUri = null;
            if (profile != null) {
                name = profile.getName();
                age = String.valueOf(profile.getAge());
                conditions = profile.getCondition();
                imageUri = profile.getImageUri();
            }

            if (ivChildProfile != null) {
                ivChildProfile.setImageResource(R.drawable.ic_default_user);
                ivChildProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivChildProfile.setContentDescription("Child profile image");

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    ivChildProfile.setClipToOutline(true);
                    ivChildProfile.setOutlineProvider(new android.view.ViewOutlineProvider() {
                        @Override
                        public void getOutline(android.view.View view, android.graphics.Outline outline) {
                            outline.setOval(0, 0, view.getWidth(), view.getHeight());
                        }
                    });
                }

                if (imageUri != null && !imageUri.trim().isEmpty()) {
                    try {
                        Uri uri = null;
                        if (imageUri.startsWith("/")) {
                            File imageFile = new File(imageUri);
                            if (imageFile.exists()) {
                                try {
                                    uri = androidx.core.content.FileProvider.getUriForFile(
                                            requireContext(),
                                            requireContext().getPackageName() + ".fileprovider",
                                            imageFile
                                    );
                                } catch (Exception e) {
                                    uri = Uri.fromFile(imageFile);
                                }
                            }
                        } else if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
                            uri = Uri.parse(imageUri);
                        } else {
                            try {
                                ImageManager imageManager = new ImageManager(requireContext());
                                if (currentProfileId > 0) {
                                    Uri imageManagerUri = imageManager.getProfileImageUri(currentProfileId);
                                    if (imageManagerUri != null) {
                                        uri = imageManagerUri;
                                    }
                                }
                            } catch (Exception e) {
                                android.util.Log.w("HomeDashboard", "ImageManager not available: " + e.getMessage());
                            }
                        }

                        if (uri != null) {
                            ivChildProfile.setImageURI(uri);
                            ivChildProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            Uri finalUri = uri;
                            ivChildProfile.postDelayed(() -> {
                                try {
                                    if (ivChildProfile.getDrawable() == null) {
                                        ivChildProfile.setImageResource(R.drawable.ic_default_user);
                                        android.util.Log.w("HomeDashboard", "Profile image failed to load, using default");
                                    } else {
                                        android.util.Log.d("HomeDashboard", "Successfully loaded profile image: " + finalUri.toString());
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("HomeDashboard", "Error verifying image load: " + e.getMessage());
                                    ivChildProfile.setImageResource(R.drawable.ic_default_user);
                                }
                            }, 200);
                        } else {
                            ivChildProfile.setImageResource(R.drawable.ic_default_user);
                            android.util.Log.w("HomeDashboard", "Could not resolve image URI: " + imageUri);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("HomeDashboard", "Error parsing profile image URI: " + e.getMessage(), e);
                        ivChildProfile.setImageResource(R.drawable.ic_default_user);
                    }
                } else {
                    ivChildProfile.setImageResource(R.drawable.ic_default_user);
                }
            }

            if (name != null && age != null) {
                String formattedConditions = "";
                if (conditions != null && !conditions.trim().isEmpty()) {
                    String conditionsToProcess = conditions.trim();
                    if (conditionsToProcess.contains("/storage/") || conditionsToProcess.contains("content://") ||
                            conditionsToProcess.contains("file://") || conditionsToProcess.contains("Android/data")) {
                        android.util.Log.w("HomeDashboard", "Conditions field appears to contain image URI, treating as empty: " + conditionsToProcess.substring(0, Math.min(50, conditionsToProcess.length())));
                        formattedConditions = "No listed condition";
                    } else {
                        String[] conditionArray = conditionsToProcess.split(",");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < conditionArray.length; i++) {
                            String trimmed = conditionArray[i].trim();
                            if (!trimmed.isEmpty()) {
                                if (sb.length() > 0) {
                                    sb.append(", ");
                                }
                                sb.append(trimmed);
                            }
                        }
                        formattedConditions = sb.toString();
                        if (formattedConditions.isEmpty()) {
                            formattedConditions = "No listed condition";
                        }
                    }
                } else {
                    formattedConditions = "No listed condition";
                }

                // Ensure TextViews only show name and details, never image URI
                tvChildName.setText(name != null ? name : "No profile set");
                String detailsText = "Age " + age + " • " + formattedConditions;
                tvChildDetails.setText(detailsText);

                android.util.Log.d("HomeDashboard", "Displaying conditions: '" + formattedConditions + "' (raw: '" + conditions + "')");

                android.util.Log.d("HomeDashboard", "Loaded profile: " + name + " (ID: " + currentProfileId + ")");
            } else {
                tvChildName.setText("No profile set");
                tvChildDetails.setText("Please create a child profile to begin");
            }
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error loading child profile: " + e.getMessage(), e);
        }
    }


    /** Load real reminders from database and display as upcoming tasks */
    private void loadUpcomingTasks() {
        if (layoutUpcomingTasks == null || !isAdded() || getContext() == null) {
            return;
        }

        try {
            layoutUpcomingTasks.removeAllViews();

            ReminderDatabaseHelper reminderDbHelper = new ReminderDatabaseHelper(requireContext());

            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            }

            List<ReminderModel> reminders;
            if (currentProfileId > 0) {
                // Get reminders filtered by profile ID (ONLY reminders for this profile, excluding global)
                reminders = reminderDbHelper.getActiveRemindersByProfile(currentProfileId);

                List<ReminderModel> profileSpecificReminders = new ArrayList<>();
                for (ReminderModel reminder : reminders) {
                    if (reminder != null && reminder.getProfileId() != null && reminder.getProfileId() == currentProfileId) {
                        profileSpecificReminders.add(reminder);
                    }
                }
                reminders = profileSpecificReminders;

                android.util.Log.d("HomeDashboard", "Loaded reminders for profile ID: " + currentProfileId + ", found: " + reminders.size() + " profile-specific reminders");
            } else {
                reminders = reminderDbHelper.getActiveReminders();
                android.util.Log.d("HomeDashboard", "No profile selected, showing all active reminders: " + reminders.size());
            }

            if (reminders.isEmpty()) {
                TextView tvEmptyState = new TextView(requireContext());
                tvEmptyState.setText("No upcoming reminders");
                tvEmptyState.setTextSize(14);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    tvEmptyState.setTextColor(getResources().getColor(R.color.subtitle_text, null));
                } else {
                    tvEmptyState.setTextColor(getResources().getColor(R.color.subtitle_text));
                }
                tvEmptyState.setGravity(android.view.Gravity.CENTER);
                tvEmptyState.setPadding(20, 40, 20, 40);
                layoutUpcomingTasks.addView(tvEmptyState);
                return;
            }

            reminders.sort((r1, r2) -> {
                String time1 = r1.getTime() != null ? r1.getTime() : "";
                String time2 = r2.getTime() != null ? r2.getTime() : "";
                return time1.compareTo(time2);
            });

            int maxReminders = Math.min(reminders.size(), 5);
            for (int i = 0; i < maxReminders; i++) {
                ReminderModel reminder = reminders.get(i);
                if (reminder != null) {
                    LinearLayout taskCard = createReminderCard(reminder);
                    if (taskCard != null) {
                        layoutUpcomingTasks.addView(taskCard);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error loading reminders: " + e.getMessage(), e);
        }
    }

    private LinearLayout createReminderCard(ReminderModel reminder) {
        if (reminder == null || !isAdded() || getContext() == null) {
            return null;
        }

        try {
            Context context = requireContext();
            LinearLayout taskCard = new LinearLayout(context);
            taskCard.setOrientation(LinearLayout.VERTICAL);
            taskCard.setPadding(20, 16, 20, 16);
            taskCard.setBackgroundResource(R.drawable.rounded_card_light);

            TextView tvTaskName = new TextView(context);
            String taskName = reminder.getTaskName() != null ? reminder.getTaskName() : "Unknown Task";
            tvTaskName.setText(taskName);
            tvTaskName.setTextSize(16);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tvTaskName.setTextColor(getResources().getColor(R.color.black, null));
            } else {
                tvTaskName.setTextColor(getResources().getColor(R.color.black));
            }
            tvTaskName.setTypeface(null, Typeface.BOLD);

            String timeStr = reminder.getTime();
            String formattedTime = "Unknown time";
            if (timeStr != null && !timeStr.isEmpty()) {
                try {
                    String[] timeParts = timeStr.split(":");
                    if (timeParts.length >= 2) {
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.HOUR_OF_DAY, hour);
                        cal.set(Calendar.MINUTE, minute);
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        formattedTime = sdf.format(cal.getTime());
                    }
                } catch (Exception e) {
                    android.util.Log.e("HomeDashboard", "Error parsing time: " + timeStr, e);
                    formattedTime = timeStr;
                }
            }

            TextView tvTaskTime = new TextView(context);
            String frequencyText = formatFrequency(reminder.getFrequency());
            tvTaskTime.setText("Scheduled at " + formattedTime + " - " + frequencyText);
            tvTaskTime.setTextSize(14);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tvTaskTime.setTextColor(getResources().getColor(R.color.subtitle_text, null));
            } else {
                tvTaskTime.setTextColor(getResources().getColor(R.color.subtitle_text));
            }

            taskCard.addView(tvTaskName);
            taskCard.addView(tvTaskTime);

            taskCard.setOnClickListener(v -> navigateToFragment(new FragmentTasks()));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            taskCard.setLayoutParams(params);
            return taskCard;
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error creating reminder card: " + e.getMessage(), e);
            return null;
        }
    }

    private String formatFrequency(String frequency) {
        switch (frequency) {
            case "once":
                return "Once";
            case "daily":
                return "Daily";
            case "weekly":
                return "Weekly";
            case "custom":
                return "Custom";
            default:
                return frequency;
        }
    }

    /** Show reminder notifications dialog for the selected profile */
    private void showReminderNotificationsDialog() {
        if (!isAdded() || getContext() == null) {
            android.util.Log.e("HomeDashboard", "Cannot show reminder dialog - fragment not attached");
            return;
        }

        try {
            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            }

            String profileName = "Child";
            if (currentProfileId > 0) {
                UserProfileDatabaseHelper profileDb = new UserProfileDatabaseHelper(requireContext());
                UserProfile profile = profileDb.getProfileById(currentProfileId);
                if (profile != null && profile.getName() != null) {
                    profileName = profile.getName();
                } else {
                    profileName = appDataDb.getSetting("child_name", "Child");
                }
            } else {
                profileName = appDataDb.getSetting("child_name", "Child");
            }

            ReminderDatabaseHelper reminderDbHelper = new ReminderDatabaseHelper(requireContext());
            List<ReminderModel> reminders;

            if (currentProfileId > 0) {
                reminders = reminderDbHelper.getActiveRemindersByProfile(currentProfileId);
                List<ReminderModel> profileSpecificReminders = new ArrayList<>();
                for (ReminderModel reminder : reminders) {
                    if (reminder != null && reminder.getProfileId() != null && reminder.getProfileId() == currentProfileId) {
                        profileSpecificReminders.add(reminder);
                    }
                }
                reminders = profileSpecificReminders;
            } else {
                reminders = reminderDbHelper.getActiveReminders();
            }

            // Sort reminders by time
            reminders.sort((r1, r2) -> {
                String time1 = r1.getTime() != null ? r1.getTime() : "";
                String time2 = r2.getTime() != null ? r2.getTime() : "";
                return time1.compareTo(time2);
            });

            LinearLayout dialogContainer = new LinearLayout(requireContext());
            dialogContainer.setOrientation(LinearLayout.VERTICAL);
            dialogContainer.setPadding(24, 24, 24, 24);

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText("Reminder Notifications - " + profileName);
            tvTitle.setTextSize(18);
            tvTitle.setTypeface(null, Typeface.BOLD);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tvTitle.setTextColor(getResources().getColor(R.color.black, null));
            } else {
                tvTitle.setTextColor(getResources().getColor(R.color.black));
            }
            tvTitle.setPadding(0, 0, 0, 16);
            dialogContainer.addView(tvTitle);

            if (reminders.isEmpty()) {
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("No pending reminders for " + profileName);
                tvEmpty.setTextSize(14);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    tvEmpty.setTextColor(getResources().getColor(R.color.subtitle_text, null));
                } else {
                    tvEmpty.setTextColor(getResources().getColor(R.color.subtitle_text));
                }
                tvEmpty.setPadding(0, 16, 0, 16);
                dialogContainer.addView(tvEmpty);
            } else {
                // Add each reminder as a card
                for (ReminderModel reminder : reminders) {
                    if (reminder != null) {
                        View reminderItem = createReminderDialogItem(reminder);
                        if (reminderItem != null) {
                            dialogContainer.addView(reminderItem);
                        }
                    }
                }
            }

            // Create and show dialog
            com.google.android.material.dialog.MaterialAlertDialogBuilder dialogBuilder =
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setView(dialogContainer)
                            .setPositiveButton("Close", null)
                            .setNeutralButton("Manage Reminders", (dialog, which) -> {
                                // Navigate to Settings screen
                                navigateToFragment(new SettingsFragment());
                            });

            dialogBuilder.show();

        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error showing reminder notifications dialog: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading reminders", Toast.LENGTH_SHORT).show();
        }
    }

    /** Create a reminder item view for the dialog */
    private View createReminderDialogItem(ReminderModel reminder) {
        if (reminder == null || !isAdded() || getContext() == null) {
            return null;
        }

        try {
            Context context = requireContext();
            LinearLayout itemContainer = new LinearLayout(context);
            itemContainer.setOrientation(LinearLayout.VERTICAL);
            itemContainer.setPadding(16, 12, 16, 12);
            itemContainer.setBackgroundResource(R.drawable.rounded_card_light);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 12);
            itemContainer.setLayoutParams(params);

            // Task name
            TextView tvTaskName = new TextView(context);
            String taskName = reminder.getTaskName() != null ? reminder.getTaskName() : "Unknown Task";
            tvTaskName.setText(taskName);
            tvTaskName.setTextSize(16);
            tvTaskName.setTypeface(null, Typeface.BOLD);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tvTaskName.setTextColor(getResources().getColor(R.color.black, null));
            } else {
                tvTaskName.setTextColor(getResources().getColor(R.color.black));
            }
            tvTaskName.setPadding(0, 0, 0, 4);

            String timeStr = reminder.getTime();
            String formattedTime = "Unknown time";
            if (timeStr != null && !timeStr.isEmpty()) {
                try {
                    String[] timeParts = timeStr.split(":");
                    if (timeParts.length >= 2) {
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.HOUR_OF_DAY, hour);
                        cal.set(Calendar.MINUTE, minute);
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        formattedTime = sdf.format(cal.getTime());
                    }
                } catch (Exception e) {
                    android.util.Log.e("HomeDashboard", "Error parsing time: " + timeStr, e);
                    formattedTime = timeStr;
                }
            }

            // Time and frequency
            TextView tvDetails = new TextView(context);
            String frequencyText = formatFrequency(reminder.getFrequency());
            tvDetails.setText(formattedTime + " • " + frequencyText);
            tvDetails.setTextSize(14);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tvDetails.setTextColor(getResources().getColor(R.color.subtitle_text, null));
            } else {
                tvDetails.setTextColor(getResources().getColor(R.color.subtitle_text));
            }

            itemContainer.addView(tvTaskName);
            itemContainer.addView(tvDetails);

            return itemContainer;
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error creating reminder dialog item: " + e.getMessage(), e);
            return null;
        }
    }

    /** Handles navigation and user clicks */
    private void setupListeners(View view) {
        if (ivReminder != null) {
            ivReminder.setOnClickListener(v -> showReminderNotificationsDialog());
        }

        // Profile click - navigate to Manage Profile
        if (layoutChildProfile != null) {
            layoutChildProfile.setOnClickListener(v -> navigateToManageProfile());
        }

        if (layoutTaskProgress != null) {
            layoutTaskProgress.setOnClickListener(v -> navigateToFragment(new FragmentReportSummary()));
        }
        if (layoutPoints != null) {
            layoutPoints.setOnClickListener(v -> navigateToFragment(new SettingsFragment()));
        }
        if (layoutWeeklyStreak != null) {
            layoutWeeklyStreak.setOnClickListener(v -> navigateToFragment(new FragmentTasks()));
        }

        if (tvViewAllStreak != null) {
            tvViewAllStreak.setOnClickListener(v -> navigateToReportSummary());
        }
    }

    /** Navigate to ManageProfileFragment */
    private void navigateToManageProfile() {
        if (!isAdded() || getActivity() == null) {
            android.util.Log.e("HomeDashboard", "Cannot navigate - fragment not added or activity is null");
            return;
        }

        try {
            boolean navControllerSuccess = false;

            try {
                FragmentActivity activity = requireActivity();
                FragmentManager fm = activity.getSupportFragmentManager();
                Fragment navHostFragment = fm.findFragmentById(R.id.nav_host_fragment);

                if (navHostFragment instanceof NavHostFragment) {
                    NavController navController = ((NavHostFragment) navHostFragment).getNavController();
                    NavDestination currentDest = navController.getCurrentDestination();

                    if (currentDest != null) {
                        int currentDestId = currentDest.getId();
                        int actionId = -1;

                        if (currentDestId == R.id.homeDashboardFragment) {
                            actionId = R.id.action_homeDashboardFragment_to_manageProfileFragment;
                        } else if (currentDestId == R.id.fragmentTasks) {
                            actionId = R.id.action_fragmentTasks_to_manageProfileFragment;
                        } else if (currentDestId == R.id.fragmentReportSummary) {
                            actionId = R.id.action_fragmentReportSummary_to_manageProfileFragment;
                        } else if (currentDestId == R.id.settingsFragment) {
                            actionId = R.id.action_settingsFragment_to_manageProfileFragment;
                        } else if (currentDestId == R.id.fragmentBadges) {
                            actionId = R.id.action_fragmentBadges_to_manageProfileFragment;
                        }

                        if (actionId != -1) {
                            navController.navigate(actionId);
                            android.util.Log.d("HomeDashboard", "Successfully navigated to ManageProfile using action: " + actionId);
                            navControllerSuccess = true;
                        } else {
                            navController.navigate(R.id.manageProfileFragment);
                            android.util.Log.d("HomeDashboard", "Successfully navigated to ManageProfile by direct ID");
                            navControllerSuccess = true;
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.w("HomeDashboard", "NavController navigation failed: " + e.getMessage(), e);
            }

            if (navControllerSuccess) {
                return;
            }

            android.util.Log.d("HomeDashboard", "Using FragmentTransaction fallback");
            ManageProfileFragment manageProfileFragment = new ManageProfileFragment();
            FragmentActivity activity = requireActivity();
            FragmentManager fm = activity.getSupportFragmentManager();

            FragmentTransaction transaction = fm.beginTransaction();
            transaction.replace(R.id.nav_host_fragment, manageProfileFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            android.util.Log.d("HomeDashboard", "FragmentTransaction completed successfully");

        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error navigating to ManageProfile: " + e.getMessage(), e);
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to open profile management", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Navigate to Report Summary using safe navigation approach */
    private void navigateToReportSummary() {
        if (!isAdded()) {
            android.util.Log.e("HomeDashboard", "Cannot navigate - fragment not added");
            return;
        }

        try {
            FragmentActivity activity = requireActivity();

            View view = getView();
            NavController navController = null;

            if (view != null) {
                try {
                    navController = Navigation.findNavController(view);
                    android.util.Log.d("HomeDashboard", "Found NavController from fragment view for Report Summary");
                } catch (IllegalStateException e) {
                    android.util.Log.w("HomeDashboard", "NavController not found from fragment view: " + e.getMessage());
                }
            }

            if (navController == null) {
                try {
                    NavHostFragment navHostFragment = (NavHostFragment) activity.getSupportFragmentManager()
                            .findFragmentById(R.id.nav_host_fragment);
                    if (navHostFragment != null) {
                        navController = navHostFragment.getNavController();
                        android.util.Log.d("HomeDashboard", "Found NavController from NavHostFragment for Report Summary");
                    }
                } catch (Exception e) {
                    android.util.Log.w("HomeDashboard", "Could not get NavController from NavHostFragment: " + e.getMessage());
                }
            }

            if (navController != null) {
                try {
                    navController.navigate(R.id.action_homeDashboardFragment_to_fragmentReportSummary);
                    android.util.Log.d("HomeDashboard", "Navigated to Report Summary using NavController");
                    return;
                } catch (Exception e) {
                    android.util.Log.e("HomeDashboard", "NavController.navigate() failed for Report Summary: " + e.getMessage(), e);
                }
            }

            View fragmentContainer = activity.findViewById(R.id.fragment_container);
            if (fragmentContainer != null) {
                FragmentManager fm = activity.getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                ft.replace(R.id.fragment_container, new FragmentReportSummary());
                ft.addToBackStack(null);
                ft.commit();
                android.util.Log.d("HomeDashboard", "Navigated to Report Summary using fragment_container");
                return;
            }

            android.util.Log.e("HomeDashboard", "All navigation methods failed for Report Summary");
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Failed to open Report Summary. Please try again.", Toast.LENGTH_SHORT).show();
            }

        } catch (IllegalArgumentException e) {
            android.util.Log.e("HomeDashboard", "IllegalArgumentException navigating to Report Summary: " + e.getMessage(), e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (ClassCastException e) {
            android.util.Log.e("HomeDashboard", "ClassCastException navigating to Report Summary: " + e.getMessage(), e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Navigation error: Invalid view type", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error navigating to Report Summary: " + e.getMessage(), e);
            e.printStackTrace();
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Failed to open Report Summary", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Navigate to fragment using NavController */
    private void navigateToFragment(Fragment fragment) {
        if (fragment == null) {
            android.util.Log.e("HomeDashboard", "Cannot navigate - fragment is null");
            return;
        }

        if (!isAdded()) {
            android.util.Log.e("HomeDashboard", "Cannot navigate - fragment not added");
            return;
        }

        View view = getView();
        if (view == null) {
            android.util.Log.e("HomeDashboard", "Cannot navigate - view is null, will retry");
            if (getView() != null) {
                getView().post(() -> navigateToFragment(fragment));
            }
            return;
        }

        try {
            NavController navController = Navigation.findNavController(view);

            // Use navigation graph actions
            if (fragment instanceof ManageProfileFragment) {
                navController.navigate(R.id.action_homeDashboardFragment_to_manageProfileFragment);
            } else if (fragment instanceof FragmentTasks) {
                navController.navigate(R.id.fragmentTasks);
            } else if (fragment instanceof FragmentReportSummary) {
                navController.navigate(R.id.action_homeDashboardFragment_to_fragmentReportSummary);
            } else if (fragment instanceof SettingsFragment) {
                navController.navigate(R.id.settingsFragment);
            } else {
                android.util.Log.w("HomeDashboard", "Unknown fragment type for navigation: " + fragment.getClass().getSimpleName());
            }
        } catch (IllegalArgumentException e) {
            android.util.Log.e("HomeDashboard", "NavController not found. Fragment may not be in NavHostFragment: " + e.getMessage());
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Navigation not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Navigation failed: " + e.getMessage(), e);
            e.printStackTrace();
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Navigation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

}

