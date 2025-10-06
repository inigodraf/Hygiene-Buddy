package com.example.hygienebuddy;

import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FragmentTaskSteps extends Fragment {

    // UI elements
    private TextView tvTaskTitle, tvStepProgress, tvInstruction, tvMinutes, tvSeconds;
    private ImageView ivStepImage;
    private ImageButton btnPlayVideo;
    private ProgressBar progressStep;
    private Button btnNext;
    private VideoView videoView;

    // Step data
    private List<TaskStep> steps;
    private int currentStepIndex = 0;

    // Timer
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long remainingTimeMillis = 0;

    // Task type (passed via Bundle)
    private String taskType;

    public FragmentTaskSteps() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks_steps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        tvTaskTitle = view.findViewById(R.id.tvTaskTitle);
        tvStepProgress = view.findViewById(R.id.tvStepProgress);
        tvInstruction = view.findViewById(R.id.tvInstruction);
        tvMinutes = view.findViewById(R.id.tvMinutes);
        tvSeconds = view.findViewById(R.id.tvSeconds);
        ivStepImage = view.findViewById(R.id.ivStepImage);
        btnPlayVideo = view.findViewById(R.id.btnPlayVideo);
        progressStep = view.findViewById(R.id.progressStep);
        btnNext = view.findViewById(R.id.btnNext);

        // Prepare a VideoView programmatically
        videoView = new VideoView(requireContext());
        ViewGroup mediaContainer = (ViewGroup) ivStepImage.getParent();
        mediaContainer.addView(videoView);
        videoView.setVisibility(View.GONE);

        // Get taskType from bundle
        if (getArguments() != null) {
            taskType = getArguments().getString("taskType", "toothbrushing");
        }

        // Load steps and display the first one
        loadSteps(taskType);
        showStep(currentStepIndex);

        // "Next" button
        btnNext.setOnClickListener(v -> goToNextStep());

        // Play video (if available)
        btnPlayVideo.setOnClickListener(v -> playFacilitatorVideo());
    }

    /** Loads task steps based on selected type */
    private void loadSteps(String type) {
        steps = new ArrayList<>();

        if (type.equals("toothbrushing")) {
            tvTaskTitle.setText("Toothbrushing Routine");
            steps.add(new TaskStep(1, "Wet your toothbrush with water.", R.drawable.ic_toothbrush, 0, 15));
            steps.add(new TaskStep(2, "Apply toothpaste — about the size of a pea.", R.drawable.ic_toothbrush, 0, 15));
            steps.add(new TaskStep(3, "Brush your teeth gently for 2 minutes.", R.drawable.ic_toothbrush, 2, 0));
            steps.add(new TaskStep(4, "Rinse your mouth and toothbrush.", R.drawable.ic_toothbrush, 0, 20));
        } else if (type.equals("handwashing")) {
            tvTaskTitle.setText("Handwashing Routine");
            steps.add(new TaskStep(1, "Wet your hands with clean water.", R.drawable.ic_handwash, 0, 10));
            steps.add(new TaskStep(2, "Apply soap to your hands.", R.drawable.ic_handwash, 0, 10));
            steps.add(new TaskStep(3, "Rub your hands together for 20 seconds.", R.drawable.ic_handwash, 0, 20));
            steps.add(new TaskStep(4, "Rinse your hands thoroughly.", R.drawable.ic_handwash, 0, 15));
            steps.add(new TaskStep(5, "Dry your hands with a clean towel.", R.drawable.ic_handwash, 0, 10));
        }

        progressStep.setMax(steps.size());
    }

    /** Displays the current step’s data and loads the correct video */
    private void showStep(int index) {
        if (index < 0 || index >= steps.size()) return;
        TaskStep current = steps.get(index);

        tvStepProgress.setText(String.format(Locale.getDefault(), "Step %d of %d", current.number, steps.size()));
        tvInstruction.setText(current.instruction);
        progressStep.setProgress(current.number);

        // Try to load the facilitator video for this step
        boolean videoLoaded = loadFacilitatorVideo(taskType, current.number);

        // Fallback to image if no video exists
        if (!videoLoaded) {
            videoView.setVisibility(View.GONE);
            ivStepImage.setVisibility(View.VISIBLE);
            ivStepImage.setImageResource(current.imageResId);
            btnPlayVideo.setVisibility(View.GONE);
        }

        // Start timer
        startTimer((current.minutes * 60L + current.seconds) * 1000);

        // Change button text for last step
        btnNext.setText(index == steps.size() - 1 ? "Finish" : "Next");
    }

    /** Moves to the next step or finishes the routine */
    private void goToNextStep() {
        if (countDownTimer != null) countDownTimer.cancel();

        if (currentStepIndex < steps.size() - 1) {
            currentStepIndex++;
            showStep(currentStepIndex);
        } else {
            tvInstruction.setText("Great job! You’ve completed the task!");
            tvStepProgress.setText("Task Completed");
            btnNext.setVisibility(View.GONE);
            ivStepImage.setImageResource(R.drawable.ic_placeholder_video);
            videoView.setVisibility(View.GONE);
            btnPlayVideo.setVisibility(View.GONE);
        }
    }

    /** Loads a facilitator-uploaded video if it exists */
    private boolean loadFacilitatorVideo(String taskType, int stepNumber) {
        try {
            // Example path: /storage/emulated/0/Android/data/com.example.hygienebuddy/files/Videos/toothbrushing_step1.mp4
            File videoFile = new File(requireContext().getExternalFilesDir("Videos"),
                    taskType + "_step" + stepNumber + ".mp4");

            if (videoFile.exists()) {
                Uri videoUri = Uri.fromFile(videoFile);
                videoView.setVideoURI(videoUri);
                videoView.seekTo(1); // preview frame
                videoView.setVisibility(View.VISIBLE);
                ivStepImage.setVisibility(View.GONE);
                btnPlayVideo.setVisibility(View.VISIBLE);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Starts video playback */
    private void playFacilitatorVideo() {
        if (videoView != null && videoView.getVisibility() == View.VISIBLE) {
            videoView.start();
            btnPlayVideo.setVisibility(View.GONE);
        }
    }

    /** Starts the step countdown timer */
    private void startTimer(long durationMillis) {
        if (durationMillis <= 0) {
            tvMinutes.setText("00");
            tvSeconds.setText("00");
            return;
        }

        remainingTimeMillis = durationMillis;

        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeMillis = millisUntilFinished;
                int totalSeconds = (int) (millisUntilFinished / 1000);
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;

                tvMinutes.setText(String.format(Locale.getDefault(), "%02d", minutes));
                tvSeconds.setText(String.format(Locale.getDefault(), "%02d", seconds));
            }

            @Override
            public void onFinish() {
                tvMinutes.setText("00");
                tvSeconds.setText("00");
            }
        }.start();

        isTimerRunning = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) countDownTimer.cancel();
        if (videoView != null) videoView.stopPlayback();
    }

    /** Model for each step */
    private static class TaskStep {
        int number;
        String instruction;
        int imageResId;
        int minutes;
        int seconds;

        TaskStep(int number, String instruction, int imageResId, int minutes, int seconds) {
            this.number = number;
            this.instruction = instruction;
            this.imageResId = imageResId;
            this.minutes = minutes;
            this.seconds = seconds;
        }
    }
}
