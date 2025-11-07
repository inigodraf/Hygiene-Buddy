package com.example.hygienebuddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;

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
    private TextView tvCompletedTotal, tvLongestStreak;

    // Upcoming tasks section
    private LinearLayout layoutUpcomingTasks;

    // Today’s date
    private TextView tvTodayDate;

    // Containers for clickable navigation
    private LinearLayout layoutChildProfile, layoutTaskProgress, layoutPoints, layoutWeeklyStreak;

    // Track last loaded profile ID to detect profile changes
    private int lastLoadedProfileId = -1;

    public HomeDashboardFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home_dashboard, container, false);

        // Bind UI elements
        bindViews(view);

        // Set up UI with placeholder (mock) data
        setTodayDate();

        // Initialize last loaded profile ID to -1 to force refresh on first load
        // This ensures profile change detection works correctly
        lastLoadedProfileId = -1;

        // Load all dashboard data (will detect profile change if needed)
        // Don't call loadAllDashboardData() directly - let onResume() handle it via refreshDashboardData()

        // Handle clicks & interactivity
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
        // Refresh all data when returning to the dashboard (profile may have changed)
        refreshDashboardData();
    }

    /** Refresh all dashboard data based on current profile */
    private void refreshDashboardData() {
        if (!isAdded()) return;

        // Get current profile ID
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
        int currentProfileId = sharedPref.getInt("current_profile_id", -1);
        if (currentProfileId == -1) {
            currentProfileId = sharedPref.getInt("selected_profile_id", -1);
        }

        // Check if profile has changed (or if this is first load)
        boolean profileChanged = (currentProfileId != lastLoadedProfileId);
        boolean isFirstLoad = (lastLoadedProfileId == -1);

        if (profileChanged || isFirstLoad) {
            android.util.Log.d("HomeDashboard", "Profile " + (isFirstLoad ? "initialized" : "changed") + " from " + lastLoadedProfileId + " to " + currentProfileId);
            lastLoadedProfileId = currentProfileId;

            // Add fade animation when profile changes (skip on first load for faster initial display)
            View rootView = getView();
            if (rootView != null && !isFirstLoad) {
                // Fade animation for profile changes
                rootView.animate()
                        .alpha(0.3f)
                        .setDuration(150)
                        .withEndAction(() -> {
                            // Load all data
                            loadAllDashboardData();
                            // Fade back in
                            rootView.animate()
                                    .alpha(1.0f)
                                    .setDuration(200)
                                    .start();
                        })
                        .start();
            } else {
                // If view not ready or first load, load data without animation
                loadAllDashboardData();
            }
        } else {
            // Profile hasn't changed, just refresh normally
            loadAllDashboardData();
        }
    }

    /** Load all dashboard data (called after animation if profile changed) */
    private void loadAllDashboardData() {
        if (!isAdded()) return;

        // Clear all existing data first to prevent showing old data
        clearDashboardData();

        // Refresh profile display
        loadChildProfile();

        // Refresh reminders (filtered by current profile)
        if (layoutUpcomingTasks != null) {
            loadUpcomingTasks();
        }

        // Refresh task progress (profile-scoped)
        loadTaskProgress();

        // Refresh streak data (profile-scoped)
        loadStreakData();
    }

    /** Clear all dashboard data to prevent showing stale data */
    private void clearDashboardData() {
        // Clear reminders
        if (layoutUpcomingTasks != null) {
            layoutUpcomingTasks.removeAllViews();
        }

        // Clear streak days
        if (layoutStreakDays != null) {
            layoutStreakDays.removeAllViews();
        }

        // Reset progress indicators
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
            // Get current profile ID
            SharedPreferences sharedPref = requireActivity().getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
            int currentProfileId = sharedPref.getInt("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = sharedPref.getInt("selected_profile_id", -1);
            }

            // Load task progress from profile-scoped SharedPreferences
            // Get progress for task-related badges (handwashing_hero, toothbrushing_champ)
            String handwashingProgressKey = "badge_progress_handwashing_hero_profile_" + currentProfileId;
            String toothbrushingProgressKey = "badge_progress_toothbrushing_champ_profile_" + currentProfileId;

            int handwashingProgress = sharedPref.getInt(handwashingProgressKey, 0);
            int toothbrushingProgress = sharedPref.getInt(toothbrushingProgressKey, 0);

            int completedTasks = handwashingProgress + toothbrushingProgress;
            int totalTasks = 20; // 10 handwashing + 10 toothbrushing (goal for each badge)

            // Calculate progress percentage
            int progressPercent = totalTasks > 0 ? (int) ((completedTasks / (float) totalTasks) * 100) : 0;
            progressPercent = Math.min(progressPercent, 100); // Cap at 100%

            progressTasks.setProgress(progressPercent);
            tvTaskProgress.setText(progressPercent + "% completed");
            tvPoints.setText((completedTasks * 10) + " XP");

            android.util.Log.d("HomeDashboard", "Loaded task progress for profile ID: " + currentProfileId + " - " + completedTasks + "/" + totalTasks + " tasks (H:" + handwashingProgress + ", T:" + toothbrushingProgress + ")");
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error loading task progress: " + e.getMessage(), e);
            // Fallback to zero if error
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
            // Get current profile ID
            SharedPreferences sharedPref = requireActivity().getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
            int currentProfileId = sharedPref.getInt("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = sharedPref.getInt("selected_profile_id", -1);
            }

            layoutStreakDays.removeAllViews();

            // Load completed days from SharedPreferences (profile-scoped)
            String completedDaysKey = "completed_days_profile_" + currentProfileId;
            String completedDaysStr = sharedPref.getString(completedDaysKey, "");
            Set<String> completedDays = new HashSet<>();
            if (!completedDaysStr.isEmpty()) {
                String[] days = completedDaysStr.split(",");
                for (String day : days) {
                    if (!day.trim().isEmpty()) {
                        completedDays.add(day.trim());
                    }
                }
            }

            // Get last 7 days for weekly streak display
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            ArrayList<Boolean> streakDays = new ArrayList<>();

            for (int i = 6; i >= 0; i--) {
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, -i);
                String dateKey = dateFormat.format(cal.getTime());
                boolean completed = completedDays.contains(dateKey);
                streakDays.add(completed);
            }

            // Display streak days
            for (boolean completed : streakDays) {
                ImageView dayView = new ImageView(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(70, 70);
                lp.setMargins(8, 8, 8, 8);
                dayView.setLayoutParams(lp);
                dayView.setPadding(8, 8, 8, 8);
                dayView.setImageResource(completed ? R.drawable.ic_streak_filled : R.drawable.ic_streak_empty);
                layoutStreakDays.addView(dayView);
            }

            // Calculate total completed days and longest streak
            int totalCompleted = completedDays.size();
            int longestStreak = calculateLongestStreak(completedDays);

            tvCompletedTotal.setText("Completed Total: " + totalCompleted + " day" + (totalCompleted != 1 ? "s" : ""));
            tvLongestStreak.setText("Longest Streak: " + longestStreak + " day" + (longestStreak != 1 ? "s" : ""));

            android.util.Log.d("HomeDashboard", "Loaded streak data for profile ID: " + currentProfileId + " - Total: " + totalCompleted + ", Longest: " + longestStreak);
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error loading streak data: " + e.getMessage(), e);
            // Fallback to empty state
            tvCompletedTotal.setText("Completed Total: 0 days");
            tvLongestStreak.setText("Longest Streak: 0 days");
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

            // Sort dates
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
            SharedPreferences sharedPref = requireActivity().getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);

            // Get current profile ID to ensure we're showing the right profile
            int currentProfileId = sharedPref.getInt("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = sharedPref.getInt("selected_profile_id", -1);
            }

            String name = sharedPref.getString("child_name", null);
            String age = sharedPref.getString("child_age", null);
            String conditions = sharedPref.getString("child_conditions", null);

            if (name != null && age != null) {
                String formattedConditions = "";
                if (conditions != null && !conditions.trim().isEmpty()) {
                    String[] conditionArray = conditions.trim().split("\\s+");
                    formattedConditions = String.join(", ", conditionArray);
                } else {
                    formattedConditions = "No listed condition";
                }

                tvChildName.setText(name);
                tvChildDetails.setText("Age " + age + " • " + formattedConditions);

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

            // Get current profile ID
            SharedPreferences sharedPref = requireActivity().getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
            int currentProfileId = sharedPref.getInt("current_profile_id", -1);
            // Fallback to selected_profile_id for backward compatibility
            if (currentProfileId == -1) {
                currentProfileId = sharedPref.getInt("selected_profile_id", -1);
            }

            // Use profile-scoped query directly from database
            List<ReminderModel> reminders;
            if (currentProfileId > 0) {
                // Get reminders filtered by profile ID (ONLY reminders for this profile, excluding global)
                // If you want to include global reminders (profile_id = 0), use getActiveRemindersByProfile()
                // For now, let's show only profile-specific reminders
                reminders = reminderDbHelper.getActiveRemindersByProfile(currentProfileId);

                // Filter out global reminders (profile_id = 0) to show only profile-specific ones
                List<ReminderModel> profileSpecificReminders = new ArrayList<>();
                for (ReminderModel reminder : reminders) {
                    if (reminder != null && reminder.getProfileId() != null && reminder.getProfileId() == currentProfileId) {
                        profileSpecificReminders.add(reminder);
                    }
                }
                reminders = profileSpecificReminders;

                android.util.Log.d("HomeDashboard", "Loaded reminders for profile ID: " + currentProfileId + ", found: " + reminders.size() + " profile-specific reminders");
            } else {
                // No profile selected - show all active reminders
                reminders = reminderDbHelper.getActiveReminders();
                android.util.Log.d("HomeDashboard", "No profile selected, showing all active reminders: " + reminders.size());
            }

            if (reminders.isEmpty()) {
                // Show empty state
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

            // Sort reminders by time (nearest first)
            reminders.sort((r1, r2) -> {
                String time1 = r1.getTime() != null ? r1.getTime() : "";
                String time2 = r2.getTime() != null ? r2.getTime() : "";
                return time1.compareTo(time2);
            });

            // Display up to 5 upcoming reminders
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

            // Format time display
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
                    formattedTime = timeStr; // Fallback to raw time string
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


    /** Handles navigation and user clicks */
    private void setupListeners(View view) {
        if (ivReminder != null) {
            ivReminder.setOnClickListener(v -> navigateToFragment(new FragmentTasks()));
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
    }

    /** Navigate to ManageProfileFragment */
    private void navigateToManageProfile() {
        try {
            ManageProfileFragment manageProfileFragment = new ManageProfileFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, manageProfileFragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            android.util.Log.e("HomeDashboard", "Error navigating to ManageProfile: " + e.getMessage(), e);
            if (getContext() != null) {
                Toast.makeText(requireContext(), "Failed to open profile management", Toast.LENGTH_SHORT).show();
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
            // Try to navigate after view is ready
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
                navController.navigate(R.id.fragmentReportSummary);
            } else if (fragment instanceof SettingsFragment) {
                navController.navigate(R.id.settingsFragment);
            } else {
                android.util.Log.w("HomeDashboard", "Unknown fragment type for navigation: " + fragment.getClass().getSimpleName());
            }
        } catch (IllegalArgumentException e) {
            // NavController not found - fragment might not be in NavHostFragment
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

