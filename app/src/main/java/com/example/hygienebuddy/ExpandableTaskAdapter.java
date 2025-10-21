package com.example.hygienebuddy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for expandable task list with individual step video uploads
 */
public class ExpandableTaskAdapter extends RecyclerView.Adapter<ExpandableTaskAdapter.TaskViewHolder> {

    private Context context;
    private List<TaskData> tasks;
    private Map<String, Boolean> expandedStates;
    private OnStepVideoUploadListener onStepVideoUploadListener;

    public interface OnStepVideoUploadListener {
        void onStepVideoUpload(String taskType, int stepNumber);
    }

    public ExpandableTaskAdapter(Context context, OnStepVideoUploadListener listener) {
        this.context = context;
        this.onStepVideoUploadListener = listener;
        this.tasks = new ArrayList<>();
        this.expandedStates = new HashMap<>();
    }

    public void setTasks(List<TaskData> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expandable_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskData task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivTaskIcon, ivExpandIcon;
        private TextView tvTaskName, tvStepsCount;
        private RecyclerView rvSteps;
        private ViewGroup layoutStepsList;
        private TaskStepAdapter stepAdapter;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            ivTaskIcon = itemView.findViewById(R.id.ivTaskIcon);
            ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvStepsCount = itemView.findViewById(R.id.tvStepsCount);
            rvSteps = itemView.findViewById(R.id.rvSteps);
            layoutStepsList = itemView.findViewById(R.id.layoutStepsList);

            // Setup steps RecyclerView
            stepAdapter = new TaskStepAdapter(context, new TaskStepAdapter.OnStepUploadListener() {
                @Override
                public void onStepUpload(String taskType, int stepNumber) {
                    if (onStepVideoUploadListener != null) {
                        onStepVideoUploadListener.onStepVideoUpload(taskType, stepNumber);
                    }
                }
            });
            rvSteps.setLayoutManager(new LinearLayoutManager(context));
            rvSteps.setAdapter(stepAdapter);

            // Set click listener for expand/collapse
            View headerView = (View) itemView.findViewById(R.id.ivTaskIcon).getParent();
            headerView.setOnClickListener(v -> toggleExpansion(getAdapterPosition()));
        }

        public void bind(TaskData task) {
            tvTaskName.setText(task.getTaskName());
            tvStepsCount.setText(task.getSteps().size() + " steps");

            // Set task icon based on task type
            if (task.getTaskType().equals("handwashing")) {
                ivTaskIcon.setImageResource(R.drawable.ic_handwashing);
            } else if (task.getTaskType().equals("toothbrushing")) {
                ivTaskIcon.setImageResource(R.drawable.ic_toothbrushing);
            }

            // Update step adapter
            stepAdapter.setSteps(task.getSteps(), task.getTaskType());

            // Update expansion state
            boolean isExpanded = expandedStates.getOrDefault(task.getTaskType(), false);
            updateExpansionState(isExpanded);
        }

        private void toggleExpansion(int position) {
            if (position == RecyclerView.NO_POSITION) return;

            TaskData task = tasks.get(position);
            String taskType = task.getTaskType();
            boolean isCurrentlyExpanded = expandedStates.getOrDefault(taskType, false);
            expandedStates.put(taskType, !isCurrentlyExpanded);

            updateExpansionState(!isCurrentlyExpanded);
        }

        private void updateExpansionState(boolean isExpanded) {
            if (isExpanded) {
                layoutStepsList.setVisibility(View.VISIBLE);
                ivExpandIcon.setRotation(90f);
            } else {
                layoutStepsList.setVisibility(View.GONE);
                ivExpandIcon.setRotation(0f);
            }
        }
    }

    /**
     * Data class representing a task with its steps
     */
    public static class TaskData {
        private String taskName;
        private String taskType;
        private List<TaskStep> steps;

        public TaskData(String taskName, String taskType, List<TaskStep> steps) {
            this.taskName = taskName;
            this.taskType = taskType;
            this.steps = steps;
        }

        public String getTaskName() {
            return taskName;
        }

        public String getTaskType() {
            return taskType;
        }

        public List<TaskStep> getSteps() {
            return steps;
        }
    }
}
