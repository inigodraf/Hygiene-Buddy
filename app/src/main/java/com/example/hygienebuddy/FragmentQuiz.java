package com.example.hygienebuddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FragmentQuiz extends Fragment {

    private static final String ARG_TASK_TYPE = "taskType";

    // Quiz elements
    private ImageView image1, image2, image3, image4;
    private TextView questionText, resultText;
    private Button resetButton, homeButton;
    private View celebrationOverlay;

    // Quiz logic
    private int correctAnswerIndex = -1;
    private boolean answerSelected = false;
    private List<Integer> currentImageResources = new ArrayList<>();

    // Task type
    private String taskType;

    // Questions and images for different tasks
    private class QuizQuestion {
        String question;
        int correctImageRes;
        int[] optionImageRes;

        QuizQuestion(String question, int correctImageRes, int[] optionImageRes) {
            this.question = question;
            this.correctImageRes = correctImageRes;
            this.optionImageRes = optionImageRes;
        }
    }

    // Quiz questions for different tasks
    private List<QuizQuestion> toothbrushingQuestions = Arrays.asList(
            new QuizQuestion("Which image shows a toothbrush?",
                    R.drawable.ic_toothbrush_correct,
                    new int[]{R.drawable.ic_toothbrush_wrong1, R.drawable.ic_toothbrush_wrong2, R.drawable.ic_toothbrush_wrong3, R.drawable.ic_toothbrush_correct}),
            new QuizQuestion("Which image shows toothpaste?",
                    R.drawable.ic_toothpaste_correct,
                    new int[]{R.drawable.ic_toothpaste_wrong1, R.drawable.ic_toothpaste_correct, R.drawable.ic_toothpaste_wrong2, R.drawable.ic_toothpaste_wrong3})
    );

    private List<QuizQuestion> handwashingQuestions = Arrays.asList(
            new QuizQuestion("Which image shows a faucet?",
                    R.drawable.ic_faucet_correct,
                    new int[]{R.drawable.ic_faucet_wrong1, R.drawable.ic_faucet_wrong2, R.drawable.ic_faucet_correct, R.drawable.ic_faucet_wrong3}),
            new QuizQuestion("Which image shows soap?",
                    R.drawable.ic_soap_correct,
                    new int[]{R.drawable.ic_soap_wrong1, R.drawable.ic_soap_correct, R.drawable.ic_soap_wrong2, R.drawable.ic_soap_wrong3})
    );

    // Default images as fallback
    private int[] defaultImages = {
            R.drawable.app_logo,
            R.drawable.app_logo,
            R.drawable.app_logo,
            R.drawable.app_logo
    };

    private QuizQuestion currentQuestion;

    public FragmentQuiz() {
        // Required empty public constructor
    }

    public static FragmentQuiz newInstance(String taskType) {
        FragmentQuiz fragment = new FragmentQuiz();
        Bundle args = new Bundle();
        args.putString(ARG_TASK_TYPE, taskType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            taskType = getArguments().getString(ARG_TASK_TYPE, "toothbrushing");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        initializeViews(view);
        setupClickListeners();
        loadNewQuestion();

        return view;
    }

    private void initializeViews(View view) {
        image1 = view.findViewById(R.id.image1);
        image2 = view.findViewById(R.id.image2);
        image3 = view.findViewById(R.id.image3);
        image4 = view.findViewById(R.id.image4);
        questionText = view.findViewById(R.id.questionText);
        resultText = view.findViewById(R.id.resultText);
        resetButton = view.findViewById(R.id.resetButton);
        homeButton = view.findViewById(R.id.homeButton);
        celebrationOverlay = view.findViewById(R.id.celebrationOverlay);
    }

    private List<QuizQuestion> getQuestionsForTask() {
        if ("handwashing".equals(taskType)) {
            return handwashingQuestions;
        } else { // Default to toothbrushing
            return toothbrushingQuestions;
        }
    }

    private void setupClickListeners() {
        image1.setOnClickListener(v -> onImageClicked(0));
        image2.setOnClickListener(v -> onImageClicked(1));
        image3.setOnClickListener(v -> onImageClicked(2));
        image4.setOnClickListener(v -> onImageClicked(3));

        resetButton.setOnClickListener(v -> resetQuiz());
        homeButton.setOnClickListener(v -> navigateToHome());
    }

    private void loadNewQuestion() {
        List<QuizQuestion> questions = getQuestionsForTask();
        if (questions.isEmpty()) {
            // Fallback if no questions are defined
            setupFallbackQuestion();
            return;
        }

        // Select a random question
        Collections.shuffle(questions);
        currentQuestion = questions.get(0);

        questionText.setText(currentQuestion.question);

        // Shuffle the images
        List<Integer> imageOptions = new ArrayList<>();
        for (int resId : currentQuestion.optionImageRes) {
            imageOptions.add(resId);
        }
        Collections.shuffle(imageOptions);

        // Set images to ImageViews
        image1.setImageResource(imageOptions.get(0));
        image2.setImageResource(imageOptions.get(1));
        image3.setImageResource(imageOptions.get(2));
        image4.setImageResource(imageOptions.get(3));

        // Find which index has the correct answer
        correctAnswerIndex = imageOptions.indexOf(currentQuestion.correctImageRes);
        currentImageResources = imageOptions;

        // Reset UI state
        answerSelected = false;
        resetAllBorders();
        resultText.setText("");
        resetButton.setVisibility(View.GONE);
        celebrationOverlay.setVisibility(View.GONE);
    }

    private void setupFallbackQuestion() {
        // Fallback question if no specific questions are defined
        currentQuestion = new QuizQuestion(
                "Select the correct image!",
                R.drawable.app_logo,
                defaultImages
        );

        questionText.setText(currentQuestion.question);

        List<Integer> imageOptions = new ArrayList<>();
        for (int resId : defaultImages) {
            imageOptions.add(resId);
        }
        Collections.shuffle(imageOptions);

        image1.setImageResource(imageOptions.get(0));
        image2.setImageResource(imageOptions.get(1));
        image3.setImageResource(imageOptions.get(2));
        image4.setImageResource(imageOptions.get(3));

        correctAnswerIndex = imageOptions.indexOf(R.drawable.app_logo);
        currentImageResources = imageOptions;
    }

    private void onImageClicked(int position) {
        if (answerSelected) {
            Toast.makeText(getContext(), "Question already answered! Click 'Try Again' for new question.", Toast.LENGTH_SHORT).show();
            return;
        }

        answerSelected = true;

        if (position == correctAnswerIndex) {
            // Correct answer
            showCelebration();
            resultText.setText("Correct! Well done!");
            resultText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            highlightCorrectAnswer();

        } else {
            // Wrong answer
            resultText.setText("Wrong answer! Try again.");
            resultText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            highlightWrongAnswer(position);
            highlightCorrectAnswer();
        }

        resetButton.setVisibility(View.VISIBLE);
        resetButton.setText("Next Question");
    }

    private ImageView getImageViewByIndex(int index) {
        switch (index) {
            case 0: return image1;
            case 1: return image2;
            case 2: return image3;
            case 3: return image4;
            default: return null;
        }
    }

    private void highlightCorrectAnswer() {
        ImageView correctImageView = getImageViewByIndex(correctAnswerIndex);
        if (correctImageView != null) {
            // Get the parent FrameLayout and set its background
            View parent = (View) correctImageView.getParent();
            if (parent != null) {
                parent.setBackgroundResource(R.drawable.image_border_correct);
            }
        }
    }

    private void highlightWrongAnswer(int wrongPosition) {
        ImageView wrongImageView = getImageViewByIndex(wrongPosition);
        if (wrongImageView != null) {
            // Get the parent FrameLayout and set its background
            View parent = (View) wrongImageView.getParent();
            if (parent != null) {
                parent.setBackgroundResource(R.drawable.image_border_wrong);
            }
        }
    }

    private void showCelebration() {
        // Show celebration overlay
        celebrationOverlay.setVisibility(View.VISIBLE);

        // Hide celebration after 2 seconds
        celebrationOverlay.postDelayed(() -> {
            celebrationOverlay.setVisibility(View.GONE);
        }, 2000);
    }

    private void resetAllBorders() {
        for (int i = 0; i < 4; i++) {
            ImageView imageView = getImageViewByIndex(i);
            if (imageView != null) {
                View parent = (View) imageView.getParent();
                if (parent != null) {
                    parent.setBackgroundResource(R.drawable.image_border);
                }
            }
        }
    }

    private void resetQuiz() {
        loadNewQuestion();
    }

    private void navigateToHome() {
        // Navigate back to HomeDashboardFragment
        Fragment homeFragment = new HomeDashboardFragment();
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, homeFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}