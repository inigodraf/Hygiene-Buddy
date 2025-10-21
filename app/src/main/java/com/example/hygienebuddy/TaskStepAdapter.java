package com.example.hygienebuddy;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for individual task steps within an expandable task
 */
public class TaskStepAdapter extends RecyclerView.Adapter<TaskStepAdapter.StepViewHolder> {

    private Context context;
    private List<TaskStep> steps;
    private String taskType;
    private OnStepUploadListener onStepUploadListener;

    public interface OnStepUploadListener {
        void onStepUpload(String taskType, int stepNumber);
    }

    public TaskStepAdapter(Context context, OnStepUploadListener listener) {
        this.context = context;
        this.onStepUploadListener = listener;
        this.steps = new ArrayList<>();
    }

    public void setSteps(List<TaskStep> steps, String taskType) {
        this.steps = steps;
        this.taskType = taskType;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task_step, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        TaskStep step = steps.get(position);
        holder.bind(step, taskType);
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    class StepViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivStepIcon, ivVideoStatus;
        private TextView tvStepText, tvStepDuration;
        private MaterialButton btnUploadStepVideo;

        public StepViewHolder(@NonNull View itemView) {
            super(itemView);

            ivStepIcon = itemView.findViewById(R.id.ivStepIcon);
            ivVideoStatus = itemView.findViewById(R.id.ivVideoStatus);
            tvStepText = itemView.findViewById(R.id.tvStepText);
            tvStepDuration = itemView.findViewById(R.id.tvStepDuration);
            btnUploadStepVideo = itemView.findViewById(R.id.btnUploadStepVideo);

            btnUploadStepVideo.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onStepUploadListener != null) {
                    TaskStep step = steps.get(position);
                    onStepUploadListener.onStepUpload(taskType, step.getStepNumber());
                }
            });
        }

        public void bind(TaskStep step, String taskType) {
            tvStepText.setText(step.getDisplayText());

            // Format duration
            String durationText;
            if (step.getMinutes() > 0) {
                durationText = "Duration: " + step.getMinutes() + "m " + step.getSeconds() + "s";
            } else {
                durationText = "Duration: " + step.getSeconds() + "s";
            }
            tvStepDuration.setText(durationText);

            // Update video status
            VideoManager videoManager = new VideoManager(context);
            boolean hasVideo = videoManager.hasStepVideo(taskType, step.getStepNumber());

            Log.d("TaskStepAdapter", "Step " + step.getStepNumber() + " has video: " + hasVideo);

            if (hasVideo) {
                ivVideoStatus.setImageResource(R.drawable.ic_checklist_filled);
                btnUploadStepVideo.setText("Replace");
            } else {
                ivVideoStatus.setImageResource(R.drawable.ic_placeholder_video);
                btnUploadStepVideo.setText("Upload");
            }
        }
    }
}
