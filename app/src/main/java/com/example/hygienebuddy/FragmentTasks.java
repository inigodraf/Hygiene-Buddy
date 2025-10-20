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
import androidx.fragment.app.FragmentActivity;

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
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        cardToothbrushing = view.findViewById(R.id.cardToothbrushing);
        cardHandwashing = view.findViewById(R.id.cardHandwashing);
        btnBack = view.findViewById(R.id.btnBack);

        // Click: Toothbrushing → Task Steps screen
        cardToothbrushing.setOnClickListener(v -> openTaskSteps("toothbrushing"));

        // Click: Handwashing → Task Steps screen
        cardHandwashing.setOnClickListener(v -> openTaskSteps("handwashing"));

        // Click: Back Button → previous screen
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // ✅ Set up bottom navigation *after* view is attached
        view.post(() -> BottomNavHelper.setupBottomNav(this, "tasks"));
    }

    /** Opens the Task Steps screen manually using FragmentManager */
    private void openTaskSteps(String taskType) {
        FragmentTaskSteps fragment = new FragmentTaskSteps();
        Bundle bundle = new Bundle();
        bundle.putString("taskType", taskType);
        fragment.setArguments(bundle);

        FragmentActivity activity = requireActivity();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment) // ✅ use fragment_container
                .addToBackStack(null)
                .commit();
    }
}
