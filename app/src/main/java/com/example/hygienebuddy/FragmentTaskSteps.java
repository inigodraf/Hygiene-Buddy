package com.example.hygienebuddy;

import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
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
    private TextView tvTaskTitle, tvStepProgress, tvInstruction, tvMinutes, tvSeconds, tvNoVideo;
    private ImageView ivStepImage;
    private ProgressBar progressStep;
    private Button btnNext;
    private VideoView videoViewTask;

    // Step data
    private List<TaskStep> steps;
    private int currentStepIndex = 0;

    // Timer
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long remainingTimeMillis = 0;

    // Task type (passed via Bundle)
    private String taskType;

    // Video management
    private VideoManager videoManager;

    private Button btnQuiz;
    private Button btnHome;

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
        tvNoVideo = view.findViewById(R.id.tvNoVideo);
        ivStepImage = view.findViewById(R.id.ivStepImage);
        progressStep = view.findViewById(R.id.progressStep);
        btnNext = view.findViewById(R.id.btnNext);
        btnQuiz = view.findViewById(R.id.btnQuiz);
        btnHome = view.findViewById(R.id.btnHome);
        videoViewTask = view.findViewById(R.id.videoViewTask);

        // Initialize VideoManager
        videoManager = new VideoManager(requireContext());

        // Get taskType from bundle
        if (getArguments() != null) {
            taskType = getArguments().getString("taskType", "toothbrushing");
        }

        // Configure VideoView to remove default controls and center content
        setupVideoView();

        // Load steps and display the first one
        loadSteps(taskType);
        showStep(currentStepIndex);

        // "Next" button
        btnNext.setOnClickListener(v -> goToNextStep());

        btnQuiz.setOnClickListener(v -> navigateToQuiz());
        btnHome.setOnClickListener(v -> navigateToHome());
    }

    /** Configure VideoView settings */
    private void setupVideoView() {
        // Remove default media controls
        videoViewTask.setMediaController(null);

        // VideoView will automatically center the video content
        // within its container due to the FrameLayout gravity="center"
    }

    /** Loads task steps based on selected type */
    private void loadSteps(String type) {
        steps = new ArrayList<>();

        if (type.equals("toothbrushing")) {
            tvTaskTitle.setText("Toothbrushing Routine");
            steps.add(new TaskStep(1, "Wet your toothbrush with water.", R.drawable.ic_toothbrush, 0, 15, "toothbrushing"));
            steps.add(new TaskStep(2, "Apply toothpaste — about the size of a pea.", R.drawable.ic_toothbrush, 0, 15, "toothbrushing"));
            steps.add(new TaskStep(3, "Brush your teeth gently for 2 minutes.", R.drawable.ic_toothbrush, 2, 0, "toothbrushing"));
            steps.add(new TaskStep(4, "Rinse your mouth and toothbrush.", R.drawable.ic_toothbrush, 0, 20, "toothbrushing"));
        } else if (type.equals("handwashing")) {
            tvTaskTitle.setText("Handwashing Routine");
            steps.add(new TaskStep(1, "Identify the necessary materials to be used (soap, water, towel).", R.drawable.ic_handwashing, 0, 5, "handwashing"));
            steps.add(new TaskStep(2, "Turn on the faucet using your dominant hand.", R.drawable.ic_handwashing, 0, 5, "handwashing"));
            steps.add(new TaskStep(3, "Wet your hands under the running water.", R.drawable.ic_handwashing, 0, 10, "handwashing"));
            steps.add(new TaskStep(4, "Turn off the faucet to save water.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
            steps.add(new TaskStep(5, "Get the soap with your dominant hand from the soap dish.", R.drawable.ic_handwashing, 0, 5, "handwashing"));
            steps.add(new TaskStep(6, "Rub your hands together to create a rich lather.", R.drawable.ic_handwashing, 0, 5, "handwashing"));
            steps.add(new TaskStep(7, "Scrub all parts of your hands, including between your fingers and under your nails.", R.drawable.ic_handwashing, 0, 20, "handwashing"));
            steps.add(new TaskStep(8, "Turn on the faucet again.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
            steps.add(new TaskStep(9, "Rinse your hands thoroughly under running water.", R.drawable.ic_handwashing, 0, 10, "handwashing"));
            steps.add(new TaskStep(10, "Turn off the faucet using your dominant hand.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
            steps.add(new TaskStep(11, "Shake your hands gently to remove excess water.", R.drawable.ic_handwashing, 0, 5, "handwashing"));
            steps.add(new TaskStep(12, "Pick up the towel using your dominant hand.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
            steps.add(new TaskStep(13, "Dry your hands thoroughly with the towel.", R.drawable.ic_handwashing, 0, 10, "handwashing"));
            steps.add(new TaskStep(14, "Return the towel to its proper place.", R.drawable.ic_handwashing, 0, 3, "handwashing"));
        }

        progressStep.setMax(steps.size());
    }

    /** Displays the current step's data and loads the correct video */
    private void showStep(int index) {
        if (index < 0 || index >= steps.size()) return;
        TaskStep current = steps.get(index);

        tvStepProgress.setText(String.format(Locale.getDefault(), "Step %d of %d", current.getStepNumber(), steps.size()));
        tvInstruction.setText(current.getInstruction());
        progressStep.setProgress(current.getStepNumber());

        // Try to load the custom video for this step
        boolean videoLoaded = loadCustomVideo(taskType, current.getStepNumber());

        // Fallback to image if no video exists
        if (!videoLoaded) {
            videoViewTask.setVisibility(View.GONE);
            tvNoVideo.setVisibility(View.VISIBLE);
            ivStepImage.setVisibility(View.VISIBLE);
            ivStepImage.setImageResource(current.getImageResId());
        } else {
            // Video loaded successfully - hide image and show video
            ivStepImage.setVisibility(View.GONE);
            tvNoVideo.setVisibility(View.GONE);
            videoViewTask.setVisibility(View.VISIBLE);

            // Auto-play the video
            autoPlayVideo();
        }

        // Start timer
        startTimer((current.getMinutes() * 60L + current.getSeconds()) * 1000);

        // Change button text for last step
        btnNext.setText(index == steps.size() - 1 ? "Finish" : "Next");
    }

    /** Moves to the next step or finishes the routine */
    private void goToNextStep() {
        if (countDownTimer != null) countDownTimer.cancel();

        // Stop current video playback
        if (videoViewTask != null) {
            videoViewTask.stopPlayback();
        }

        if (currentStepIndex < steps.size() - 1) {
            currentStepIndex++;
            showStep(currentStepIndex);
        } else {
            tvInstruction.setText("Great job! You've completed the task!");
            tvStepProgress.setText("Task Completed");
            btnNext.setVisibility(View.GONE);
            btnQuiz.setVisibility(View.VISIBLE);
            btnHome.setVisibility(View.VISIBLE);
            ivStepImage.setImageResource(R.drawable.ic_placeholder_video);
            videoViewTask.setVisibility(View.GONE);
            tvNoVideo.setVisibility(View.GONE);
        }
    }

    private void navigateToQuiz() {
        // Replace with your actual quiz fragment class
        FragmentQuiz fragmentQuiz = new FragmentQuiz();

        // Pass any necessary data to the quiz fragment
        Bundle args = new Bundle();
        args.putString("taskType", taskType);
        fragmentQuiz.setArguments(args);

        // Perform fragment transaction
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragmentQuiz)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToHome() {
        HomeDashboardFragment homeFragment = new HomeDashboardFragment();

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .addToBackStack(null)
                .commit();
    }

    /** Loads a custom uploaded video if it exists for the specific task step */
    private boolean loadCustomVideo(String taskType, int stepNumber) {
        try {
            // Get the raw file for VideoView (VideoView works better with file paths)
            File videoFile = videoManager.getStepVideoFile(taskType, stepNumber);

            if (videoFile != null && videoFile.exists()) {
                Uri videoUri = Uri.fromFile(videoFile);
                videoViewTask.setVideoURI(videoUri);

                // Set up video completion listener for looping
                videoViewTask.setOnCompletionListener(mp -> {
                    // Loop the video automatically
                    videoViewTask.start();
                });

                // Set up error listener for debugging
                videoViewTask.setOnErrorListener((mp, what, extra) -> {
                    Log.e("VideoView", "Video error: what=" + what + ", extra=" + extra);
                    Toast.makeText(getContext(), "Error loading video", Toast.LENGTH_SHORT).show();
                    // Fallback to image on error
                    fallbackToImage();
                    return true;
                });

                // Set up prepared listener
                videoViewTask.setOnPreparedListener(mp -> {
                    Log.d("VideoView", "Video prepared successfully");
                    videoViewTask.setVisibility(View.VISIBLE);
                    tvNoVideo.setVisibility(View.GONE);
                    ivStepImage.setVisibility(View.GONE);

                    // Video is centered automatically by the FrameLayout
                    // No play button needed since we auto-play
                });

                return true;
            } else {
                Log.d("VideoView", "No video file found for " + taskType + " step " + stepNumber);
            }
        } catch (Exception e) {
            Log.e("VideoView", "Error loading video: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /** Automatically plays the video when step is loaded */
    private void autoPlayVideo() {
        if (videoViewTask != null && videoViewTask.getVisibility() == View.VISIBLE) {
            videoViewTask.start();
            // No play button to hide since we removed it
        }
    }

    /** Fallback to image when video fails to load */
    private void fallbackToImage() {
        if (currentStepIndex >= 0 && currentStepIndex < steps.size()) {
            TaskStep current = steps.get(currentStepIndex);
            videoViewTask.setVisibility(View.GONE);
            tvNoVideo.setVisibility(View.VISIBLE);
            ivStepImage.setVisibility(View.VISIBLE);
            ivStepImage.setImageResource(current.getImageResId());
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
        if (videoViewTask != null) videoViewTask.stopPlayback();
    }
}