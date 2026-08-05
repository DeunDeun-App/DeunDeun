package com.example.deundeun.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deundeun.data.model.TrainingRecord;
import com.example.deundeun.databinding.ItemTrainingRecordBinding;
import com.example.deundeun.databinding.ViewAchievementChipBinding;

import java.util.ArrayList;
import java.util.List;

/** 홈·기록탭이 공유하는 훈련 기록 목록 어댑터. */
public class RecentRecordAdapter extends RecyclerView.Adapter<RecentRecordAdapter.RecordHolder> {

    private final List<TrainingRecord> items = new ArrayList<>();

    public void submit(List<TrainingRecord> records) {
        items.clear();
        items.addAll(records);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrainingRecordBinding binding = ItemTrainingRecordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RecordHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RecordHolder extends RecyclerView.ViewHolder {

        private final ItemTrainingRecordBinding binding;

        RecordHolder(ItemTrainingRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TrainingRecord record) {
            binding.recordTitle.setText(record.getTitle());
            binding.recordMeta.setText(record.getMetaLine());
            binding.recordScore.setText(
                    itemView.getContext().getString(
                            com.example.deundeun.R.string.score_format, record.getScore()));

            bindAchievements(record.getAchievements());

            itemView.setContentDescription(record.getTitle() + ", " + record.getMetaLine()
                    + ", " + record.getScore() + "점");
        }

        private void bindAchievements(List<String> achievements) {
            ViewGroup container = binding.recordAchievements;
            container.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(container.getContext());
            for (String achievement : achievements) {
                ViewAchievementChipBinding chip =
                        ViewAchievementChipBinding.inflate(inflater, container, false);
                chip.chipLabel.setText(achievement);
                container.addView(chip.getRoot());
            }
            container.setVisibility(achievements.isEmpty() ? ViewGroup.GONE : ViewGroup.VISIBLE);
        }
    }
}
