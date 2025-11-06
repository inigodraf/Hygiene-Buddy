package com.example.hygienebuddy;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class RewardPopupDialogFragment extends DialogFragment {

    private static final String ARG_BADGE_TITLE = "arg_badge_title";
    private static final String ARG_BADGE_DESC = "arg_badge_desc";
    private static final String ARG_BADGE_KEY = "arg_badge_key";
    private static final String ARG_BADGE_DATE = "arg_badge_date";

    public static RewardPopupDialogFragment newInstance(String title, String description) {
        RewardPopupDialogFragment frag = new RewardPopupDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BADGE_TITLE, title);
        args.putString(ARG_BADGE_DESC, description);
        frag.setArguments(args);
        return frag;
    }

    public static RewardPopupDialogFragment newInstance(String title, String description, String imageKey) {
        RewardPopupDialogFragment frag = new RewardPopupDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BADGE_TITLE, title);
        args.putString(ARG_BADGE_DESC, description);
        args.putString(ARG_BADGE_KEY, imageKey);
        frag.setArguments(args);
        return frag;
    }

    public static RewardPopupDialogFragment newInstance(String title, String description, String imageKey, String earnedDate) {
        RewardPopupDialogFragment frag = new RewardPopupDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BADGE_TITLE, title);
        args.putString(ARG_BADGE_DESC, description);
        args.putString(ARG_BADGE_KEY, imageKey);
        args.putString(ARG_BADGE_DATE, earnedDate);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reward_popup, null);
        String title = getArguments() != null ? getArguments().getString(ARG_BADGE_TITLE, "Badge Unlocked!") : "Badge Unlocked!";
        String desc = getArguments() != null ? getArguments().getString(ARG_BADGE_DESC, "Great job!") : "Great job!";
        String key = getArguments() != null ? getArguments().getString(ARG_BADGE_KEY) : null;
        String earnedDate = getArguments() != null ? getArguments().getString(ARG_BADGE_DATE) : null;

        ImageView ivBadge = view.findViewById(R.id.ivBadge);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDesc = view.findViewById(R.id.tvDesc);
        TextView tvDate = view.findViewById(R.id.tvDate);

        if (key != null && !key.isEmpty()) {
            ivBadge.setImageResource(BadgeThemeManager.getBadgeIconResByKey(requireContext(), key));
        } else {
            ivBadge.setImageResource(BadgeThemeManager.getBadgeIconRes(requireContext(), title));
        }
        tvTitle.setText(title);
        tvDesc.setText(desc);
        if (tvDate != null) {
            if (earnedDate != null && !earnedDate.isEmpty()) {
                tvDate.setText("Earned on: " + earnedDate);
                tvDate.setVisibility(View.VISIBLE);
            } else {
                tvDate.setVisibility(View.GONE);
            }
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setPositiveButton("Awesome!", null)
                .create();
    }
}


