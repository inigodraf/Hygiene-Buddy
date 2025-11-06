package com.example.hygienebuddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FragmentBadges extends Fragment {

    private RecyclerView rvEarnedBadges, rvAllBadges;
    private TextView tvMotivationMessage;
    private List<BadgeModel> cachedAllBadges;
    private List<BadgeModel> cachedEarnedBadges;
    private BadgeAdapter earnedAdapter;
    private BadgeAdapter allAdapter;
    private BadgeRepository badgeRepository;
    private final BroadcastReceiver themeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (earnedAdapter != null) earnedAdapter.notifyDataSetChanged();
            if (allAdapter != null) allAdapter.notifyDataSetChanged();
        }
    };

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

        // Repository-backed data
        badgeRepository = new BadgeRepository(requireContext());
        cachedAllBadges = badgeRepository.getAllBadges();
        cachedEarnedBadges = new ArrayList<>();
        for (BadgeModel badge : cachedAllBadges) {
            if (badge.isEarned()) {
                cachedEarnedBadges.add(badge);
            }
        }

        // Set adapters
        earnedAdapter = new BadgeAdapter(getContext(), cachedEarnedBadges);
        allAdapter = new BadgeAdapter(getContext(), cachedAllBadges);
        rvEarnedBadges.setAdapter(earnedAdapter);
        rvAllBadges.setAdapter(allAdapter);

        // Motivation text
        if (cachedEarnedBadges.isEmpty()) {
            tvMotivationMessage.setText("You're just getting started! Complete tasks to earn your first badge!");
        } else {
            tvMotivationMessage.setText("You're doing great! You've earned " + cachedEarnedBadges.size() + " badge(s)! Keep going!");
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Wait until the view hierarchy is fully drawn before updating navbar
        view.post(() -> BottomNavHelper.setupBottomNav(this, "badges"));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh from DB and rebind to reflect theme changes
        refreshBadges();
    }

    @Override
    public void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(requireContext(), themeChangedReceiver, new IntentFilter(BadgeThemeManager.ACTION_BADGE_THEME_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            requireContext().unregisterReceiver(themeChangedReceiver);
        } catch (Exception ignored) {}
    }


    private void refreshBadges() {
        if (badgeRepository == null) return;
        cachedAllBadges = badgeRepository.getAllBadges();
        cachedEarnedBadges.clear();
        for (BadgeModel badge : cachedAllBadges) {
            if (badge.isEarned()) {
                cachedEarnedBadges.add(badge);
            }
        }
        if (earnedAdapter != null) earnedAdapter.notifyDataSetChanged();
        if (allAdapter != null) allAdapter.notifyDataSetChanged();
    }
}

