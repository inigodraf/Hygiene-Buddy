package com.example.hygienebuddy;

import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
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

import android.content.res.Resources;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FragmentTaskSteps extends Fragment {

    // UI elements
    private TextView tvTaskTitle, tvStepProgress, tvInstruction, tvMinutes, tvSeconds, tvNoVideo;
    private TextView tvMinutesLabel, tvSecondsLabel;
    private ImageView ivStepImage;
    private ImageView btnSpeaker;
    private ImageButton btnLangToggle;
    private ProgressBar progressStep;
    private Button btnNext, btnQuiz, btnHome;
    private VideoView videoViewTask;

    // 👁️ Eye tracker UI
    private EyeTrackerHelper eyeTrackerHelper;
    private SurfaceView eyeTrackerPreview;
    private TextView tvFocusWarning;

    // Step data
    private List<TaskStep> steps;
    private int currentStepIndex = 0;

    // Timer
    private CountDownTimer countDownTimer;
    private long remainingTimeMillis = 0;

    // Task type
    private String taskType;

    // Video management
    private VideoManager videoManager;

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
        tvMinutesLabel = view.findViewById(R.id.tvMinutesLabel);
        tvSecondsLabel = view.findViewById(R.id.tvSecondsLabel);
        tvNoVideo = view.findViewById(R.id.tvNoVideo);
        ivStepImage = view.findViewById(R.id.ivStepImage);
        btnSpeaker = view.findViewById(R.id.btnSpeaker);
        btnLangToggle = view.findViewById(R.id.btnLangToggle);
        progressStep = view.findViewById(R.id.progressStep);
        btnNext = view.findViewById(R.id.btnNext);
        btnQuiz = view.findViewById(R.id.btnQuiz);
        btnHome = view.findViewById(R.id.btnHome);
        videoViewTask = view.findViewById(R.id.videoViewTask);

        // 👁️ Eye tracking UI
        eyeTrackerPreview = view.findViewById(R.id.eyeTrackerPreview);
        tvFocusWarning = view.findViewById(R.id.tvFocusWarning);

        // Initialize VideoManager
        videoManager = new VideoManager(requireContext());

        // Get taskType from bundle
        if (getArguments() != null) {
            taskType = getArguments().getString("taskType", "toothbrushing");
        }

        // Configure VideoView
        setupVideoView();

        // Restore state if available
        if (savedInstanceState != null) {
            taskType = savedInstanceState.getString("taskType", taskType);
            currentStepIndex = savedInstanceState.getInt("currentStepIndex", 0);
            remainingTimeMillis = savedInstanceState.getLong("remainingTimeMillis", 0);
        }

        // Load steps and display using current locale, preserving index if restored
        reloadLocalizedResourcesPreserveIndex(savedInstanceState != null);

        // Buttons
        btnNext.setOnClickListener(v -> goToNextStep());
        btnQuiz.setOnClickListener(v -> navigateToQuiz());
        btnHome.setOnClickListener(v -> navigateToHome());

        // Language toggle
        btnLangToggle.setOnClickListener(v -> {
            String currentLang = LocaleManager.getLanguage(requireContext());
            if ("en".equals(currentLang)) {
                LocaleManager.setLanguage(requireContext(), "tl");
            } else {
                LocaleManager.setLanguage(requireContext(), "en");
            }
            reloadLocalizedResourcesPreserveIndex(true);
            updateLangToggleIcon();
        });

        // Set initial flag icon
        updateLangToggleIcon();

        // Responsive layout fix
        View mediaContainer = view.findViewById(R.id.layoutMediaContainer);
        if (mediaContainer != null) {
            mediaContainer.post(this::resizeMediaContainer);
        }
    }

    /** Configure VideoView */
    private void setupVideoView() {
        videoViewTask.setMediaController(null);
    }

    /** Loads task steps based on selected type using localized resources */
    private void loadSteps(String type) {
        steps = new ArrayList<>();
        if (type.equals("toothbrushing")) {
            tvTaskTitle.setText(getLocalizedString(R.string.toothbrushing_title));
            String[] arr = getLocalizedResources().getStringArray(R.array.toothbrushing_steps);
            for (int i = 0; i < arr.length; i++) {
                int minutes = (i == 2) ? 2 : 0;
                int seconds = (i == 2) ? 0 : (i == 0 ? 15 : (i == 1 ? 15 : 20));
                steps.add(new TaskStep(i + 1, arr[i], R.drawable.ic_toothbrush, minutes, seconds, "toothbrushing"));
            }
        } else if (type.equals("handwashing")) {
            tvTaskTitle.setText(getLocalizedString(R.string.handwashing_title));
            String[] arr = getLocalizedResources().getStringArray(R.array.handwashing_steps);
            int[] secondsByIndex = new int[] {5,5,10,3,5,5,20,3,10,3,5,3,10,3};
            for (int i = 0; i < arr.length; i++) {
                int minutes = 0;
                int seconds = secondsByIndex[i];
                steps.add(new TaskStep(i + 1, arr[i], R.drawable.ic_handwashing, minutes, seconds, "handwashing"));
            }
        }
        progressStep.setMax(steps.size());
    }

    /** Displays the current step */
    private void showStep(int index) {
        if (index < 0 || index >= steps.size()) return;
        TaskStep current = steps.get(index);

        tvStepProgress.setText(String.format(Locale.getDefault(),
                getLocalizedString(R.string.ui_step_of), current.getStepNumber(), steps.size()));
        tvInstruction.setText(current.getInstruction());
        progressStep.setProgress(current.getStepNumber());

        boolean videoLoaded = loadCustomVideo(taskType, current.getStepNumber());
        if (!videoLoaded) {
            videoViewTask.setVisibility(View.GONE);
            tvNoVideo.setVisibility(View.VISIBLE);
            ivStepImage.setVisibility(View.VISIBLE);
            ivStepImage.setImageResource(current.getImageResId());
        } else {
            ivStepImage.setVisibility(View.GONE);
            tvNoVideo.setVisibility(View.GONE);
            videoViewTask.setVisibility(View.VISIBLE);
            autoPlayVideo();
        }

        startTimer((current.getMinutes() * 60L + current.getSeconds()) * 1000);

        // 👁️ Start eye tracking for this step
        startEyeTracking();

        btnNext.setText(index == steps.size() - 1 ? getLocalizedString(R.string.ui_finish)
                : getLocalizedString(R.string.ui_next));
        tvMinutesLabel.setText(getLocalizedString(R.string.ui_minutes));
        tvSecondsLabel.setText(getLocalizedString(R.string.ui_seconds));
    }

    /** Next step */
    private void goToNextStep() {
        stopEyeTracking(); // 👁️ Stop tracking before switching
        if (countDownTimer != null) countDownTimer.cancel();
        if (videoViewTask != null) videoViewTask.stopPlayback();

        if (currentStepIndex < steps.size() - 1) {
            currentStepIndex++;
            showStep(currentStepIndex);
        } else {
            tvInstruction.setText(getLocalizedString(R.string.ui_great_job));
            tvStepProgress.setText(getLocalizedString(R.string.ui_task_completed));
            btnNext.setVisibility(View.GONE);
            btnQuiz.setVisibility(View.VISIBLE);
            btnHome.setVisibility(View.VISIBLE);
            ivStepImage.setImageResource(R.drawable.ic_placeholder_video);
            videoViewTask.setVisibility(View.GONE);
            tvNoVideo.setVisibility(View.GONE);
        }
    }

    private void reloadLocalizedResourcesPreserveIndex(boolean preserveIndex) {
        loadSteps(taskType);
        if (!preserveIndex) currentStepIndex = 0;
        else if (currentStepIndex >= steps.size()) currentStepIndex = steps.size() - 1;
        showStep(currentStepIndex);
    }

    private Resources getLocalizedResources() {
        return LocaleManager.getLocalizedResources(requireContext());
    }

    private String getLocalizedString(int resId) {
        return getLocalizedResources().getString(resId);
    }

    private void updateLangToggleIcon() {
        String lang = LocaleManager.getLanguage(requireContext());
        btnLangToggle.setImageResource("tl".equals(lang) ? R.drawable.ic_flag_ph : R.drawable.ic_flag_us);
    }

    private boolean loadCustomVideo(String taskType, int stepNumber) {
        try {
            File videoFile = videoManager.getStepVideoFile(taskType, stepNumber);
            if (videoFile != null && videoFile.exists()) {
                Uri videoUri = Uri.fromFile(videoFile);
                videoViewTask.setVideoURI(videoUri);
                videoViewTask.setOnCompletionListener(mp -> videoViewTask.start());
                videoViewTask.setOnErrorListener((mp, what, extra) -> { fallbackToImage(); return true; });
                videoViewTask.setOnPreparedListener(mp -> {
                    videoViewTask.setVisibility(View.VISIBLE);
                    tvNoVideo.setVisibility(View.GONE);
                    ivStepImage.setVisibility(View.GONE);
                });
                return true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private void autoPlayVideo() {
        if (videoViewTask != null && videoViewTask.getVisibility() == View.VISIBLE) videoViewTask.start();
    }

    private void fallbackToImage() {
        if (currentStepIndex >= 0 && currentStepIndex < steps.size()) {
            TaskStep current = steps.get(currentStepIndex);
            videoViewTask.setVisibility(View.GONE);
            tvNoVideo.setVisibility(View.VISIBLE);
            ivStepImage.setVisibility(View.VISIBLE);
            ivStepImage.setImageResource(current.getImageResId());
        }
    }

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
    }

    private void resizeMediaContainer() {
        View container = getView() != null ? getView().findViewById(R.id.layoutMediaContainer) : null;
        if (container == null) return;
        int width = container.getWidth();
        if (width == 0) {
            container.post(this::resizeMediaContainer);
            return;
        }
        int height = Math.max((int) (width * 9f / 16f), 200);
        ViewGroup.LayoutParams lp = container.getLayoutParams();
        lp.height = height;
        container.setLayoutParams(lp);
    }

    // 👁️ Start/Stop Eye Tracking
    private void startEyeTracking() {
        if (eyeTrackerHelper != null) {
            eyeTrackerHelper.stop();
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{android.Manifest.permission.CAMERA},
                    101
            );
            return;
        }

        eyeTrackerHelper = new EyeTrackerHelper(requireContext(), getViewLifecycleOwner(), new EyeTrackerHelper.EyeTrackerListener() {
            @Override
            public void onUserLookAway() {
                requireActivity().runOnUiThread(() -> tvFocusWarning.setVisibility(View.VISIBLE));
            }

            @Override
            public void onUserLookBack() {
                requireActivity().runOnUiThread(() -> tvFocusWarning.setVisibility(View.GONE));
            }
        });

        eyeTrackerHelper.startEyeTracking(eyeTrackerPreview);
    }

    private void stopEyeTracking() {
        if (eyeTrackerHelper != null) {
            eyeTrackerHelper.stop();
            eyeTrackerHelper = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) countDownTimer.cancel();
        if (videoViewTask != null) videoViewTask.stopPlayback();
        stopEyeTracking();
    }

    private void navigateToQuiz() {
        FragmentQuiz fragmentQuiz = new FragmentQuiz();
        Bundle args = new Bundle();
        args.putString("taskType", taskType);
        fragmentQuiz.setArguments(args);
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
}
