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
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.VideoView;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.nio.ByteBuffer;
import java.io.FileOutputStream;
import java.io.InputStream;
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

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private RecyclerView rvTasks, rvReminders;
    private MaterialButton btnListVideos, btnAddReminder;
    private TextView tvNoReminders;
    private RadioGroup rgBadgeTheme;
    private RadioButton rbBubbleQuest, rbCleanHeroes;

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

    // Reminders
    private ReminderDatabaseHelper reminderDbHelper;
    private ReminderAdapter reminderAdapter;
    private List<ReminderModel> reminderList = new ArrayList<>();

    // Reinforcers
    private List<String> reinforcersList = new ArrayList<>();

    // Voice cloning import
    private ActivityResultLauncher<Intent> audioPickerLauncher;
    private ActivityResultLauncher<Intent> videoAudioExtractorLauncher;
    private String voiceSampleFilePath = null;

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        rvTasks = view.findViewById(R.id.rvTasks);
        rvReminders = view.findViewById(R.id.rvReminders);
        btnListVideos = view.findViewById(R.id.btnListVideos);
        btnAddReminder = view.findViewById(R.id.btnAddReminder);
        tvNoReminders = view.findViewById(R.id.tvNoReminders);
        rgBadgeTheme = view.findViewById(R.id.rgBadgeTheme);
        rbBubbleQuest = view.findViewById(R.id.rbBubbleQuest);
        rbCleanHeroes = view.findViewById(R.id.rbCleanHeroes);

        // Initialize helpers
        try {
            reminderDbHelper = new ReminderDatabaseHelper(requireContext());
            videoManager = new VideoManager(requireContext());

            setupVideoLaunchers();
            setupPermissionLauncher();
            setupExpandableTasks();
            setupVoiceImportLaunchers();

            if (btnListVideos != null) {
                btnListVideos.setOnClickListener(v -> showExistingVideosDialog());
            }

            // Reminder setup - with null checks
            if (rvReminders != null && btnAddReminder != null && tvNoReminders != null) {
                setupRemindersRecyclerView();
                btnAddReminder.setOnClickListener(v -> showAddReminderDialog());
                loadReminders();
            } else {
                android.util.Log.e("SettingsFragment", "Reminder views not found in layout");
            }
        } catch (Exception e) {
            android.util.Log.e("SettingsFragment", "Error initializing SettingsFragment: " + e.getMessage(), e);
            // Don't crash - just log the error
        }


        // Badge Theme setup
        setupBadgeThemeSelector();

        View btnImportVoice = view.findViewById(R.id.btnImportVoice);
        if (btnImportVoice != null)
            btnImportVoice.setOnClickListener(v -> importVoiceSample());


        return view;
    }

    private void setupBadgeThemeSelector() {
        if (rgBadgeTheme == null || rbBubbleQuest == null || rbCleanHeroes == null) return;
        BadgeThemeManager.Theme current = BadgeThemeManager.getCurrentTheme(requireContext());
        if (current == BadgeThemeManager.Theme.CLEAN_HEROES) {
            rbCleanHeroes.setChecked(true);
        } else {
            rbBubbleQuest.setChecked(true);
        }

        rgBadgeTheme.setOnCheckedChangeListener((group, checkedId) -> {
            BadgeThemeManager.Theme selected = checkedId == R.id.rbCleanHeroes
                    ? BadgeThemeManager.Theme.CLEAN_HEROES
                    : BadgeThemeManager.Theme.BUBBLE_QUEST;
            BadgeThemeManager.setCurrentTheme(requireContext(), selected);
            Toast.makeText(getContext(), "Badge theme set to " + (selected == BadgeThemeManager.Theme.CLEAN_HEROES ? "Clean Heroes" : "Bubble Quest"), Toast.LENGTH_SHORT).show();
        });
    }

    // ---------------------------------------------------------------
    // VIDEO MANAGEMENT SETUP
    // ---------------------------------------------------------------
    private void setupVideoLaunchers() {
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
                        if (!currentTaskSelected.isEmpty()) {
                            uploadVideoFromGallery();
                        }
                    } else {
                        Toast.makeText(getContext(), "Permission denied. Video upload unavailable.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------------------------------------------------------------
    // EXPANDABLE TASKS SETUP
    // ---------------------------------------------------------------
    private void setupExpandableTasks() {
        List<ExpandableTaskAdapter.TaskData> tasks = new ArrayList<>();

        // ✅ Handwashing task (kept same)
        List<TaskStep> handwashingSteps = new ArrayList<>();
        handwashingSteps.add(new TaskStep(1, "Identify the necessary materials to be used.", R.drawable.ic_handwashing, 0, 5, "handwashing"));
        handwashingSteps.add(new TaskStep(2, "Turn on the faucet using your dominant hand.", R.drawable.ic_handwashing, 0, 5, "handwashing"));
        handwashingSteps.add(new TaskStep(3, "Wet your hands under the running water.", R.drawable.ic_handwashing, 0, 10, "handwashing"));
        handwashingSteps.add(new TaskStep(4, "Turn off the faucet to save water.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
        handwashingSteps.add(new TaskStep(5, "Get the soap with your dominant hand from the soap dish.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
        handwashingSteps.add(new TaskStep(6, "Rub your hands together to create a rich lather.", R.drawable.ic_handwashing, 0, 5, "handwashing"));
        handwashingSteps.add(new TaskStep(7, "Scrub all parts of your hands, including between your fingers and under your nails.", R.drawable.ic_handwashing, 0, 20, "handwashing"));
        handwashingSteps.add(new TaskStep(8, "Turn on the faucet again.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
        handwashingSteps.add(new TaskStep(9, "Rinse your hands thoroughly under running water.", R.drawable.ic_handwashing, 0, 10, "handwashing"));
        handwashingSteps.add(new TaskStep(10, "Turn off the faucet using your dominant hand.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
        handwashingSteps.add(new TaskStep(11, "Shake your hands gently to remove excess water.", R.drawable.ic_handwashing, 0, 5, "handwashing"));
        handwashingSteps.add(new TaskStep(12, "Pick up the towel using your dominant hand.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
        handwashingSteps.add(new TaskStep(13, "Dry your hands thoroughly with the towel.", R.drawable.ic_handwashing, 0, 10, "handwashing"));
        handwashingSteps.add(new TaskStep(14, "Return the towel to its proper place.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
        tasks.add(new ExpandableTaskAdapter.TaskData("Handwashing", "handwashing", handwashingSteps));

        // ✅ Toothbrushing task (new 15-step version)
        List<TaskStep> toothbrushingSteps = new ArrayList<>();
        toothbrushingSteps.add(new TaskStep(1, "Pick up your toothbrush.", R.drawable.ic_toothbrushing, 0, 5, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(2, "Rinse the toothbrush with water.", R.drawable.ic_toothbrushing, 0, 3, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(3, "Open the toothpaste cap.", R.drawable.ic_toothbrushing, 0, 3, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(4, "Squeeze a small amount of toothpaste onto the brush.", R.drawable.ic_toothbrushing, 0, 4, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(5, "Close the toothpaste cap.", R.drawable.ic_toothbrushing, 0, 2, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(6, "Start brushing your top front teeth.", R.drawable.ic_toothbrushing, 0, 10, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(7, "Brush the top side teeth (left side).", R.drawable.ic_toothbrushing, 0, 10, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(8, "Brush the top side teeth (right side).", R.drawable.ic_toothbrushing, 0, 10, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(9, "Brush the bottom front teeth.", R.drawable.ic_toothbrushing, 0, 10, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(10, "Brush the bottom side teeth (left side).", R.drawable.ic_toothbrushing, 0, 10, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(11, "Brush the bottom side teeth (right side).", R.drawable.ic_toothbrushing, 0, 10, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(12, "Gently brush your tongue.", R.drawable.ic_toothbrushing, 0, 5, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(13, "Spit out the toothpaste into the sink.", R.drawable.ic_toothbrushing, 0, 3, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(14, "Rinse your mouth with water.", R.drawable.ic_toothbrushing, 0, 5, "toothbrushing"));
        toothbrushingSteps.add(new TaskStep(15, "Rinse your toothbrush and put it back in its holder.", R.drawable.ic_toothbrushing, 0, 5, "toothbrushing"));
        tasks.add(new ExpandableTaskAdapter.TaskData("Toothbrushing", "toothbrushing", toothbrushingSteps));

        // Attach adapter
        taskAdapter = new ExpandableTaskAdapter(requireContext(), this::selectStepForVideo);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(taskAdapter);
        taskAdapter.setTasks(tasks);
    }

    private void selectStepForVideo(String taskType, int stepNumber) {
        currentTaskSelected = taskType;
        currentStepSelected = stepNumber;

        File existingVideo = videoManager.getStepVideoFile(taskType, stepNumber);

        if (existingVideo != null && existingVideo.exists()) {
            // ✅ Show preview dialog if video already exists
            showExistingVideoPreview(existingVideo);
        } else {
            // ✅ Default behavior (no video yet)
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Upload Video for " + capitalizeFirst(taskType) + " - Step " + stepNumber)
                    .setMessage("Choose how you want to add a video for this step:")
                    .setPositiveButton("From Gallery", (dialog, which) -> uploadVideoFromGallery())
                    .setNegativeButton("Record New", (dialog, which) -> recordVideoWithCamera())
                    .setNeutralButton("Cancel", null)
                    .show();
        }
    }

    private void showExistingVideoPreview(File videoFile) {
        View previewLayout = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_video_preview_exoplayer, null);

        PlayerView playerView = previewLayout.findViewById(R.id.playerView);
        com.google.android.exoplayer2.ExoPlayer player =
                new com.google.android.exoplayer2.ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(player);

        Uri videoUri = Uri.fromFile(videoFile);
        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Preview Existing Video")
                .setView(previewLayout)
                .setPositiveButton("Replace", (dialog, which) -> {
                    player.release();
                    uploadVideoFromGallery(); // choose another
                })
                .setNegativeButton("Remove", (dialog, which) -> {
                    player.release();
                    boolean deleted = videoFile.delete();
                    Toast.makeText(
                            getContext(),
                            deleted ? "Video removed successfully." : "Failed to remove video.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNeutralButton("Cancel", (dialog, which) -> player.release())
                .setOnDismissListener(dialog -> player.release())
                .show();
    }


    private void uploadVideoFromGallery() {
        if (!checkStoragePermission()) {
            requestStoragePermission();
            return;
        }
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setType("video/*");
        videoPickerLauncher.launch(pickIntent);
    }

    private void recordVideoWithCamera() {
        if (!checkCameraPermission()) {
            requestCameraPermission();
            return;
        }
        Intent recordIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        recordIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        recordIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 300);
        videoRecorderLauncher.launch(recordIntent);
    }

    private void saveVideoToLocalStorage(Uri videoUri) {
        if (videoUri == null) return;

        View previewLayout = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_video_preview_exoplayer, null);

        PlayerView playerView = previewLayout.findViewById(R.id.playerView);
        com.google.android.exoplayer2.ExoPlayer player = new com.google.android.exoplayer2.ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(player);

        // Prepare media
        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        // Show dialog
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Preview Selected Video")
                .setView(previewLayout)
                .setPositiveButton("Use This Video", (dialog, which) -> {
                    boolean success = videoManager.saveStepVideo(currentTaskSelected, currentStepSelected, videoUri);
                    if (success) {
                        Toast.makeText(getContext(),
                                "✅ " + capitalizeFirst(currentTaskSelected) + " Step " + currentStepSelected + " video saved!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "❌ Failed to save video. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                    player.release();
                })
                .setNegativeButton("Try Another", (dialog, which) -> {
                    player.release();
                    uploadVideoFromGallery();
                })
                .setNeutralButton("Cancel", (dialog, which) -> player.release())
                .setOnDismissListener(dialog -> player.release())
                .show();
    }




    private void showExistingVideosDialog() {
        StringBuilder message = new StringBuilder("Uploaded step videos:\n\n");
        boolean hasVideos = false;

        int[] handwashingSteps = videoManager.getStoredStepVideos("handwashing");
        if (handwashingSteps.length > 0) {
            message.append("Handwashing:\n");
            for (int step : handwashingSteps) {
                message.append("  • Step ").append(step).append("\n");
            }
            message.append("\n");
            hasVideos = true;
        }

        int[] toothbrushingSteps = videoManager.getStoredStepVideos("toothbrushing");
        if (toothbrushingSteps.length > 0) {
            message.append("Toothbrushing:\n");
            for (int step : toothbrushingSteps) {
                message.append("  • Step ").append(step).append("\n");
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
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        permissionLauncher.launch(new String[]{permission});
    }

    private void requestCameraPermission() {
        permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // ---------------------------------------------------------------
    // REMINDERS & REINFORCERS
    // ---------------------------------------------------------------
    private void setupRemindersRecyclerView() {
        if (rvReminders == null || !isAdded() || getContext() == null) {
            android.util.Log.e("SettingsFragment", "Cannot setup reminders RecyclerView - views not ready");
            return;
        }
        try {
            rvReminders.setLayoutManager(new LinearLayoutManager(requireContext()));
            if (reminderList == null) {
                reminderList = new ArrayList<>();
            }
            reminderAdapter = new ReminderAdapter(reminderList, (position, reminder) -> deleteReminder(reminder.getId()));
            rvReminders.setAdapter(reminderAdapter);
        } catch (Exception e) {
            android.util.Log.e("SettingsFragment", "Error setting up reminders RecyclerView: " + e.getMessage(), e);
        }
    }

    private void loadReminders() {
        if (reminderDbHelper == null || reminderAdapter == null || tvNoReminders == null || reminderList == null) {
            android.util.Log.e("SettingsFragment", "Cannot load reminders - components not initialized");
            return;
        }
        if (!isAdded()) {
            android.util.Log.e("SettingsFragment", "Cannot load reminders - fragment not added");
            return;
        }
        try {
            reminderList.clear();

            // Get current profile ID
            // Get current profile ID from SQLite
            AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
            if (currentProfileId == -1) {
                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            }

            // Load reminders filtered by profile ID
            List<ReminderModel> profileReminders;
            if (currentProfileId > 0) {
                // Get all reminders (active and inactive) for this profile using profile-scoped query
                profileReminders = reminderDbHelper.getAllRemindersByProfile(currentProfileId);

                // Filter out global reminders (profile_id = 0) to show only profile-specific ones
                List<ReminderModel> profileSpecificReminders = new ArrayList<>();
                for (ReminderModel reminder : profileReminders) {
                    if (reminder != null && reminder.getProfileId() != null && reminder.getProfileId() == currentProfileId) {
                        profileSpecificReminders.add(reminder);
                    }
                }
                reminderList.addAll(profileSpecificReminders);
                android.util.Log.d("SettingsFragment", "Loaded reminders for profile ID: " + currentProfileId + ", found: " + profileSpecificReminders.size() + " profile-specific reminders");
            } else {
                // No profile selected - show all reminders (backward compatibility)
                reminderList.addAll(reminderDbHelper.getAllReminders());
                android.util.Log.d("SettingsFragment", "No profile selected, showing all reminders: " + reminderList.size());
            }

            reminderAdapter.notifyDataSetChanged();
            tvNoReminders.setVisibility(reminderList.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            android.util.Log.e("SettingsFragment", "Error loading reminders: " + e.getMessage(), e);
        }
    }

    private void showAddReminderDialog() {
        if (!isAdded() || getContext() == null) {
            android.util.Log.e("SettingsFragment", "Cannot show add reminder dialog - fragment not attached");
            return;
        }
        try {
            String[] taskOptions = {"Toothbrushing", "Handwashing"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Task")
                    .setItems(taskOptions, (dialog, which) -> {
                        String taskName = taskOptions[which];
                        showTimeAndFrequencyDialog(taskName);
                    })
                    .show();
        } catch (Exception e) {
            android.util.Log.e("SettingsFragment", "Error showing add reminder dialog: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error opening reminder dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimeAndFrequencyDialog(String taskName) {
        if (!isAdded() || getContext() == null) {
            return;
        }

        try {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_reminder, null);

            // Time picker
            TextView tvTimePicker = dialogView.findViewById(R.id.tvTimePicker);
            if (tvTimePicker == null) {
                Toast.makeText(getContext(), "Error loading reminder dialog", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar selectedTime = Calendar.getInstance();
            selectedTime.set(Calendar.HOUR_OF_DAY, 8);
            selectedTime.set(Calendar.MINUTE, 0);

            updateTimeDisplay(tvTimePicker, selectedTime);

            tvTimePicker.setOnClickListener(v -> {
                android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                        requireContext(),
                        (view, hourOfDay, minute) -> {
                            selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            selectedTime.set(Calendar.MINUTE, minute);
                            updateTimeDisplay(tvTimePicker, selectedTime);
                        },
                        selectedTime.get(Calendar.HOUR_OF_DAY),
                        selectedTime.get(Calendar.MINUTE),
                        false
                );
                timePickerDialog.show();
            });

            // Frequency selection
            RadioGroup rgFrequency = dialogView.findViewById(R.id.rgFrequency);
            RadioButton rbOnce = dialogView.findViewById(R.id.rbOnce);
            RadioButton rbDaily = dialogView.findViewById(R.id.rbDaily);
            RadioButton rbWeekly = dialogView.findViewById(R.id.rbWeekly);

            if (rbOnce == null || rbDaily == null || rbWeekly == null) {
                Toast.makeText(getContext(), "Error loading reminder dialog", Toast.LENGTH_SHORT).show();
                return;
            }

            rbDaily.setChecked(true); // Default to daily

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add Reminder for " + taskName)
                    .setView(dialogView)
                    .setPositiveButton("Add", (dialog, which) -> {
                        try {
                            String time = String.format(Locale.getDefault(), "%02d:%02d",
                                    selectedTime.get(Calendar.HOUR_OF_DAY),
                                    selectedTime.get(Calendar.MINUTE));

                            String frequency = "daily";
                            String daysOfWeek = null;
                            if (rbOnce.isChecked()) {
                                frequency = "once";
                            } else if (rbWeekly.isChecked()) {
                                frequency = "weekly";
                                // For weekly, default to all days (Mon-Sun)
                                daysOfWeek = "Mon,Tue,Wed,Thu,Fri,Sat,Sun";
                            }

                            ReminderModel reminder = new ReminderModel();
                            reminder.setTaskName(taskName);
                            reminder.setTime(time);
                            reminder.setFrequency(frequency);
                            reminder.setActive(true);
                            if (daysOfWeek != null) {
                                reminder.setDaysOfWeek(daysOfWeek);
                            }

                            // Set profile_id to current profile (if available)
                            // Get current profile ID from SQLite
                            AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
                            int currentProfileId = appDataDb.getIntSetting("current_profile_id", -1);
                            if (currentProfileId == -1) {
                                currentProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
                            }
                            if (currentProfileId > 0) {
                                reminder.setProfileId(currentProfileId);
                                android.util.Log.d("SettingsFragment", "Setting reminder profile_id to: " + currentProfileId);
                            } else {
                                // No profile selected - reminder will be global (profile_id = 0)
                                reminder.setProfileId(0);
                                android.util.Log.d("SettingsFragment", "No profile selected - reminder will be global");
                            }

                            // Validate reminder before inserting
                            if (reminder.getTaskName() == null || reminder.getTaskName().isEmpty()) {
                                Toast.makeText(getContext(), "Error: Task name is required", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (reminder.getTime() == null || reminder.getTime().isEmpty()) {
                                Toast.makeText(getContext(), "Error: Time is required", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (reminder.getFrequency() == null || reminder.getFrequency().isEmpty()) {
                                Toast.makeText(getContext(), "Error: Frequency is required", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            android.util.Log.d("SettingsFragment", "Inserting reminder: " + reminder.getTaskName() +
                                    " at " + reminder.getTime() + " (" + reminder.getFrequency() + ")");

                            long reminderId = reminderDbHelper.insertReminder(reminder);
                            if (reminderId > 0) {
                                reminder.setId((int) reminderId);
                                // Schedule the reminder
                                try {
                                    ReminderManager.scheduleReminder(requireContext(), reminder);
                                } catch (Exception e) {
                                    android.util.Log.e("SettingsFragment", "Error scheduling reminder: " + e.getMessage(), e);
                                    // Continue anyway - reminder is saved in DB
                                }
                                loadReminders();
                                Toast.makeText(getContext(), "Reminder added successfully!", Toast.LENGTH_SHORT).show();
                            } else {
                                android.util.Log.e("SettingsFragment", "insertReminder returned -1, check logs for details");
                                Toast.makeText(getContext(), "Failed to add reminder. Check logs for details.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            android.util.Log.e("SettingsFragment", "Error adding reminder: " + e.getMessage(), e);
                            Toast.makeText(getContext(), "Error adding reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            android.util.Log.e("SettingsFragment", "Error showing reminder dialog: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error loading reminder dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTimeDisplay(TextView tvTimePicker, Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvTimePicker.setText(sdf.format(calendar.getTime()));
    }

    private void deleteReminder(int reminderId) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Cancel the alarm before deleting
                    ReminderManager.cancelReminder(requireContext(), reminderId);
                    reminderDbHelper.deleteReminder(reminderId);
                    loadReminders();
                    Toast.makeText(getContext(), "Reminder deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static class ReinforcerAdapter extends RecyclerView.Adapter<ReinforcerAdapter.ViewHolder> {
        private final List<String> items;
        ReinforcerAdapter(List<String> items) { this.items = items; }

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
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setPadding(16, 16, 16, 16);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
            return new ViewHolder(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
        view.post(() -> BottomNavHelper.setupBottomNav(this, "settings"));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh reminders when resuming (profile may have changed)
        if (reminderDbHelper != null && reminderAdapter != null) {
            loadReminders();
        }
    }

    // ---------------------------------------------------------------
// VOICE CLONING IMPORT FEATURE
// ---------------------------------------------------------------
    private void setupVoiceImportLaunchers() {
        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri audioUri = result.getData().getData();
                        if (audioUri != null) {
                            handleImportedAudio(audioUri);
                        }
                    }
                });

        videoAudioExtractorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        if (videoUri != null) {
                            extractAudioFromVideo(videoUri);
                        }
                    }
                });
    }

    private void importVoiceSample() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Import Voice Sample")
                .setItems(new CharSequence[]{"Import MP3 file", "Import from Video"}, (dialog, which) -> {
                    if (which == 0) {
                        pickAudioFile();
                    } else {
                        pickVideoFile();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickAudioFile() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("audio/*");
        audioPickerLauncher.launch(pickIntent);
    }

    private void pickVideoFile() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("video/*");
        videoAudioExtractorLauncher.launch(pickIntent);
    }

    private void handleImportedAudio(Uri audioUri) {
        try {
            File outputDir = new File(requireContext().getFilesDir(), "voice_samples");
            if (!outputDir.exists()) outputDir.mkdirs();

            String fileName = "sample_" + System.currentTimeMillis() + ".mp3";
            File outputFile = new File(outputDir, fileName);

            try (InputStream in = requireContext().getContentResolver().openInputStream(audioUri);
                 FileOutputStream out = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            voiceSampleFilePath = outputFile.getAbsolutePath();
            Toast.makeText(getContext(), "Voice sample imported successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error importing audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void extractAudioFromVideo(Uri videoUri) {
        try {
            File outputDir = new File(requireContext().getFilesDir(), "voice_samples");
            if (!outputDir.exists()) outputDir.mkdirs();

            File outputFile = new File(outputDir, "voice_sample_" + System.currentTimeMillis() + ".mp4");

            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(requireContext(), videoUri, null);

            int audioTrackIndex = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }

            if (audioTrackIndex == -1) {
                extractor.release();
                Toast.makeText(getContext(), "No audio track found in video", Toast.LENGTH_SHORT).show();
                return;
            }

            extractor.selectTrack(audioTrackIndex);
            MediaMuxer muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int dstIndex = muxer.addTrack(extractor.getTrackFormat(audioTrackIndex));
            muxer.start();

            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            while (true) {
                info.offset = 0;
                info.size = extractor.readSampleData(buffer, 0);
                if (info.size < 0) break;
                info.presentationTimeUs = extractor.getSampleTime();
                info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                muxer.writeSampleData(dstIndex, buffer, info);
                extractor.advance();
            }

            muxer.stop();
            muxer.release();
            extractor.release();

            voiceSampleFilePath = outputFile.getAbsolutePath();
            Toast.makeText(getContext(), "Audio extracted successfully!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Audio extraction failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
