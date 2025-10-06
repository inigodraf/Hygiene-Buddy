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
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private final List<BadgeModel> badges;
    private final Context context;

    public BadgeAdapter(Context context, List<BadgeModel> badges) {
        this.context = context;
        this.badges = badges;
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
                Toast.makeText(context, "You earned the \"" + badge.getTitle() + "\" badge!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Keep going! You're close to earning \"" + badge.getTitle() + "\"!", Toast.LENGTH_SHORT).show();
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
        ImageView ivStatusIndicator;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadgeTitle = itemView.findViewById(R.id.tvBadgeTitle);
            tvBadgeDescription = itemView.findViewById(R.id.tvBadgeDescription);
            layoutProgress = itemView.findViewById(R.id.layoutProgress);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvProgressText = itemView.findViewById(R.id.tvProgressText);
            tvEarnedDate = itemView.findViewById(R.id.tvEarnedDate);
            ivStatusIndicator = itemView.findViewById(R.id.ivStatusIndicator);
        }
    }
}
