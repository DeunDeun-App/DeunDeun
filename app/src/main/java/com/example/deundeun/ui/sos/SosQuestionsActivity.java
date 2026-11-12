package com.example.deundeun.ui.sos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivitySosQuestionsBinding;
import com.example.deundeun.ui.common.InsetsUtil;

import java.util.Arrays;
import java.util.List;

/**
 * SOS "도움 요청하기"
 */
public class SosQuestionsActivity extends AppCompatActivity {

    private ActivitySosQuestionsBinding binding;
    private final SosAnswers answers = new SosAnswers();

    private List<TextView> situationOptions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySosQuestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.topBar);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        binding.btnBack.setOnClickListener(v -> finish());

        situationOptions = Arrays.asList(binding.situationFire, binding.situationEarthquake,
                binding.situationFlood, binding.situationUnknown);
        for (int i = 0; i < situationOptions.size(); i++) {
            final int index = i;
            situationOptions.get(i).setOnClickListener(v -> {
                selectOne(situationOptions, (TextView) v);
                answers.setSituationIndex(index);
                syncProgress();
            });
        }

        bindPair(binding.trappedYes, binding.trappedNo, true);
        bindPair(binding.hurtNo, binding.hurtYes, false);

        binding.btnNext.setOnClickListener(v -> goNext());

        syncProgress();
    }

    /** 두 개짜리 질문. {@code trappedQuestion} 이면 갇힘, 아니면 다침 답을 채운다. */
    private void bindPair(TextView first, TextView second, boolean trappedQuestion) {
        List<TextView> pair = Arrays.asList(first, second);
        for (TextView option : pair) {
            option.setOnClickListener(v -> {
                selectOne(pair, (TextView) v);
                // 갇힘은 첫 번째가 "네", 다침은 첫 번째가 "아니요" 라 뒤집어 담는다.
                boolean isFirst = v == first;
                if (trappedQuestion) {
                    answers.setTrapped(isFirst);
                } else {
                    answers.setHurt(!isFirst);
                }
                syncProgress();
            });
        }
    }

    private void selectOne(List<TextView> group, TextView selected) {
        for (TextView option : group) {
            option.setSelected(option == selected);
        }
    }

    private void syncProgress() {
        int answered = answers.answeredCount();
        View[] segments = {binding.progress1, binding.progress2, binding.progress3};
        for (int i = 0; i < segments.length; i++) {
            segments[i].setActivated(i < answered);
        }
        binding.progressBar.setContentDescription(
                getString(R.string.cd_sos_progress, segments.length, answered));
    }

    private void goNext() {
        if (!answers.isComplete()) {
            Toast.makeText(this, R.string.sos_q_situation, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, SosMessageActivity.class);
        intent.putExtra(SosAnswers.EXTRA, answers);
        intent.putExtra(SosActivity.EXTRA_GUARDIAN_NAME,
                getIntent().getStringExtra(SosActivity.EXTRA_GUARDIAN_NAME));
        startActivity(intent);
    }
}
