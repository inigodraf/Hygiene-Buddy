package com.example.hygienebuddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class FragmentTasks extends Fragment {

    private CardView cardToothbrushing, cardHandwashing;
    private ImageView btnBack;

    public FragmentTasks() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        cardToothbrushing = view.findViewById(R.id.cardToothbrushing);
        cardHandwashing = view.findViewById(R.id.cardHandwashing);
        btnBack = view.findViewById(R.id.btnBack);

        // Initialize NavController
        NavController navController = Navigation.findNavController(view);

        // Click: Toothbrushing → Task Steps screen
        cardToothbrushing.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("taskType", "toothbrushing"); // optional if you need to identify the task
            navController.navigate(R.id.action_fragmentTasks_to_fragmentTaskSteps, bundle);
        });

        // Click: Handwashing → Task Steps screen
        cardHandwashing.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("taskType", "handwashing");
            navController.navigate(R.id.action_fragmentTasks_to_fragmentTaskSteps, bundle);
        });

        // Click: Back Button → previous screen
        btnBack.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        // Highlight home in bottom nav and setup click listeners
        BottomNavHelper.setupBottomNav(this, "tasks");
    }
}
