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

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    // Quiz elements
    private ImageView image1, image2, image3, image4;
    private TextView instructionText, resultText;
    private Button resetButton, homeButton;
    private View celebrationOverlay;

    // Quiz logic
    private List<Integer> correctOrder = Arrays.asList(1, 2, 3, 4);
    private List<Integer> userSequence = new ArrayList<>();
    private List<Integer> currentImagePositions = new ArrayList<>();

    // Image resources
    private int[] imageResources = {
            R.drawable.app_logo,
            R.drawable.ic_add,
            R.drawable.ic_data,
            R.drawable.ic_checklist
    };

    public FragmentQuiz() {
        // Required empty public constructor
    }

    public static FragmentQuiz newInstance(String param1, String param2) {
        FragmentQuiz fragment = new FragmentQuiz();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        initializeViews(view);
        setupClickListeners();
        resetQuiz();

        return view;
    }

    private void initializeViews(View view) {
        image1 = view.findViewById(R.id.image1);
        image2 = view.findViewById(R.id.image2);
        image3 = view.findViewById(R.id.image3);
        image4 = view.findViewById(R.id.image4);
        instructionText = view.findViewById(R.id.instructionText);
        resultText = view.findViewById(R.id.resultText);
        resetButton = view.findViewById(R.id.resetButton);
        homeButton = view.findViewById(R.id.homeButton);
        celebrationOverlay = view.findViewById(R.id.celebrationOverlay);
    }

    private void setupClickListeners() {
        image1.setOnClickListener(v -> onImageClicked(0));
        image2.setOnClickListener(v -> onImageClicked(1));
        image3.setOnClickListener(v -> onImageClicked(2));
        image4.setOnClickListener(v -> onImageClicked(3));

        resetButton.setOnClickListener(v -> resetQuiz());
        homeButton.setOnClickListener(v -> navigateToHome());
    }

    private void shuffleImages() {
        List<Integer> imageIndices = new ArrayList<>();
        for (int i = 0; i < imageResources.length; i++) {
            imageIndices.add(i);
        }
        Collections.shuffle(imageIndices);

        image1.setImageResource(imageResources[imageIndices.get(0)]);
        image2.setImageResource(imageResources[imageIndices.get(1)]);
        image3.setImageResource(imageResources[imageIndices.get(2)]);
        image4.setImageResource(imageResources[imageIndices.get(3)]);

        // Reset all borders to normal
        resetAllBorders();

        currentImagePositions.clear();
        for (int i = 0; i < imageIndices.size(); i++) {
            currentImagePositions.add(imageIndices.get(i) + 1);
        }
    }

    private void onImageClicked(int position) {
        if (userSequence.contains(position)) {
            Toast.makeText(getContext(), "You already clicked this image!", Toast.LENGTH_SHORT).show();
            return;
        }

        userSequence.add(position);
        highlightImage(position);

        if (userSequence.size() == correctOrder.size()) {
            checkSequence();
        } else {
            instructionText.setText("Selected " + userSequence.size() + " of 4 images\nClick next image in sequence");
        }
    }

    private void highlightImage(int position) {
        int highlightColor = R.drawable.image_border_selected;

        switch (position) {
            case 0:
                image1.setBackgroundResource(highlightColor);
                break;
            case 1:
                image2.setBackgroundResource(highlightColor);
                break;
            case 2:
                image3.setBackgroundResource(highlightColor);
                break;
            case 3:
                image4.setBackgroundResource(highlightColor);
                break;
        }
    }

    private void checkSequence() {
        List<Integer> userImageSequence = new ArrayList<>();
        for (int position : userSequence) {
            userImageSequence.add(currentImagePositions.get(position));
        }

        if (userImageSequence.equals(correctOrder)) {
            showCelebration();
            resultText.setText("Correct! Good job!");
            resultText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            showWrongSequence();
            resultText.setText("Wrong sequence! Try again.");
            resultText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        resetButton.setVisibility(View.VISIBLE);
        instructionText.setText("Sequence complete!");
    }

    private void showCelebration() {
        // Show celebration overlay
        celebrationOverlay.setVisibility(View.VISIBLE);

        // Set all correct images to green border
        for (int i = 0; i < userSequence.size(); i++) {
            int position = userSequence.get(i);
            switch (position) {
                case 0:
                    image1.setBackgroundResource(R.drawable.image_border_correct);
                    break;
                case 1:
                    image2.setBackgroundResource(R.drawable.image_border_correct);
                    break;
                case 2:
                    image3.setBackgroundResource(R.drawable.image_border_correct);
                    break;
                case 3:
                    image4.setBackgroundResource(R.drawable.image_border_correct);
                    break;
            }
        }

        // Hide celebration after 3 seconds
        celebrationOverlay.postDelayed(() -> {
            celebrationOverlay.setVisibility(View.GONE);
        }, 3000);
    }

    private void showWrongSequence() {
        // Set all images to red border
        for (int position : userSequence) {
            switch (position) {
                case 0:
                    image1.setBackgroundResource(R.drawable.image_border_wrong);
                    break;
                case 1:
                    image2.setBackgroundResource(R.drawable.image_border_wrong);
                    break;
                case 2:
                    image3.setBackgroundResource(R.drawable.image_border_wrong);
                    break;
                case 3:
                    image4.setBackgroundResource(R.drawable.image_border_wrong);
                    break;
            }
        }

        // Flash the result text
        resultText.setAlpha(0f);
        resultText.animate().alpha(1f).setDuration(500).start();
    }

    private void resetAllBorders() {
        image1.setBackgroundResource(R.drawable.image_border);
        image2.setBackgroundResource(R.drawable.image_border);
        image3.setBackgroundResource(R.drawable.image_border);
        image4.setBackgroundResource(R.drawable.image_border);
    }

    private void resetQuiz() {
        userSequence.clear();
        resultText.setText("");
        shuffleImages();
        resetButton.setVisibility(View.GONE);
        celebrationOverlay.setVisibility(View.GONE);
        instructionText.setText("Click the images in the correct order!");
    }

    private void navigateToHome() {
        // Navigate back to HomeDashboardFragment
        Fragment homeFragment = new HomeDashboardFragment();
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, homeFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}