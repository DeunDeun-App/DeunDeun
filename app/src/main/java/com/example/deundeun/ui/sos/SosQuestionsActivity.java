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
 * Figma 92:392 / 121:82 / 121:110 — SOS "도움 요청하기".
 *
 * <p>시안 세 장은 답을 하나씩 채워가는 같은 화면이다. 상단 3칸이 답한 개수만큼 찬다.
 */
public class SosQuestionsActivity extends AppCompatActivity {

    private ActivitySosQuestionsBinding binding;
    private final SosAnswers answers = new SosAnswers();

    private List<TextView> whereOptions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySosQuestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.topBar);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        binding.btnBack.setOnClickListener(v -> finish());

        whereOptions = Arrays.asList(binding.whereSubway, binding.whereHome,
                binding.whereSchool, binding.whereUnknown);
        for (int i = 0; i < whereOptions.size(); i++) {
            final int index = i;
            whereOptions.get(i).setOnClickListener(v -> {
                selectOne(whereOptions, (TextView) v);
                answers.setWhereIndex(index);
                syncProgress();
            });
        }

        bindPair(binding.scaredYes, binding.scaredNo, true);
        bindPair(binding.hurtNo, binding.hurtYes, false);

        binding.btnNext.setOnClickListener(v -> goNext());

        syncProgress();
    }

    /** 두 개짜리 질문. {@code scaredQuestion} 이면 무서움, 아니면 다침 답을 채운다. */
    private void bindPair(TextView first, TextView second, boolean scaredQuestion) {
        List<TextView> pair = Arrays.asList(first, second);
        for (TextView option : pair) {
            option.setOnClickListener(v -> {
                selectOne(pair, (TextView) v);
                // 무서움은 첫 번째가 "네", 다침은 첫 번째가 "아니요" 라 뒤집어 담는다.
                boolean isFirst = v == first;
                if (scaredQuestion) {
                    answers.setScared(isFirst);
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
            // 절반만 채워진 메시지가 보호자에게 가면 상황 판단이 어려워진다.
            Toast.makeText(this, R.string.sos_q_where, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, SosMessageActivity.class);
        intent.putExtra(SosAnswers.EXTRA, answers);
        startActivity(intent);
    }
}
