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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

        // Bind UI elements
        bindViews(view);

        // Set up UI with placeholder (mock) data
        setTodayDate();
        loadChildProfile(); //Dynamic profile: name, age, conditions
        loadMockTaskProgress();
        loadMockStreakData();
        loadMockUpcomingTasks();

        // Handle clicks & interactivity
        setupListeners(view);

        return view;
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


    /** Mock: dynamically adds upcoming task cards */
    private void loadMockUpcomingTasks() {
        layoutUpcomingTasks.removeAllViews();

        String[] tasks = {"Handwashing", "Toothbrushing"};
        String[] times = {"8:00 AM", "8:30 AM"};

        for (int i = 0; i < tasks.length; i++) {
            LinearLayout taskCard = new LinearLayout(getContext());
            taskCard.setOrientation(LinearLayout.VERTICAL);
            taskCard.setPadding(20, 16, 20, 16);
            taskCard.setBackgroundResource(R.drawable.rounded_card_light);

            TextView tvTaskName = new TextView(getContext());
            tvTaskName.setText(tasks[i]);
            tvTaskName.setTextSize(16);
            tvTaskName.setTextColor(getResources().getColor(R.color.black));
            tvTaskName.setTypeface(null, Typeface.BOLD);

            TextView tvTaskTime = new TextView(getContext());
            tvTaskTime.setText("Scheduled at " + times[i]);
            tvTaskTime.setTextSize(14);
            tvTaskTime.setTextColor(getResources().getColor(R.color.subtitle_text));

            taskCard.addView(tvTaskName);
            taskCard.addView(tvTaskTime);

            taskCard.setOnClickListener(v -> safeNavigate(R.id.fragmentTasks));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            layoutUpcomingTasks.addView(taskCard, params);
        }
    }


    /** Handles navigation and user clicks */
    private void setupListeners(View view) {
        ivReminder.setOnClickListener(v -> safeNavigate(R.id.fragmentTasks));
        layoutChildProfile.setOnClickListener(v -> safeNavigate(R.id.fragmentBadges));
        layoutTaskProgress.setOnClickListener(v -> safeNavigate(R.id.fragmentReportSummary));
        layoutPoints.setOnClickListener(v -> safeNavigate(R.id.settingsFragment));
        layoutWeeklyStreak.setOnClickListener(v -> safeNavigate(R.id.fragmentTasks));
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
