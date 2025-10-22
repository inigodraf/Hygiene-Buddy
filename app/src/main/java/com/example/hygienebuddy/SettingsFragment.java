package com.example.hygienebuddy;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SettingsFragment extends Fragment {

    private LinearLayout layoutTasksList, layoutRemindersList;
    private RecyclerView rvReinforcers, rvTasks;
    private MaterialButton btnListVideos, btnAddReminder, btnManageReinforcers;
    private TextView tvNoReminders;

    // Data placeholders (to be replaced by database integration)
    private List<String> reminderList = new ArrayList<>();
    private List<String> reinforcersList = new ArrayList<>();

    // Video management
    private VideoManager videoManager;
    private ActivityResultLauncher<Intent> videoPickerLauncher;
    private ActivityResultLauncher<Intent> videoRecorderLauncher;
    private String currentTaskSelected = "";
    private int currentStepSelected = 0;

    // Permission launcher
    private ActivityResultLauncher<String[]> permissionLauncher;

    // Expandable task adapter
    private ExpandableTaskAdapter taskAdapter;

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        //layoutTasksList = view.findViewById(R.id.layoutTasksList);
        layoutRemindersList = view.findViewById(R.id.layoutRemindersList);
        rvReinforcers = view.findViewById(R.id.rvReinforcers);
        rvTasks = view.findViewById(R.id.rvTasks);
        btnListVideos = view.findViewById(R.id.btnListVideos);
        btnAddReminder = view.findViewById(R.id.btnAddReminder);
        btnManageReinforcers = view.findViewById(R.id.btnManageReinforcers);
        tvNoReminders = view.findViewById(R.id.tvNoReminders);

        // Initialize VideoManager
        videoManager = new VideoManager(requireContext());

        // --- Video Upload Handling ---
        setupVideoLaunchers();
        setupPermissionLauncher();

        // --- Setup Expandable Task List ---
        setupExpandableTasks();

        // --- Video Management Buttons ---
        btnListVideos.setOnClickListener(v -> showExistingVideosDialog());

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
    // VIDEO MANAGEMENT SETUP
    // ---------------------------------------------------------------
    private void setupVideoLaunchers() {
        // Gallery picker launcher
        videoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        if (videoUri != null && !currentTaskSelected.isEmpty()) {
                            saveVideoToLocalStorage(videoUri);
                        }
                    }
                });

        // Camera recorder launcher
        videoRecorderLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        if (videoUri != null && !currentTaskSelected.isEmpty()) {
                            saveVideoToLocalStorage(videoUri);
                        }
                    }
                });
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (String permission : permissions.keySet()) {
                        if (!permissions.get(permission)) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        Toast.makeText(getContext(), "Permission granted! You can now upload videos.", Toast.LENGTH_SHORT).show();
                        // Retry the action that was blocked
                        if (!currentTaskSelected.isEmpty()) {
                            // Determine which action to retry based on what was requested
                            if (permissions.containsKey(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                                    permissions.containsKey(Manifest.permission.READ_MEDIA_VIDEO)) {
                                uploadVideoFromGallery();
                            } else if (permissions.containsKey(Manifest.permission.CAMERA)) {
                                recordVideoWithCamera();
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Permission denied. Video upload is not available.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------------------------------------------------------------
    // EXPANDABLE TASKS SETUP
    // ---------------------------------------------------------------
    private void setupExpandableTasks() {
        // Create task data
        List<ExpandableTaskAdapter.TaskData> tasks = new ArrayList<>();

        // Handwashing task
        List<TaskStep> handwashingSteps = new ArrayList<>();
        handwashingSteps.add(new TaskStep(1, "Identify the necessary materials to be used.", R.drawable.ic_handwashing, 0, 5, "handwashing")); // 5s
        handwashingSteps.add(new TaskStep(2, "Turn on the faucet using your dominant hand.", R.drawable.ic_handwashing, 0, 5, "handwashing")); // 5s
        handwashingSteps.add(new TaskStep(3, "Wet your hands under the running water.", R.drawable.ic_handwashing, 0, 10, "handwashing")); // 10s
        handwashingSteps.add(new TaskStep(4, "Turn off the faucet to save water.", R.drawable.ic_handwashing, 0, 3, "handwashing")); // 3s
        handwashingSteps.add(new TaskStep(5, "Get the soap with your dominant hand from the soap dish.", R.drawable.ic_handwashing, 0, 3, "handwashing")); // 5s
        handwashingSteps.add(new TaskStep(6, "Rub your hands together to create a rich lather.", R.drawable.ic_handwashing, 0, 5, "handwashing")); // 5s
        handwashingSteps.add(new TaskStep(7, "Scrub all parts of your hands, including between your fingers and under your nails.", R.drawable.ic_handwashing, 0, 20, "handwashing")); // 20s (main scrubbing)
        handwashingSteps.add(new TaskStep(8, "Turn on the faucet again.", R.drawable.ic_handwashing, 0, 3, "handwashing")); // 3s
        handwashingSteps.add(new TaskStep(9, "Rinse your hands thoroughly under running water.", R.drawable.ic_handwashing, 0, 10, "handwashing")); // 10s
        handwashingSteps.add(new TaskStep(10, "Turn off the faucet using your dominant hand.", R.drawable.ic_handwashing, 0, 3, "handwashing")); // 3s
        handwashingSteps.add(new TaskStep(11, "Shake your hands gently to remove excess water.", R.drawable.ic_handwashing, 0, 5, "handwashing")); // 5s
        handwashingSteps.add(new TaskStep(12, "Pick up the towel using your dominant hand.", R.drawable.ic_handwashing, 0, 3, "handwashing")); // 3s
        handwashingSteps.add(new TaskStep(13, "Dry your hands thoroughly with the towel.", R.drawable.ic_handwashing, 0, 10, "handwashing")); // 10s
        handwashingSteps.add(new TaskStep(14, "Return the towel to its proper place.", R.drawable.ic_handwashing, 0, 3, "handwashing")); // 3s
        tasks.add(new ExpandableTaskAdapter.TaskData("Handwashing", "handwashing", handwashingSteps));

        // Toothbrushing task
        List<TaskStep> toothbrushingSteps = new ArrayList<>();
        toothbrushingSteps.add(new TaskStep(1,  "Get your toothbrush case.", R.drawable.ic_toothbrushing, 0, 5,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(2,  "Unzip the case.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(3,  "Take out the toothpaste.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(4,  "Unscrew the toothpaste cap.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(5,  "Place the cap on the countertop.", R.drawable.ic_toothbrushing, 0, 2,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(6,  "Turn on the faucet.", R.drawable.ic_toothbrushing, 0, 2,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(7,  "Take out your toothbrush.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(8,  "Wet the bristles of the toothbrush.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(9,  "Put toothpaste on the toothbrush.", R.drawable.ic_toothbrushing, 0, 4,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(10, "Place the toothpaste on the countertop.", R.drawable.ic_toothbrushing, 0, 2,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(11, "Bring the toothbrush with toothpaste up to your mouth.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));

        // Main brushing phase (scientifically ~2 minutes total, 20s per section)
        toothbrushingSteps.add(new TaskStep(12, "Brush teeth: Left back — top, outside then inside.", R.drawable.ic_toothbrushing, 0, 20, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(13, "Brush teeth: Left back — bottom, outside then inside.", R.drawable.ic_toothbrushing, 0, 20, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(14, "Brush teeth: Front — top, outside then inside.", R.drawable.ic_toothbrushing, 0, 20, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(15, "Brush teeth: Front — bottom, outside then inside.", R.drawable.ic_toothbrushing, 0, 20, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(16, "Brush teeth: Right back — top, outside then inside.", R.drawable.ic_toothbrushing, 0, 20, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(17, "Brush teeth: Right back — bottom, outside then inside.", R.drawable.ic_toothbrushing, 0, 20, "toothbrushing"));

        toothbrushingSteps.add(new TaskStep(18, "Spit the toothpaste into the sink.", R.drawable.ic_toothbrushing, 0, 4,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(19, "Rinse the toothbrush under running water.", R.drawable.ic_toothbrushing, 0, 4,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(20, "Shake off excess water from the toothbrush.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(21, "Put the toothbrush back in the toothbrush case.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(22, "Get your drinking cup from the case.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(23, "Rinse your mouth with water.", R.drawable.ic_toothbrushing, 0, 5,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(24, "Spit the water into the sink.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(25, "Rinse the cup with water.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(26, "Wipe the cup dry.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(27, "Put the cup back into the toothbrush case.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(28, "Put the toothpaste cap back on the tube.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(29, "Put the toothpaste into the toothbrush case.", R.drawable.ic_toothbrushing, 0, 3,  "toothbrushing"));
        tasks.add(new ExpandableTaskAdapter.TaskData("Toothbrushing", "toothbrushing", toothbrushingSteps));


        // Setup adapter
        taskAdapter = new ExpandableTaskAdapter(requireContext(), new ExpandableTaskAdapter.OnStepVideoUploadListener() {
            @Override
            public void onStepVideoUpload(String taskType, int stepNumber) {
                selectStepForVideo(taskType, stepNumber);
            }
        });

        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(taskAdapter);
        taskAdapter.setTasks(tasks);
    }

    private void selectStepForVideo(String taskType, int stepNumber) {
        currentTaskSelected = taskType;
        currentStepSelected = stepNumber;

        // Show dialog to choose upload method
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Upload Video for " + capitalizeFirst(taskType) + " - Step " + stepNumber)
                .setMessage("Choose how you want to add a video for this step:")
                .setPositiveButton("From Gallery", (dialog, which) -> uploadVideoFromGallery())
                .setNegativeButton("Record New", (dialog, which) -> recordVideoWithCamera())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void uploadVideoFromGallery() {
        if (currentTaskSelected.isEmpty()) {
            Toast.makeText(getContext(), "Please select a task first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkStoragePermission()) {
            requestStoragePermission();
            return;
        }

        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setType("video/*");
        videoPickerLauncher.launch(pickIntent);
    }

    private void recordVideoWithCamera() {
        if (currentTaskSelected.isEmpty()) {
            Toast.makeText(getContext(), "Please select a task first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkCameraPermission()) {
            requestCameraPermission();
            return;
        }

        Intent recordIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        recordIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        recordIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 300); // 5 minutes max
        videoRecorderLauncher.launch(recordIntent);
    }

    private void saveVideoToLocalStorage(Uri videoUri) {
        boolean success = videoManager.saveStepVideo(currentTaskSelected, currentStepSelected, videoUri);

        if (success) {
            Toast.makeText(getContext(),
                    "✅ " + capitalizeFirst(currentTaskSelected) + " Step " + currentStepSelected + " video saved locally!",
                    Toast.LENGTH_SHORT).show();

            // Preview the uploaded video
            previewUploadedVideo(videoManager.getStepVideoUri(currentTaskSelected, currentStepSelected));

            // Refresh the adapter to show updated video status
            if (taskAdapter != null) {
                taskAdapter.notifyDataSetChanged();
            }

            // Debug: Log video status
            boolean hasVideo = videoManager.hasStepVideo(currentTaskSelected, currentStepSelected);
            android.util.Log.d("SettingsFragment", "Video saved and verified: " + hasVideo);
        } else {
            Toast.makeText(getContext(),
                    "❌ Failed to save video. Please try again.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void previewUploadedVideo(Uri videoUri) {
        if (videoUri != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(videoUri, "video/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Check if there's an app that can handle this intent
                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "No video player app found", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error opening video preview: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showExistingVideosDialog() {
        StringBuilder message = new StringBuilder("Uploaded step videos:\n\n");
        boolean hasVideos = false;

        // Check handwashing steps
        int[] handwashingSteps = videoManager.getStoredStepVideos("handwashing");
        if (handwashingSteps.length > 0) {
            message.append("Handwashing:\n");
            for (int step : handwashingSteps) {
                long fileSize = videoManager.getStepVideoFileSize("handwashing", step);
                String sizeText = formatFileSize(fileSize);
                message.append("  • Step ").append(step).append(" (").append(sizeText).append(")\n");
            }
            message.append("\n");
            hasVideos = true;
        }

        // Check toothbrushing steps
        int[] toothbrushingSteps = videoManager.getStoredStepVideos("toothbrushing");
        if (toothbrushingSteps.length > 0) {
            message.append("Toothbrushing:\n");
            for (int step : toothbrushingSteps) {
                long fileSize = videoManager.getStepVideoFileSize("toothbrushing", step);
                String sizeText = formatFileSize(fileSize);
                message.append("  • Step ").append(step).append(" (").append(sizeText).append(")\n");
            }
            hasVideos = true;
        }

        if (!hasVideos) {
            Toast.makeText(getContext(), "No step videos uploaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Existing Step Videos")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_VIDEO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Storage Permission Needed")
                    .setMessage("This app needs access to your storage to upload videos from your gallery.")
                    .setPositiveButton("Grant", (dialog, which) -> {
                        permissionLauncher.launch(new String[]{permission});
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            permissionLauncher.launch(new String[]{permission});
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Camera Permission Needed")
                    .setMessage("This app needs access to your camera to record videos.")
                    .setPositiveButton("Grant", (dialog, which) -> {
                        permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
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
        reinforcersList.add("Sticker Option1");
        reinforcersList.add("Sticker Option2");

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
