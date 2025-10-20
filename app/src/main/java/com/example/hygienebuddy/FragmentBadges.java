package com.example.hygienebuddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FragmentBadges extends Fragment {

    private RecyclerView rvEarnedBadges, rvAllBadges;
    private TextView tvMotivationMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_badges, container, false);

        // Initialize views
        rvEarnedBadges = view.findViewById(R.id.rvEarnedBadges);
        rvAllBadges = view.findViewById(R.id.rvAllBadges);
        tvMotivationMessage = view.findViewById(R.id.tvMotivationMessage);

        rvEarnedBadges.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAllBadges.setLayoutManager(new LinearLayoutManager(getContext()));

        // Sample data (replace later with actual logic or SQLite data)
        List<BadgeModel> allBadges = getMockBadges();
        List<BadgeModel> earnedBadges = new ArrayList<>();

        for (BadgeModel badge : allBadges) {
            if (badge.isEarned()) {
                earnedBadges.add(badge);
            }
        }

        // Set adapters
        BadgeAdapter earnedAdapter = new BadgeAdapter(getContext(), earnedBadges);
        BadgeAdapter allAdapter = new BadgeAdapter(getContext(), allBadges);
        rvEarnedBadges.setAdapter(earnedAdapter);
        rvAllBadges.setAdapter(allAdapter);

        // Motivation text
        if (earnedBadges.isEmpty()) {
            tvMotivationMessage.setText("You're just getting started! Complete tasks to earn your first badge!");
        } else {
            tvMotivationMessage.setText("You're doing great! You've earned " + earnedBadges.size() + " badge(s)! Keep going!");
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Wait until the view hierarchy is fully drawn before updating navbar
        view.post(() -> BottomNavHelper.setupBottomNav(this, "badges"));
    }


    private List<BadgeModel> getMockBadges() {
        List<BadgeModel> badges = new ArrayList<>();
        badges.add(new BadgeModel("First Step", "Completed your first hygiene task!", true, "2025-10-01", 0, 0));
        badges.add(new BadgeModel("Consistency Champ", "Completed tasks for 5 consecutive days!", false, null, 3, 5));
        badges.add(new BadgeModel("Super Cleaner", "Completed all hygiene routines this week!", false, null, 4, 7));
        badges.add(new BadgeModel("Helper Star", "Assisted a friend during hygiene time!", true, "2025-09-28", 0, 0));
        return badges;
    }
}

