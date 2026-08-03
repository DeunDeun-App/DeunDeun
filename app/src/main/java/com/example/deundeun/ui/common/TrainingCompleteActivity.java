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
 * Figma 153:124(시뮬레이션) / 125:402(119 연습) — 훈련 완료.
 *
 * <p>두 시안은 제목과 항목별 판정 유무만 다르므로 한 화면으로 합쳤다.
 * 판정 목록을 넘기지 않으면 시뮬레이션 시안이 된다.
 */
public class TrainingCompleteActivity extends AppCompatActivity {

    /** 화면 제목 문자열 리소스. */
    public static final String EXTRA_TITLE_RES = "title_res";
    /** 판정 항목 라벨 리소스 배열. 없으면 체크리스트를 숨긴다. */
    public static final String EXTRA_CHECK_LABELS = "check_labels";
    /** 위 항목과 같은 길이의 통과 여부. */
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

        // "다시 연습"과 "닫기" 모두 이 화면을 닫는다. 앞 화면이 이미 스택에서 빠져 있어
        // 닫으면 훈련탭으로 돌아간다. 다시 연습은 진입 화면을 새로 띄운다.
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

            // 체크/엑스 아이콘만으로는 통과 여부가 스크린리더에 전달되지 않는다.
            row.getRoot().setContentDescription(getString(labels[i]) + ", "
                    + getString(results[i] ? R.string.cd_passed : R.string.cd_failed));

            container.addView(row.getRoot());
        }
    }
}
