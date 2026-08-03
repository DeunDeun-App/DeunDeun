package com.example.deundeun.ui.sos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.MainActivity;
import com.example.deundeun.R;
import com.example.deundeun.data.FakeTrainingRepository;
import com.example.deundeun.databinding.ActivitySosSentBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.home.HomeFragment;

/** Figma 108:69 — 메시지 전송 완료. 필요하면 119 연결로 이어진다. */
public class SosSentActivity extends AppCompatActivity {

    private ActivitySosSentBinding binding;
    private final FakeTrainingRepository repository = new FakeTrainingRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySosSentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.sosSentRoot);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        String guardian = repository.getUserProfile().getGuardianRelation();
        binding.sentDesc.setText(getString(R.string.sos_sent_desc, guardian));

        binding.btnOk.setOnClickListener(v -> goHome());
        binding.btnCall119.setOnClickListener(v -> dial119());
    }

    /**
     * 홈으로 돌아간다. 그냥 finish() 하면 SOS 질문 화면이 스택에 남아 있어 그리로 되돌아간다.
     * 도움 요청을 마친 뒤 질문지를 다시 보여줄 이유가 없으므로 SOS 화면들을 모두 걷어낸다.
     */
    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_TAB, HomeFragment.TAG);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * 전화 앱에 119를 띄우기만 한다. 바로 거는 ACTION_CALL 은 쓰지 않는다.
     * 실수로 눌렸을 때 되돌릴 수 없고, 통화 권한도 따로 받아야 한다.
     */
    private void dial119() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:119")));
    }
}
