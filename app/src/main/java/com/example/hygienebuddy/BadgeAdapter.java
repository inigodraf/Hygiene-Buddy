package com.example.hygienebuddy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private List<BadgeModel> badges;
    private final Context context;

    public BadgeAdapter(Context context, List<BadgeModel> badges) {
        this.context = context;
        this.badges = badges != null ? badges : new ArrayList<>();
    }

    public void setBadges(List<BadgeModel> badges) {
        this.badges = badges != null ? badges : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        BadgeModel badge = badges.get(position);

        holder.tvBadgeTitle.setText(badge.getTitle());
        holder.tvBadgeDescription.setText(badge.getDescription());

        // Themed badge icon (prefer explicit imageKey when available)
        int iconRes = (badge.getImageKey() != null && !badge.getImageKey().isEmpty())
                ? BadgeThemeManager.getBadgeIconResByKey(context, badge.getImageKey())
                : BadgeThemeManager.getBadgeIconRes(context, badge.getTitle());
        holder.ivBadgeIcon.setImageResource(iconRes);

        if (badge.isEarned()) {
            // Show earned badge details
            holder.layoutProgress.setVisibility(View.GONE);
            holder.tvEarnedDate.setVisibility(View.VISIBLE);
            holder.tvEarnedDate.setText("Earned on: " + badge.getEarnedDate());
            holder.ivStatusIndicator.setImageResource(R.drawable.ic_earned);
            holder.itemView.setAlpha(1.0f);
        } else {
            // Show progress toward earning the badge
            holder.layoutProgress.setVisibility(View.VISIBLE);
            holder.tvEarnedDate.setVisibility(View.GONE);
            holder.progressBar.setMax(badge.getGoal());
            holder.progressBar.setProgress(badge.getProgress());
            holder.tvProgressText.setText(badge.getProgress() + "/" + badge.getGoal() + " tasks completed");
            holder.ivStatusIndicator.setImageResource(R.drawable.ic_locked);
            holder.itemView.setAlpha(0.7f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (badge.isEarned()) {
                if (context instanceof FragmentActivity) {
                    RewardPopupDialogFragment dialog = RewardPopupDialogFragment.newInstance(
                            badge.getTitle(),
                            badge.getDescription(),
                            badge.getImageKey(),
                            badge.getEarnedDate()
                    );
                    dialog.show(((FragmentActivity) context).getSupportFragmentManager(), "reward_popup");
                } else {
                    Toast.makeText(context, "You earned the \"" + badge.getTitle() + "\" badge!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "This badge is locked. Keep completing tasks to unlock it!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadgeTitle, tvBadgeDescription, tvProgressText, tvEarnedDate;
        View layoutProgress;
        ProgressBar progressBar;
        ImageView ivStatusIndicator, ivBadgeIcon;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadgeTitle = itemView.findViewById(R.id.tvBadgeTitle);
            tvBadgeDescription = itemView.findViewById(R.id.tvBadgeDescription);
            layoutProgress = itemView.findViewById(R.id.layoutProgress);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvProgressText = itemView.findViewById(R.id.tvProgressText);
            tvEarnedDate = itemView.findViewById(R.id.tvEarnedDate);
            ivStatusIndicator = itemView.findViewById(R.id.ivStatusIndicator);
            ivBadgeIcon = itemView.findViewById(R.id.ivBadgeIcon);
        }
    }
}
