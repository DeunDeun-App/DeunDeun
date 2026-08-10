package com.example.deundeun.ui.record;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.example.deundeun.R;
import com.example.deundeun.data.repository.FakeTrainingRepository;
import com.example.deundeun.data.model.AchievementRate;
import com.example.deundeun.data.model.TrainingRecord;
import com.example.deundeun.databinding.FragmentRecordBinding;
import com.example.deundeun.databinding.ItemAchievementRateBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.home.RecentRecordAdapter;

import java.util.List;

/**
 * 기록탭
 */
public class RecordFragment extends Fragment {

    public static final String TAG = "record";
    private static final String STATE_MODE_SCORE = "mode_score";

    private FragmentRecordBinding binding;
    private final FakeTrainingRepository repository = new FakeTrainingRepository();

    private boolean scoreMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.applyBottomInset(binding.recordScroll);

        if (savedInstanceState != null) {
            scoreMode = savedInstanceState.getBoolean(STATE_MODE_SCORE, false);
        }

        binding.tabHistory.setOnClickListener(v -> setScoreMode(false));
        binding.tabScore.setOnClickListener(v -> setScoreMode(true));

        bindHistoryMode();
        bindScoreMode();
        applyMode();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MODE_SCORE, scoreMode);
    }

    private void setScoreMode(boolean score) {
        if (scoreMode == score) {
            return;
        }
        scoreMode = score;
        applyMode();
    }

    private void applyMode() {
        binding.modeHistory.setVisibility(scoreMode ? View.GONE : View.VISIBLE);
        binding.modeScore.setVisibility(scoreMode ? View.VISIBLE : View.GONE);

        styleTab(binding.tabHistory, !scoreMode);
        styleTab(binding.tabScore, scoreMode);
    }

    private void styleTab(android.widget.TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_segmented_thumb : 0);
        tab.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.text_primary : R.color.text_secondary));
        tab.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(),
                selected ? R.font.pretendard_extrabold : R.font.pretendard_semibold));
        tab.setSelected(selected);
    }

    private void bindHistoryMode() {
        List<TrainingRecord> records = repository.getRecentRecords();
        if (!records.isEmpty()) {
            TrainingRecord latest = records.get(0);
            binding.historyTitle.setText(latest.getTitle());
            binding.historyMeta.setText(latest.getMetaLine());
        }

        binding.barChart.setData(
                repository.getWeekLabels(),
                repository.getSimulationCounts(),
                repository.getEmergencyCallCounts());

        RecentRecordAdapter adapter = new RecentRecordAdapter();
        binding.recentRecords.setAdapter(adapter);
        adapter.submit(records);
    }

    private void bindScoreMode() {
        List<TrainingRecord> records = repository.getRecentRecords();
        if (!records.isEmpty()) {
            TrainingRecord latest = records.get(0);
            binding.scoreTitle.setText(latest.getTitle());
            binding.scoreMeta.setText(latest.getMetaLine());
        }

        binding.lineChart.setData(repository.getScoreDateLabels(), repository.getScoreTrend());

        bindAchievements(repository.getAchievementRates());
    }

    private void bindAchievements(List<AchievementRate> rates) {
        ViewGroup container = binding.achievementList;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        for (AchievementRate rate : rates) {
            ItemAchievementRateBinding row =
                    ItemAchievementRateBinding.inflate(inflater, container, false);

            row.rateLabel.setText(rate.getLabel());
            row.ratePercent.setText(getString(R.string.percent_format, rate.getPercent()));

            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) row.rateFill.getLayoutParams();
            params.matchConstraintPercentWidth = rate.getPercent() / 100f;
            row.rateFill.setLayoutParams(params);
            ViewCompat.setBackgroundTintList(row.rateFill,
                    ContextCompat.getColorStateList(requireContext(), colorFor(rate.getPercent())));

            row.getRoot().setContentDescription(
                    rate.getLabel() + " " + rate.getPercent() + "퍼센트");

            container.addView(row.getRoot());
        }
    }

    private int colorFor(int percent) {
        if (percent >= 90) {
            return R.color.orange_primary;
        }
        return percent >= 75 ? R.color.orange_mid : R.color.orange_pale;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
