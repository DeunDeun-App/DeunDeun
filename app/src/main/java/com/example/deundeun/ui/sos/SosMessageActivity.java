package com.example.deundeun.ui.sos;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.FakeTrainingRepository;
import com.example.deundeun.databinding.ActivitySosMessageBinding;
import com.example.deundeun.ui.common.InsetsUtil;

/**
 * 보낼 메시지 미리보기
 */
public class SosMessageActivity extends AppCompatActivity {

    private ActivitySosMessageBinding binding;
    private final FakeTrainingRepository repository = new FakeTrainingRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySosMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.messageScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        SosAnswers answers = (SosAnswers) getIntent().getSerializableExtra(SosAnswers.EXTRA);
        String guardian = repository.getUserProfile().getGuardianRelation();

        binding.guardianName.setText(guardian);
        binding.messageBody.setText(buildMessage(guardian, answers));

        binding.btnEdit.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> send(answers));
    }

    private String buildMessage(String guardian, @Nullable SosAnswers answers) {
        if (answers == null) {
            return "";
        }
        String[] whereSentences = getResources().getStringArray(R.array.sos_where_sentences);
        int index = answers.getWhereIndex() == null ? 0 : answers.getWhereIndex();
        String where = whereSentences[Math.min(index, whereSentences.length - 1)];

        String scared = getString(Boolean.TRUE.equals(answers.getScared())
                ? R.string.sos_scared_line : R.string.sos_not_scared_line);
        String hurt = getString(Boolean.TRUE.equals(answers.getHurt())
                ? R.string.sos_hurt_line : R.string.sos_not_hurt_line);
        return getString(R.string.sos_preview_message, guardian, where, scared, hurt);
    }

    private void send(@Nullable SosAnswers answers) {
        // TODO: 실제 전송(위치 권한 → GPS → 보호자 문자)
        Intent intent = new Intent(this, SosSentActivity.class);
        intent.putExtra(SosAnswers.EXTRA, answers);
        startActivity(intent);
        finish();
    }
}
