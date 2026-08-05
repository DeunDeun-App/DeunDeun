package com.example.deundeun.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivityTrainingCompleteBinding;
import com.example.deundeun.databinding.ViewCheckRowBinding;

/**
 * 훈련 완료.
 */
public class TrainingCompleteActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE_RES = "title_res";
    public static final String EXTRA_CHECK_LABELS = "check_labels";
    public static final String EXTRA_CHECK_RESULTS = "check_results";

    private ActivityTrainingCompleteBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityTrainingCompleteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.completeRoot);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        int titleRes = getIntent().getIntExtra(EXTRA_TITLE_RES, R.string.complete_training);
        binding.completeTitle.setText(titleRes);

        bindChecklist();

        binding.btnRetry.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
        binding.btnClose.setOnClickListener(v -> finish());
    }

    private void bindChecklist() {
        int[] labels = getIntent().getIntArrayExtra(EXTRA_CHECK_LABELS);
        boolean[] results = getIntent().getBooleanArrayExtra(EXTRA_CHECK_RESULTS);

        if (labels == null || results == null || labels.length != results.length) {
            binding.checklistCard.setVisibility(View.GONE);
            return;
        }

        binding.checklistCard.setVisibility(View.VISIBLE);
        ViewGroup container = binding.checklist;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < labels.length; i++) {
            ViewCheckRowBinding row = ViewCheckRowBinding.inflate(inflater, container, false);
            row.checkLabel.setText(labels[i]);
            row.checkIcon.setImageResource(
                    results[i] ? R.drawable.ic_check_small : R.drawable.ic_x_small);

            row.getRoot().setContentDescription(getString(labels[i]) + ", "
                    + getString(results[i] ? R.string.cd_passed : R.string.cd_failed));

            container.addView(row.getRoot());
        }
    }
}
