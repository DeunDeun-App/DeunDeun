package com.example.deundeun.ui.sos;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.repository.FakeTrainingRepository;
import com.example.deundeun.databinding.ActivitySosBinding;
import com.example.deundeun.ui.common.InsetsUtil;

/**
 * SOS 도움요청 진입.
 *
 * 실제 요청(위치 권한, GPS 수집, 보호자 전송)은 아직 붙지 않았다.
 * 위치 권한은 사용자가 이 화면에서 "도움 요청하기"를 누른 뒤에 요청해야 한다.
 */
public class SosActivity extends AppCompatActivity {

    private ActivitySosBinding binding;
    private final FakeTrainingRepository repository = new FakeTrainingRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.sosRoot);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        String relation = repository.getUserProfile().getGuardianRelation();
        binding.sosStep1.setText(getString(R.string.sos_step_1, relation));

        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnRequest.setOnClickListener(v -> requestHelp());
    }

    private void requestHelp() {
        // TODO: 여기서 위치 권한을 요청한다. 사용자가 요청 의사를 밝힌 뒤가 맞는 시점이다.
        startActivity(new Intent(this, SosQuestionsActivity.class));
    }
}
