package com.example.hygienebuddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FragmentReportSummary extends Fragment {

    private ImageView btnBack, ivUserAvatar;
    private TextView tvUserName, tvUserAge, tvUserConditions, tvCurrentMonth,
            tvTotalPoints, tvBadgesEarned;
    private MaterialButtonToggleGroup toggleDateRange;
    private MaterialButton btnWeek, btnMonth, btnDownloadReport;
    private ImageButton btnPrevMonth, btnNextMonth;
    private GridLayout gridCalendar;

    private Calendar calendar;

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

        // Initialize calendar
        calendar = Calendar.getInstance();

        // Load initial data (mocked)
        loadMockUserData();
        updateCalendarView();

        // Back button
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Date range toggle
        toggleDateRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnWeek) {
                Toast.makeText(getContext(), "Showing weekly summary", Toast.LENGTH_SHORT).show();
                // TODO: Query weekly data from database
            } else if (checkedId == R.id.btnMonth) {
                Toast.makeText(getContext(), "Showing monthly summary", Toast.LENGTH_SHORT).show();
                // TODO: Query monthly data from database
            }
        });

        // Month navigation
        btnPrevMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateCalendarView();
        });

        btnNextMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateCalendarView();
        });

        // Download report
        btnDownloadReport.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Downloading CSV report...", Toast.LENGTH_SHORT).show();
            // TODO: Implement CSV export logic from SQLite or Room DB
        });

        return view;
    }

    /** Mock data — replace with real DB query results later */
    private void loadMockUserData() {
        // TODO: Replace with actual user info from DB or arguments
        tvUserName.setText("Ethan");
        tvUserAge.setText("Age 8");
        tvUserConditions.setText("ASD, ADHD");

        // Replace with actual computed stats from DB
        tvTotalPoints.setText("150 XP");
        tvBadgesEarned.setText("3");
    }

    /** Updates the calendar grid dynamically */
    private void updateCalendarView() {
        gridCalendar.removeAllViews();

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCurrentMonth.setText(monthFormat.format(calendar.getTime()));

        Calendar tempCal = (Calendar) calendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1; // 0-based
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Fill empty slots before day 1
        for (int i = 0; i < firstDayOfWeek; i++) {
            addEmptyDay();
        }

        // Populate days
        for (int day = 1; day <= daysInMonth; day++) {
            addCalendarDay(day);
        }
    }

    /** Adds a blank space in the calendar grid (for padding days) */
    private void addEmptyDay() {
        View empty = new View(getContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        empty.setLayoutParams(params);
        gridCalendar.addView(empty);
    }

    /** Adds a day cell (with dynamic appearance for completed/today) */
    private void addCalendarDay(int day) {
        TextView tvDay = new TextView(getContext());
        tvDay.setText(String.valueOf(day));
        tvDay.setGravity(android.view.Gravity.CENTER);
        tvDay.setPadding(16, 16, 16, 16);
        tvDay.setTextSize(14f);

        Calendar today = Calendar.getInstance();
        boolean isToday = (day == today.get(Calendar.DAY_OF_MONTH)
                && calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR));

        // Mock completed days (for demo)
        boolean isCompleted = (day % 3 == 0); // Every 3rd day as "completed"

        if (isToday) {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_day_today);
        } else if (isCompleted) {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_day_completed);
        }

        // Clickable day cells (optional)
        tvDay.setOnClickListener(v ->
                Toast.makeText(getContext(), "Day " + day + " selected", Toast.LENGTH_SHORT).show()
        );

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        tvDay.setLayoutParams(params);

        gridCalendar.addView(tvDay);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Highlight home in bottom nav and setup click listeners
        BottomNavHelper.setupBottomNav(this, "report");
    }

}
