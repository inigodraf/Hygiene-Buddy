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
    private int lastLoadedProfileId = -1;
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

        // Initialize repository
        badgeRepository = new BadgeRepository(requireContext());
        cachedAllBadges = new ArrayList<>();
        cachedEarnedBadges = new ArrayList<>();

        // Initialize adapters with empty lists (will be populated in refreshBadges)
        earnedAdapter = new BadgeAdapter(getContext(), cachedEarnedBadges);
        allAdapter = new BadgeAdapter(getContext(), cachedAllBadges);
        rvEarnedBadges.setAdapter(earnedAdapter);
        rvAllBadges.setAdapter(allAdapter);

        // Initialize last loaded profile ID from SQLite
        AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
        lastLoadedProfileId = appDataDb.getIntSetting("current_profile_id", -1);
        if (lastLoadedProfileId == -1) {
            lastLoadedProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
        }

        // Load badges for current profile
        refreshBadges();

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
        // Refresh badges when resuming (profile may have changed)
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
        if (badgeRepository == null || !isAdded()) return;

        // Get current profile ID from SQLite
        AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
        int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
        if (currentProfileId == -1) {
            currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
        }

        // Check if profile has changed
        boolean profileChanged = (currentProfileId != lastLoadedProfileId);

        if (profileChanged) {
            android.util.Log.d("FragmentBadges", "Profile changed from " + lastLoadedProfileId + " to " + currentProfileId);
            lastLoadedProfileId = currentProfileId;
        }

        // Load all badges from database (structure is the same for all profiles)
        cachedAllBadges = badgeRepository.getAllBadges();

        // Create profile-scoped badge models with unlock status and progress from SQLite
        List<BadgeModel> profileScopedBadges = new ArrayList<>();
        cachedEarnedBadges.clear();

        for (BadgeModel badge : cachedAllBadges) {
            if (badge == null) continue;

            String badgeKey = badge.getImageKey();
            if (badgeKey == null || badgeKey.isEmpty()) {
                // If no key, use database values
                profileScopedBadges.add(badge);
                if (badge.isEarned()) {
                    cachedEarnedBadges.add(badge);
                }
                continue;
            }

            // Get profile-scoped unlock status and progress from SQLite
            boolean isUnlocked = appDataDb.isBadgeUnlocked(currentProfileId, badgeKey);
            int progress = appDataDb.getBadgeProgress(currentProfileId, badgeKey);
            String earnedDate = appDataDb.getBadgeEarnedDate(currentProfileId, badgeKey);

            // Create profile-scoped badge model
            BadgeModel profileBadge = new BadgeModel(
                    badgeKey,                    // key
                    badge.getTitle(),            // title
                    badge.getDescription(),      // description
                    isUnlocked,                  // isEarned
                    earnedDate,                  // earnedDate
                    progress,                    // progress
                    badge.getGoal(),            // goal
                    badge.getImageKey() != null ? badge.getImageKey() : badgeKey  // imageKey (use badge's imageKey or fallback to key)
            );

            profileScopedBadges.add(profileBadge);
            if (isUnlocked) {
                cachedEarnedBadges.add(profileBadge);
            }
        }

        // Update cached badges with profile-scoped data
        cachedAllBadges = profileScopedBadges;

        // Update adapters
        if (earnedAdapter != null) {
            earnedAdapter.setBadges(cachedEarnedBadges);
            earnedAdapter.notifyDataSetChanged();
        }
        if (allAdapter != null) {
            allAdapter.setBadges(cachedAllBadges);
            allAdapter.notifyDataSetChanged();
        }

        // Update motivation message
        if (tvMotivationMessage != null) {
            if (cachedEarnedBadges.isEmpty()) {
                tvMotivationMessage.setText("You're just getting started! Complete tasks to earn your first badge!");
            } else {
                tvMotivationMessage.setText("You're doing great! You've earned " + cachedEarnedBadges.size() + " badge(s)! Keep going!");
            }
        }

        android.util.Log.d("FragmentBadges", "Refreshed badges for profile ID: " + currentProfileId + " - Earned: " + cachedEarnedBadges.size() + ", Total: " + cachedAllBadges.size());
    }
}

