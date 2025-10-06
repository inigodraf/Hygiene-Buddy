package com.example.hygienebuddy;

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
        loadMockChildProfile();
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

        // Highlight home in bottom nav and setup click listeners
        BottomNavHelper.setupBottomNav(this, "home");
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

        // New clickable containers from XML
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

    /** Mock: loads a child profile (replace later with DB data) */
    private void loadMockChildProfile() {
        tvChildName.setText("Liam Santos");
        tvChildDetails.setText("Age 8 • ADHD");
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
        streakDays.add(true);  // Mon
        streakDays.add(true);  // Tue
        streakDays.add(false); // Wed
        streakDays.add(true);  // Thu
        streakDays.add(false); // Fri
        streakDays.add(false); // Sat
        streakDays.add(true);  // Sun

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

    /** Mock: dynamically adds upcoming task cards */
    private void loadMockUpcomingTasks() {
        layoutUpcomingTasks.removeAllViews();

        String[] tasks = {"Handwashing", "Toothbrushing", "Hair Combing"};
        String[] times = {"8:00 AM", "8:30 AM", "9:00 AM"};

        for (int i = 0; i < tasks.length; i++) {
            LinearLayout taskCard = new LinearLayout(getContext());
            taskCard.setOrientation(LinearLayout.VERTICAL);
            taskCard.setPadding(20, 16, 20, 16);
            taskCard.setBackgroundResource(R.drawable.rounded_card_light);

            TextView tvTaskName = new TextView(getContext());
            tvTaskName.setText(tasks[i]);
            tvTaskName.setTextSize(16);
            tvTaskName.setTextColor(getResources().getColor(R.color.black));
            tvTaskName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            tvTaskName.setTypeface(null, Typeface.BOLD);

            TextView tvTaskTime = new TextView(getContext());
            tvTaskTime.setText("Scheduled at " + times[i]);
            tvTaskTime.setTextSize(14);
            tvTaskTime.setTextColor(getResources().getColor(R.color.subtitle_text));

            taskCard.addView(tvTaskName);
            taskCard.addView(tvTaskTime);

            String actionName = "action_homeDashboard_to_taskDetails";
            taskCard.setOnClickListener(v -> safeNavigate(v, actionName));

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
        String actionReminder = "action_homeDashboard_to_reminderPage";
        String actionChildProfile = "action_homeDashboard_to_childProfile";
        String actionTaskList = "action_homeDashboard_to_taskList";
        String actionRewards = "action_homeDashboard_to_rewardsPage";
        String actionStreakDetails = "action_homeDashboard_to_streakDetails";

        ivReminder.setOnClickListener(v -> safeNavigate(v, actionReminder));
        layoutChildProfile.setOnClickListener(v -> safeNavigate(v, actionChildProfile));
        layoutTaskProgress.setOnClickListener(v -> safeNavigate(v, actionTaskList));
        layoutPoints.setOnClickListener(v -> safeNavigate(v, actionRewards));
        layoutWeeklyStreak.setOnClickListener(v -> safeNavigate(v, actionStreakDetails));
    }

    private void safeNavigate(View view, String name) {
        if (view == null || name == null) return;

        int resId = 0;
        try {
            resId = requireContext().getResources().getIdentifier(name, "id", requireContext().getPackageName());
        } catch (Exception e) {}

        if (resId == 0) {
            Toast.makeText(getContext(),
                    "Navigation action/destination '" + name + "' not found. Add it to res/navigation/nav_graph.xml",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Navigation.findNavController(view).navigate(resId);
        } catch (Exception ex) {
            Toast.makeText(getContext(),
                    "Unable to navigate using '" + name + "'. Check nav_graph or host fragment id.",
                    Toast.LENGTH_SHORT).show();
            ex.printStackTrace();
        }
    }
}
