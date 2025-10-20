package com.example.hygienebuddy;

import android.graphics.Color;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FragmentStreak extends Fragment {

    private ImageView btnBack;
    private ImageButton btnPrevMonth, btnNextMonth;
    private TextView tvMonthYear, tvCurrentStreak;
    private GridLayout gridCalendar;

    private Calendar currentCalendar;
    private Set<String> completedDays; // e.g., "2025-10-05"
    private int currentStreakCount = 0;

    public FragmentStreak() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_streak, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        btnBack = view.findViewById(R.id.btnBack);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        gridCalendar = view.findViewById(R.id.gridCalendar);
        tvCurrentStreak = view.findViewById(R.id.tvCurrentStreak);

        // Initialize calendar
        currentCalendar = Calendar.getInstance();
        completedDays = new HashSet<>();

        // Example: mock completed days (normally loaded from database)
        completedDays.add("2025-10-01");
        completedDays.add("2025-10-02");
        completedDays.add("2025-10-03");
        completedDays.add("2025-10-05");

        // Display initial month
        updateCalendar();

        // Handle back button
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Month navigation
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        // Highlight home in bottom nav and setup click listeners
        // BottomNavHelper.setupBottomNav(this, "streak");
    }

    private void updateCalendar() {
        // Clear previous cells
        gridCalendar.removeAllViews();

        // Format month and year
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentCalendar.getTime()));

        // Start of month
        Calendar tempCal = (Calendar) currentCalendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1; // Sunday=1
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Create empty cells before first day
        for (int i = 0; i < firstDayOfWeek; i++) {
            addEmptyCell();
        }

        // Create day cells
        SimpleDateFormat dayKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar today = Calendar.getInstance();
        currentStreakCount = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            TextView dayView = new TextView(getContext());
            dayView.setText(String.valueOf(day));
            dayView.setGravity(android.view.Gravity.CENTER);
            dayView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    100
            ));
            dayView.setPadding(4, 4, 4, 4);

            tempCal.set(Calendar.DAY_OF_MONTH, day);
            String dateKey = dayKeyFormat.format(tempCal.getTime());

            // Default background
            dayView.setBackgroundResource(R.drawable.bg_calendar_day_normal);

            // Check if completed
            if (completedDays.contains(dateKey)) {
                dayView.setBackgroundResource(R.drawable.bg_calendar_day_completed);
            }

            // Highlight today's date
            if (isSameDay(tempCal, today)) {
                dayView.setBackgroundResource(R.drawable.bg_calendar_day_today);
                dayView.setTextColor(Color.WHITE);
            }

            // OnClick: toggle completion
            dayView.setOnClickListener(v -> {
                if (completedDays.contains(dateKey)) {
                    completedDays.remove(dateKey);
                    Toast.makeText(getContext(), "Marked as incomplete", Toast.LENGTH_SHORT).show();
                } else {
                    completedDays.add(dateKey);
                    Toast.makeText(getContext(), "Marked as completed", Toast.LENGTH_SHORT).show();
                }
                updateCalendar();
            });

            gridCalendar.addView(dayView);
        }

        // Update streak count
        currentStreakCount = calculateCurrentStreak();
        tvCurrentStreak.setText(String.valueOf(currentStreakCount));
    }

    private void addEmptyCell() {
        TextView empty = new TextView(getContext());
        empty.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                100
        ));
        gridCalendar.addView(empty);
    }

    private int calculateCurrentStreak() {
        Calendar today = Calendar.getInstance();
        int streak = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        while (true) {
            String key = sdf.format(today.getTime());
            if (completedDays.contains(key)) {
                streak++;
                today.add(Calendar.DAY_OF_MONTH, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}
