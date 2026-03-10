package com.example.hygienebuddy;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.res.Resources;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class FragmentTaskSteps extends Fragment {

    // UI elements
// UI elements

    private FrameLayout layoutMediaContainer; // Add this line
    private TextView tvTaskTitle, tvStepProgress, tvInstruction, tvNoVideo;
    private ImageView ivStepImage, btnExit; // Added btnExit here
    private ImageView btnSpeaker;
    private ImageButton btnLangToggle;
    private ProgressBar progressStep;
    private Button btnNext, btnQuiz, btnHome, btnBack; // Added btnBack here
    private VideoView videoViewTask;

    // Eye tracking UI
    private EyeTrackerHelper eyeTrackerHelper;
    private PreviewView eyeTrackerPreview;
    private GraphicOverlay graphicOverlay;
    private TextView tvFocusWarning;

    // Step data
    private List<TaskStep> steps;
    private int currentStepIndex = 0;

    // Task type
    private String taskType;

    // Video management
    private VideoManager videoManager;

    // Audio
    private MediaPlayer voicePlayer;
    private boolean isVoicePaused = false;
    private boolean attentionPlayedThisStep = false; // prevent spam
    private long lastAttentionAtMs = 0L; // cooldown across rapid look-away events
    private static final long ATTENTION_COOLDOWN_MS = 6000L;

    // Step-voice looping (4s gap)
    private Handler voiceLoopHandler = new Handler(Looper.getMainLooper());
    private Runnable voiceLoopRunnable = null;
    private File lastStepVoiceFile = null;
    // Add layoutCameraPreview here!
    private View layoutCameraPreview;


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

        // Bind UI
        tvTaskTitle = view.findViewById(R.id.tvTaskTitle);
        tvStepProgress = view.findViewById(R.id.tvStepProgress);
        tvInstruction = view.findViewById(R.id.tvInstruction);
        tvNoVideo = view.findViewById(R.id.tvNoVideo);
        ivStepImage = view.findViewById(R.id.ivStepImage);
        btnSpeaker = view.findViewById(R.id.btnSpeaker);
        btnLangToggle = view.findViewById(R.id.btnLangToggle);
        progressStep = view.findViewById(R.id.progressStep);
        btnNext = view.findViewById(R.id.btnNext);
        btnBack = view.findViewById(R.id.btnBack);
        btnQuiz = view.findViewById(R.id.btnQuiz);
        btnHome = view.findViewById(R.id.btnHome);
        videoViewTask = view.findViewById(R.id.videoViewTask);
        btnExit = view.findViewById(R.id.btnExit);
        layoutMediaContainer = view.findViewById(R.id.layoutMediaContainer);
        layoutCameraPreview = view.findViewById(R.id.layoutCameraPreview);

        // Eye tracking
        eyeTrackerPreview = view.findViewById(R.id.eyeTrackerPreview);
        graphicOverlay = view.findViewById(R.id.graphicOverlay);
        tvFocusWarning = view.findViewById(R.id.tvFocusWarning);

        videoManager = new VideoManager(requireContext());

        // Get task type from arguments
        if (getArguments() != null)
            taskType = getArguments().getString("taskType", "toothbrushing");

        setupVideoView();

        // Load steps
        reloadLocalizedResourcesPreserveIndex(false);

        // Buttons
        btnNext.setOnClickListener(v -> goToNextStep());
        btnQuiz.setOnClickListener(v -> navigateToQuiz());
        btnHome.setOnClickListener(v -> navigateToHome());
        btnBack.setOnClickListener(v -> goToPreviousStep());
        btnExit.setOnClickListener(v -> exitTask());

        // Speaker: play/pause current step voice
        btnSpeaker.setOnClickListener(v -> toggleStepVoice());

        // Language toggle
        btnLangToggle.setOnClickListener(v -> {
            String currentLang = LocaleManager.getLanguage(requireContext());
            LocaleManager.setLanguage(requireContext(), currentLang.equals("en") ? "tl" : "en");
            // cancel any pending loops, reload, and auto-play current step (new language)
            cancelVoiceLoop();
            reloadLocalizedResourcesPreserveIndex(true);
            updateLangToggleIcon();
            autoPlayStepVoice();
        });

        updateLangToggleIcon();

        // Adjust media container
        View mediaContainer = view.findViewById(R.id.layoutMediaContainer);
        if (mediaContainer != null) mediaContainer.post(this::resizeMediaContainer);
    }

    /** Configure VideoView (muted & looped) */
    private void setupVideoView() {
        videoViewTask.setMediaController(null);
    }

    /** Load localized task steps */
    private void loadSteps(String type) {
        steps = new ArrayList<>();
        if (type.equals("toothbrushing")) {
            tvTaskTitle.setText(getLocalizedString(R.string.toothbrushing_title));
            String[] arr = getLocalizedResources().getStringArray(R.array.toothbrushing_steps);
            for (int i = 0; i < arr.length; i++) {
                steps.add(new TaskStep(i + 1, arr[i], R.drawable.ic_toothbrush, 0, 0, "toothbrushing"));
            }
        } else if (type.equals("handwashing")) {
            tvTaskTitle.setText(getLocalizedString(R.string.handwashing_title));
            String[] arr = getLocalizedResources().getStringArray(R.array.handwashing_steps);
            for (int i = 0; i < arr.length; i++) {
                steps.add(new TaskStep(i + 1, arr[i], R.drawable.ic_handwashing, 0, 0, "handwashing"));
            }
        }
        progressStep.setMax(steps.size());
    }

    /** Show current step */

    private void goToPreviousStep() {
        if (currentStepIndex > 0) {
            // Clean up current step before moving back
            stopEyeTracking();
            cancelVoiceLoop();
            stopAndReleaseVoice();
            if (videoViewTask != null) videoViewTask.stopPlayback();

            currentStepIndex--;
            showStep(currentStepIndex);
        }
    }

    /** Go to next step */
    private void goToNextStep() {
        if (currentStepIndex < steps.size() - 1) {
            // Standard step navigation
            stopEyeTracking();
            cancelVoiceLoop();
            stopAndReleaseVoice();
            if (videoViewTask != null) videoViewTask.stopPlayback();

            currentStepIndex++;
            showStep(currentStepIndex);
        } else {
            // TRIGGER CELEBRATION HERE
            showCelebrationScreen();
        }
    }

    private void showCelebrationScreen() {
        // 1. CLEANUP: Stop tracking, looping audio, and video immediately to prevent crashes
        stopEyeTracking();
        cancelVoiceLoop();
        stopAndReleaseVoice();
        if (videoViewTask != null) {
            videoViewTask.stopPlayback();
        }

        // 2. Clear UI clutter (Hide instructions, progress, navigation buttons)
        if (tvInstruction != null) tvInstruction.setVisibility(View.GONE);
        if (tvStepProgress != null) tvStepProgress.setVisibility(View.GONE);
        if (progressStep != null) progressStep.setVisibility(View.GONE);
        if (btnBack != null) btnBack.setVisibility(View.GONE);
        if (btnNext != null) btnNext.setVisibility(View.GONE);

        // HIDE TOP BUTTONS: Mute and Language Toggle
        if (btnSpeaker != null) btnSpeaker.setVisibility(View.GONE);
        if (btnLangToggle != null) btnLangToggle.setVisibility(View.GONE);

        // Hide the camera preview layout if it exists
        if (layoutCameraPreview != null) {
            layoutCameraPreview.setVisibility(View.GONE);
        }

        // 3. Configure the Celebration Picture to be LARGER
        if (layoutMediaContainer != null) {
            layoutMediaContainer.setVisibility(View.VISIBLE);

            // Remove the 16:9 video height restriction so the image can grow
            ViewGroup.LayoutParams containerLp = layoutMediaContainer.getLayoutParams();
            containerLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutMediaContainer.setLayoutParams(containerLp);
        }

        if (videoViewTask != null) {
            videoViewTask.setVisibility(View.GONE); // Hide video part
        }

        if (tvNoVideo != null) {
            tvNoVideo.setVisibility(View.GONE); // Hide fallback text
        }

        if (ivStepImage != null) {
            ivStepImage.setVisibility(View.VISIBLE);
            ivStepImage.setImageResource(R.drawable.goodjob);

            // Allow the ImageView to expand to fit the actual image dimensions
            ViewGroup.LayoutParams lp = ivStepImage.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            ivStepImage.setLayoutParams(lp);
            ivStepImage.setAdjustViewBounds(true); // Forces the image to keep its aspect ratio while expanding
            ivStepImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        // 4. Update Title and show Quiz/Home buttons
        if (tvTaskTitle != null) {
            // Changed the text and added a newline (\n) so it stacks nicely
            tvTaskTitle.setText("TASK COMPLETED\nGOOD JOB!");

            // Sized down from 32 to 24 (you can adjust this number to make it even smaller if needed)
            tvTaskTitle.setTextSize(24);
        }

        // HIDE the container holding the Back/Next buttons to save space
        View layoutButtons = getView() != null ? getView().findViewById(R.id.layoutButtons) : null;
        if (layoutButtons != null) {
            layoutButtons.setVisibility(View.GONE);
        }

        // SHOW the Quiz button
        if (btnQuiz != null) {
            btnQuiz.setVisibility(View.VISIBLE);
        }

        // SHOW the Home button (This was missing!)
        if (btnHome != null) {
            btnHome.setVisibility(View.VISIBLE);
        }

        // 5. Play the completion audio!
        playCompletionVoice();
    }
    private void playCelebrationVoice() {
        // Replace with your actual celebration audio resource
        //int celebrationResId = R.raw.voice_celebration;

        // Use your existing voice player logic
        stopAndReleaseVoice();
        //voicePlayer = MediaPlayer.create(requireContext(), celebrationResId);
        voicePlayer.start();
    }

    private void showStep(int index) {
        if (index < 0 || index >= steps.size()) return;
        TaskStep current = steps.get(index);

        // --- 1. VISUAL TRANSITION ---
        // We trigger a quick fade-out/in effect so the user sees the change
        applyStepTransition();

        // --- 2. LOGIC & CLEANUP ---
        attentionPlayedThisStep = false;
        cancelVoiceLoop();
        stopAndReleaseVoice();
        updateLangToggleIcon();

        // --- 3. UI UPDATES ---
        tvStepProgress.setText(String.format(Locale.getDefault(),
                getLocalizedString(R.string.ui_step_of), current.getStepNumber(), steps.size()));
        tvInstruction.setText(current.getInstruction());
        progressStep.setProgress(current.getStepNumber());

        if (btnBack != null) {
            // Change View.GONE to View.INVISIBLE
            btnBack.setVisibility(index == 0 ? View.INVISIBLE : View.VISIBLE);
        }

        // Media Loading
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

        // --- 4. AUDIO & TRACKING ---
        autoPlayStepVoice();
        startEyeTracking();

        // Navigation Text
        btnNext.setText(index == steps.size() - 1 ? getLocalizedString(R.string.ui_finish)
                : getLocalizedString(R.string.ui_next));
    }

    /** Reload localized steps */
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

                // Mute + loop
                videoViewTask.setOnPreparedListener(mp -> {
                    try {
                        mp.setVolume(0f, 0f);    // mute video
                        mp.setLooping(true);     // loop silently
                    } catch (Throwable ignored) {}
                    videoViewTask.setVisibility(View.VISIBLE);
                    tvNoVideo.setVisibility(View.GONE);
                    ivStepImage.setVisibility(View.GONE);
                });

                // We handle looping in onPrepared via mp.setLooping(true)
                videoViewTask.setOnCompletionListener(null);

                videoViewTask.setOnErrorListener((mp, what, extra) -> {
                    fallbackToImage();
                    return true;
                });
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    // Start Eye Tracking
    private void startEyeTracking() {
        if (eyeTrackerHelper != null) eyeTrackerHelper.stop();

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{android.Manifest.permission.CAMERA},
                    101
            );
            return;
        }

        eyeTrackerHelper = new EyeTrackerHelper(requireContext(), getViewLifecycleOwner(),
                new EyeTrackerHelper.EyeTrackerListener() {
                    @Override
                    public void onUserLookAway() {
                        requireActivity().runOnUiThread(() -> {
                            tvFocusWarning.setVisibility(View.VISIBLE);
                            maybePlayAttentionVoice(); // does not loop
                        });
                    }

                    @Override
                    public void onUserLookBack() {
                        requireActivity().runOnUiThread(() -> tvFocusWarning.setVisibility(View.GONE));
                    }
                });

        // Updated version with overlay support
        eyeTrackerHelper.startEyeTracking(eyeTrackerPreview, graphicOverlay);
    }

    private void stopEyeTracking() {
        if (eyeTrackerHelper != null) {
            try {
                // 1. Unbind the camera and analysis first
                eyeTrackerHelper.stop();

                // 2. Clear any active graphic overlays so they don't try to redraw
                if (graphicOverlay != null) {
                    graphicOverlay.clear();
                }
            } catch (Exception e) {
                Log.e("FragmentTaskSteps", "Error stopping EyeTracker: " + e.getMessage());
            } finally {
                // 3. DO NOT set to null if you might need it again quickly,
                // but if you do, ensure all threads are finished.
                eyeTrackerHelper = null;
            }
        }

        // Always hide the warning UI
        if (tvFocusWarning != null) {
            tvFocusWarning.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (videoViewTask != null) videoViewTask.stopPlayback();
        stopEyeTracking();
        cancelVoiceLoop();
        stopAndReleaseVoice();
    }

    private void navigateToQuiz() {
        FragmentQuiz fragmentQuiz = FragmentQuiz.newInstance(taskType);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragmentQuiz)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToHome() {
        HomeDashboardFragment homeFragment = new HomeDashboardFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, homeFragment)
                .addToBackStack(null)
                .commit();
    }

    // VOICE: helpers

    private void toggleStepVoice() {
        if (voicePlayer == null) {
            // ADDED: The user clicked to unmute, so reset the flag and play
            isVoicePaused = false;
            autoPlayStepVoice();
            return;
        }
        if (voicePlayer.isPlaying()) {
            voicePlayer.pause();
            isVoicePaused = true;
            setSpeakerIcon(false);
            cancelVoiceLoop();
        } else {
            try {
                voicePlayer.start();
                isVoicePaused = false;
                setSpeakerIcon(true);
                scheduleNextLoop();
            } catch (IllegalStateException ignored) {
                autoPlayStepVoice();
            }
        }
    }

    private void autoPlayStepVoice() {
        File f = getCurrentStepVoiceFile();
        lastStepVoiceFile = f;

        if (f == null || !f.exists()) {
            setSpeakerIcon(false);
            cancelVoiceLoop();
            return;
        }

        // ADDED: If the user previously muted the app, abort playback!
        if (isVoicePaused) {
            setSpeakerIcon(false);
            return;
        }

        playVoiceFile(f, /*shouldLoop*/true);
    }

    private void maybePlayAttentionVoice() {
        long now = System.currentTimeMillis();
        if (attentionPlayedThisStep && (now - lastAttentionAtMs) < ATTENTION_COOLDOWN_MS) {
            return;
        }
        File f = getAttentionVoiceFile();
        if (f != null && f.exists()) {
            // Attention does NOT loop; cancel current loop temporarily, resume step loop after
            cancelVoiceLoop();
            playVoiceFile(f, /*shouldLoop*/false);
            attentionPlayedThisStep = true;
            lastAttentionAtMs = now;

            // after attention finishes, restart step loop if file exists and not paused
            voiceLoopHandler.postDelayed(() -> {
                if (!isAdded()) return;
                if (!isVoicePaused) autoPlayStepVoice();
            }, 1000); // small grace after attention
        }
    }

    private void playCompletionVoice() {
        File f = getCompletionVoiceFile();
        if (f != null && f.exists()) {
            // Completion does NOT loop; stop step loop
            cancelVoiceLoop();
            playVoiceFile(f, /*shouldLoop*/false);
        }
    }

    private void playVoiceFile(File file, boolean shouldLoop) {
        stopAndReleaseVoice(); // stop current
        try {
            voicePlayer = new MediaPlayer();
            voicePlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
            );
            voicePlayer.setDataSource(file.getAbsolutePath());
            voicePlayer.setOnPreparedListener(mp -> {
                mp.start();
                isVoicePaused = false;
                setSpeakerIcon(true);
            });
            voicePlayer.setOnCompletionListener(mp -> {
                setSpeakerIcon(false);
                if (shouldLoop) {
                    // schedule replay of the LAST step file after 4 seconds
                    scheduleNextLoop();
                }
            });
            voicePlayer.setOnErrorListener((mp, what, extra) -> {
                setSpeakerIcon(false);
                cancelVoiceLoop();
                Toast.makeText(requireContext(), "Audio error", Toast.LENGTH_SHORT).show();
                return true;
            });
            voicePlayer.prepareAsync();
        } catch (IOException e) {
            setSpeakerIcon(false);
            cancelVoiceLoop();
            Toast.makeText(requireContext(), "Cannot play audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleNextLoop() {
        cancelVoiceLoop();
        if (lastStepVoiceFile == null || !lastStepVoiceFile.exists()) return;
        if (!isAdded() || isVoicePaused) return;

        voiceLoopRunnable = () -> {
            if (!isAdded() || isVoicePaused) return;
            // re-verify current step file (in case language/step changed)
            File current = getCurrentStepVoiceFile();
            lastStepVoiceFile = current;
            if (current != null && current.exists()) {
                playVoiceFile(current, /*shouldLoop*/true);
            }
        };
        voiceLoopHandler.postDelayed(voiceLoopRunnable, 4000); // 4 seconds gap
    }

    private void cancelVoiceLoop() {
        if (voiceLoopRunnable != null) {
            voiceLoopHandler.removeCallbacks(voiceLoopRunnable);
            voiceLoopRunnable = null;
        }
    }

    private void stopAndReleaseVoice() {
        if (voicePlayer != null) {
            try {
                voicePlayer.stop();
            } catch (IllegalStateException ignored) {}
            voicePlayer.release();
            voicePlayer = null;
        }
        // REMOVED: isVoicePaused = false; <-- This was causing the reset!
        setSpeakerIcon(!isVoicePaused); // Keep icon consistent with the saved state
    }

    private void setSpeakerIcon(boolean playing) {
        // swap icon if you have separate drawables; otherwise just keep one
        btnSpeaker.setAlpha(playing ? 1f : 0.6f);
    }

    private String resolveLangCodeSuffix() {
        // LocaleManager: "en" or "tl"
        String lang = LocaleManager.getLanguage(requireContext());
        return "en".equals(lang) ? "ENG" : "PH";
    }

    private File getCurrentStepVoiceFile() {
        if (currentStepIndex < 0 || currentStepIndex >= steps.size()) return null;
        TaskStep current = steps.get(currentStepIndex);
        String lang = resolveLangCodeSuffix();
        String prefix = "handwashing".equals(taskType) ? "HWSteps" : "TBSteps";
        String fileName = prefix + current.getStepNumber() + "_" + lang + ".wav";
        return new File(requireContext().getFilesDir(), "tts_audio/" + fileName);
    }

    private File getAttentionVoiceFile() {
        String lang = resolveLangCodeSuffix();
        String fileName = "Attention_" + lang + ".wav";
        return new File(requireContext().getFilesDir(), "tts_audio/" + fileName);
    }

    private File getCompletionVoiceFile() {
        String lang = resolveLangCodeSuffix();
        String fileName = "Completion_" + lang + ".wav";
        return new File(requireContext().getFilesDir(), "tts_audio/" + fileName);
    }

    private void applyStepTransition() {
        View container = getView() != null ? getView().findViewById(R.id.layoutMediaContainer) : null;
        if (container != null) {
            // Reset position and alpha
            container.setAlpha(0f);
            container.setTranslationX(50f); // Slide in from the right slightly

            // Animate to final position
            container.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(400)
                    .start();
        }

        // Also animate the instruction text so it "pops"
        tvInstruction.setAlpha(0f);
        tvInstruction.animate().alpha(1f).setDuration(600).start();
    }

    private void exitTask() {
        // 1. Stop all active systems
        stopEyeTracking();
        cancelVoiceLoop();
        stopAndReleaseVoice();

        // 2. Stop video
        if (videoViewTask != null) {
            videoViewTask.stopPlayback();
        }

        // 3. Go back to the previous screen (Dashboard)
        if (isAdded()) {
            getParentFragmentManager().popBackStack();
        }
    }
}
