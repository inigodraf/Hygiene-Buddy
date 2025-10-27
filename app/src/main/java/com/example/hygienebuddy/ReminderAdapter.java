package com.example.hygienebuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private List<ReminderModel> reminders;
    private OnReminderClickListener listener;

    public interface OnReminderClickListener {
        void onDeleteClick(int position, ReminderModel reminder);
    }

    public ReminderAdapter(List<ReminderModel> reminders, OnReminderClickListener listener) {
        this.reminders = reminders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        ReminderModel reminder = reminders.get(position);
        holder.tvTaskName.setText(reminder.getTaskName());

        // Format time and frequency display
        String timeDisplay = formatTimeDisplay(reminder);
        holder.tvTime.setText(timeDisplay);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position, reminder);
            }
        });
    }

    private String formatTimeDisplay(ReminderModel reminder) {
        String time = reminder.getTime();
        String frequency = reminder.getFrequency();

        switch (frequency) {
            case "once":
                return time + " - Once";
            case "daily":
                return time + " - Daily";
            case "weekly":
                String days = reminder.getDaysOfWeek();
                if (days != null && !days.isEmpty()) {
                    return time + " - Weekly (" + days + ")";
                }
                return time + " - Weekly";
            case "custom":
                int interval = reminder.getCustomInterval();
                if (interval > 0) {
                    return time + " - Every " + interval + " days";
                }
                return time + " - Custom";
            case "multiple":
                String timesPerDay = reminder.getTimesPerDay();
                if (timesPerDay != null && !timesPerDay.isEmpty()) {
                    String[] times = timesPerDay.split(",");
                    if (times.length > 1) {
                        return times.length + " times daily (" + timesPerDay + ")";
                    }
                }
                return time + " - Multiple times";
            default:
                return time + " - " + capitalize(frequency);
        }
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    public void updateList(List<ReminderModel> newReminders) {
        reminders = newReminders;
        notifyDataSetChanged();
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvTime;
        ImageView btnDelete;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}



