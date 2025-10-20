package com.example.hygienebuddy;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SettingsFragment extends Fragment {

    private LinearLayout layoutTasksList, layoutRemindersList;
    private RecyclerView rvReinforcers;
    private MaterialButton btnManageInstructions, btnAddReminder, btnManageReinforcers;
    private TextView tvNoReminders;

    // Data placeholders (to be replaced by database integration)
    private List<String> reminderList = new ArrayList<>();
    private List<String> reinforcersList = new ArrayList<>();

    // Launcher for video upload
    private ActivityResultLauncher<Intent> videoPickerLauncher;
    private String currentTaskSelected = "";

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        layoutTasksList = view.findViewById(R.id.layoutTasksList);
        layoutRemindersList = view.findViewById(R.id.layoutRemindersList);
        rvReinforcers = view.findViewById(R.id.rvReinforcers);
        btnManageInstructions = view.findViewById(R.id.btnManageInstructions);
        btnAddReminder = view.findViewById(R.id.btnAddReminder);
        btnManageReinforcers = view.findViewById(R.id.btnManageReinforcers);
        tvNoReminders = view.findViewById(R.id.tvNoReminders);

        // --- Video Upload Handling ---
        setupVideoPicker();

        // --- Task Upload Buttons ---
        View taskHandwashing = view.findViewById(R.id.taskHandwashing);
        View taskToothbrushing = view.findViewById(R.id.taskToothbrushing);

        taskHandwashing.setOnClickListener(v -> openUploadActivity("Handwashing"));
        taskToothbrushing.setOnClickListener(v -> openUploadActivity("Toothbrushing"));

        // --- Manage Instructions Button ---
        btnManageInstructions.setOnClickListener(v ->
                Toast.makeText(getContext(), "Open Manage Instructions Screen", Toast.LENGTH_SHORT).show()
        );

        // --- Reminder Handling ---
        btnAddReminder.setOnClickListener(v -> openReminderDialog());
        displayReminders();

        // --- Reinforcers Handling ---
        setupReinforcers();
        btnManageReinforcers.setOnClickListener(v ->
                Toast.makeText(getContext(), "Manage Reinforcers Clicked", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    // ---------------------------------------------------------------
    // VIDEO PICKER SETUP
    // ---------------------------------------------------------------
    private void setupVideoPicker() {
        videoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        if (videoUri != null) {
                            Toast.makeText(getContext(),
                                    currentTaskSelected + " video uploaded: " + videoUri.getLastPathSegment(),
                                    Toast.LENGTH_LONG).show();

                            previewUploadedVideo(videoUri);
                        }
                    }
                });
    }

    private void openUploadActivity(String taskName) {
        Intent intent = new Intent(getContext(), UploadVideoActivity.class);
        intent.putExtra("TASK_NAME", taskName);
        startActivity(intent);
    }

    private void previewUploadedVideo(Uri videoUri) {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), videoUri);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                Toast.makeText(getContext(), "Playing sample of uploaded video...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error previewing video", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------
    // REMINDER SYSTEM (ALARM MANAGER DEMO)
    // ---------------------------------------------------------------
    private void openReminderDialog() {
        String newReminder = "Handwashing Reminder - " + System.currentTimeMillis();
        reminderList.add(newReminder);

        scheduleReminder();
        displayReminders();
    }

    private void displayReminders() {
        layoutRemindersList.removeAllViews();
        if (reminderList.isEmpty()) {
            tvNoReminders.setVisibility(View.VISIBLE);
            return;
        }
        tvNoReminders.setVisibility(View.GONE);

        for (String reminder : reminderList) {
            TextView tv = new TextView(getContext());
            tv.setText(reminder);
            tv.setTextSize(14f);
            tv.setPadding(8, 8, 8, 8);
            layoutRemindersList.addView(tv);
        }
    }

    private void scheduleReminder() {
        Context context = getContext();
        if (context == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("reminder_text", "Time for your hygiene task!");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 10);

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            Toast.makeText(context, "Reminder scheduled (demo)", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------
    // REINFORCERS DISPLAY (RECYCLERVIEW)
    // ---------------------------------------------------------------
    private void setupReinforcers() {
        reinforcersList.add("Sticker");
        reinforcersList.add("Song");
        reinforcersList.add("Cartoon Time");
        reinforcersList.add("Treat");

        rvReinforcers.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvReinforcers.setAdapter(new ReinforcerAdapter(reinforcersList));
    }

    // ---------------------------------------------------------------
    // ADAPTER CLASS
    // ---------------------------------------------------------------
    static class ReinforcerAdapter extends RecyclerView.Adapter<ReinforcerAdapter.ViewHolder> {
        private final List<String> items;

        ReinforcerAdapter(List<String> items) {
            this.items = items;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = new TextView(itemView.getContext());
                ((ViewGroup) itemView).addView(textView);
            }
        }

        @NonNull
        @Override
        public ReinforcerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setPadding(16, 16, 16, 16);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
            return new ViewHolder(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull ReinforcerAdapter.ViewHolder holder, int position) {
            String item = items.get(position);
            holder.textView.setText(item);
            holder.textView.setTextSize(16f);
            holder.textView.setPadding(8, 8, 8, 8);
            holder.textView.setOnClickListener(v ->
                    Toast.makeText(v.getContext(), "Selected: " + item, Toast.LENGTH_SHORT).show()
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ✅ Delay setup until view hierarchy is ready
        view.post(() -> BottomNavHelper.setupBottomNav(this, "settings"));
    }
}
