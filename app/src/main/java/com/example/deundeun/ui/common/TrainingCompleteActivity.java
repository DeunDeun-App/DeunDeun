package com.example.deundeun.ui.common;

import android.content.Intent;
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
    /** 서버에서 받은 항목 이름. 있으면 {@link #EXTRA_CHECK_LABELS} 대신 쓴다. */
    public static final String EXTRA_CHECK_LABEL_TEXTS = "check_label_texts";
    public static final String EXTRA_CHECK_RESULTS = "check_results";
    /** "다시 연습" 을 누르면 띄울 화면 ({@code Class}). 없으면 이전 화면으로 돌아간다. */
    public static final String EXTRA_RETRY_ACTIVITY = "retry_activity";

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

        binding.btnRetry.setOnClickListener(v -> retry());
        binding.btnClose.setOnClickListener(v -> finish());
    }

    private void retry() {
        Class<?> retryActivity = (Class<?>) getIntent().getSerializableExtra(EXTRA_RETRY_ACTIVITY);

        if (retryActivity != null) {
            startActivity(new Intent(this, retryActivity));
        } else {
            setResult(RESULT_OK);
        }
        finish();
    }

    private void bindChecklist() {
        String[] labels = readLabels();
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

            row.getRoot().setContentDescription(labels[i] + ", "
                    + getString(results[i] ? R.string.cd_passed : R.string.cd_failed));

            container.addView(row.getRoot());
        }
    }

    @Nullable
    private String[] readLabels() {
        String[] texts = getIntent().getStringArrayExtra(EXTRA_CHECK_LABEL_TEXTS);
        if (texts != null) {
            return texts;
        }

        int[] labelRes = getIntent().getIntArrayExtra(EXTRA_CHECK_LABELS);
        if (labelRes == null) {
            return null;
        }

        String[] labels = new String[labelRes.length];
        for (int i = 0; i < labelRes.length; i++) {
            labels[i] = getString(labelRes[i]);
        }
        return labels;
    }
}
