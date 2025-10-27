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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    // Reminder database
    private ReminderDatabaseHelper reminderDbHelper;

    // Containers for clickable navigation
    private LinearLayout layoutChildProfile, layoutTaskProgress, layoutPoints, layoutWeeklyStreak;

    public HomeDashboardFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home_dashboard, container, false);

        // Initialize reminder database
        reminderDbHelper = new ReminderDatabaseHelper(requireContext());

        // Bind UI elements
        bindViews(view);

        // Set up UI with placeholder (mock) data
        setTodayDate();
        loadChildProfile(); //Dynamic profile: name, age, conditions
        loadMockTaskProgress();
        loadMockStreakData();
        loadUpcomingReminders(); // Changed from loadMockUpcomingTasks()

        // Handle clicks & interactivity
        setupListeners(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh reminders when returning to this screen
        if (reminderDbHelper != null) {
            loadUpcomingReminders();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.post(() -> BottomNavHelper.setupBottomNav(this, "home"));
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

    /** Mock: loads example progress and points */
    private void loadMockTaskProgress() {
        int completed = 3;
        int total = 5;
        int progressPercent = (int) ((completed / (float) total) * 100);

        progressTasks.setProgress(progressPercent);
        tvTaskProgress.setText(progressPercent + "% completed");
        tvPoints.setText((completed * 10) + " XP");
    }

    /** Mock: loads example streak icons and stats */
    private void loadMockStreakData() {
        layoutStreakDays.removeAllViews();
        ArrayList<Boolean> streakDays = new ArrayList<>();
        streakDays.add(true);
        streakDays.add(true);
        streakDays.add(false);
        streakDays.add(true);
        streakDays.add(false);
        streakDays.add(false);
        streakDays.add(true);

        for (boolean completed : streakDays) {
            ImageView dayView = new ImageView(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(70, 70);
            lp.setMargins(8, 8, 8, 8);
            dayView.setLayoutParams(lp);
            dayView.setPadding(8, 8, 8, 8);
            dayView.setImageResource(completed ? R.drawable.ic_streak_filled : R.drawable.ic_streak_empty);
            layoutStreakDays.addView(dayView);
        }

        tvCompletedTotal.setText("Completed Total: 4 days");
        tvLongestStreak.setText("Longest Streak: 2 days");
    }

    private void loadChildProfile() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
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
        } else {
            tvChildName.setText("No profile set");
            tvChildDetails.setText("Please create a child profile to begin");
        }
    }


    /** Load actual reminders from database and display them */
    private void loadUpcomingReminders() {
        layoutUpcomingTasks.removeAllViews();

        // Fetch active reminders from database
        List<ReminderModel> reminders = reminderDbHelper.getActiveReminders();

        if (reminders == null || reminders.isEmpty()) {
            // Show placeholder text if no reminders
            TextView noRemindersText = new TextView(getContext());
            noRemindersText.setText("No upcoming tasks. Set reminders in Settings.");
            noRemindersText.setTextSize(14);
            noRemindersText.setTextColor(getResources().getColor(R.color.subtitle_text));
            noRemindersText.setGravity(android.view.Gravity.CENTER);
            noRemindersText.setPadding(20, 16, 20, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutUpcomingTasks.addView(noRemindersText, params);
            return;
        }

        // Sort reminders by time (nearest first)
        Collections.sort(reminders, new Comparator<ReminderModel>() {
            @Override
            public int compare(ReminderModel r1, ReminderModel r2) {
                return r1.getTime().compareTo(r2.getTime());
            }
        });

        // Display up to 5 upcoming reminders
        int maxDisplay = Math.min(reminders.size(), 5);
        for (int i = 0; i < maxDisplay; i++) {
            ReminderModel reminder = reminders.get(i);

            LinearLayout taskCard = new LinearLayout(getContext());
            taskCard.setOrientation(LinearLayout.VERTICAL);
            taskCard.setPadding(20, 16, 20, 16);
            taskCard.setBackgroundResource(R.drawable.rounded_card_light);

            TextView tvTaskName = new TextView(getContext());
            tvTaskName.setText(reminder.getTaskName());
            tvTaskName.setTextSize(16);
            tvTaskName.setTextColor(getResources().getColor(R.color.black));
            tvTaskName.setTypeface(null, Typeface.BOLD);

            TextView tvTaskTime = new TextView(getContext());
            String timeDisplay = formatReminderTimeDisplay(reminder);
            tvTaskTime.setText("Scheduled: " + timeDisplay);
            tvTaskTime.setTextSize(14);
            tvTaskTime.setTextColor(getResources().getColor(R.color.subtitle_text));

            taskCard.addView(tvTaskName);
            taskCard.addView(tvTaskTime);

            // Make task card clickable to navigate to tasks screen
            taskCard.setOnClickListener(v -> safeNavigate(R.id.fragmentTasks));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            layoutUpcomingTasks.addView(taskCard, params);
        }
    }

    /** Format time for display (convert 24-hour to 12-hour with AM/PM) */
    private String formatTime(String time24) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date date = inputFormat.parse(time24);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return time24; // Return original if parsing fails
        }
    }

    /** Format reminder time display for dashboard */
    private String formatReminderTimeDisplay(ReminderModel reminder) {
        String time = formatTime(reminder.getTime());
        String frequency = reminder.getFrequency();

        switch (frequency) {
            case "once":
                return time + " (Once)";
            case "daily":
                return time + " (Daily)";
            case "weekly":
                String days = reminder.getDaysOfWeek();
                if (days != null && !days.isEmpty()) {
                    return time + " (" + days + ")";
                }
                return time + " (Weekly)";
            case "custom":
                int interval = reminder.getCustomInterval();
                if (interval > 0) {
                    return time + " (Every " + interval + " days)";
                }
                return time + " (Custom)";
            case "multiple":
                String timesPerDay = reminder.getTimesPerDay();
                if (timesPerDay != null && !timesPerDay.isEmpty()) {
                    String[] times = timesPerDay.split(",");
                    if (times.length > 1) {
                        return times.length + " times daily";
                    }
                }
                return time + " (Multiple)";
            default:
                return time + " (" + capitalize(frequency) + ")";
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(Locale.getDefault()) + text.substring(1).toLowerCase(Locale.getDefault());
    }

    /** Handles navigation and user clicks */
    private void setupListeners(View view) {
        ivReminder.setOnClickListener(v -> safeNavigate(R.id.fragmentTasks));
        layoutChildProfile.setOnClickListener(v -> navigateToManageProfile());
        layoutTaskProgress.setOnClickListener(v -> safeNavigate(R.id.fragmentReportSummary));
        layoutPoints.setOnClickListener(v -> safeNavigate(R.id.settingsFragment));
        layoutWeeklyStreak.setOnClickListener(v -> safeNavigate(R.id.fragmentTasks));
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
            Toast.makeText(requireContext(), "Failed to open profile management", Toast.LENGTH_SHORT).show();
        }
    }

    /** Safe navigation helper using main NavHostFragment */
    private void safeNavigate(int destinationId) {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            int current = navController.getCurrentDestination() != null
                    ? navController.getCurrentDestination().getId()
                    : -1;

            if (current != destinationId) {
                navController.navigate(destinationId);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Navigation failed. Check nav_graph.xml.", Toast.LENGTH_SHORT).show();
        }
    }
}
