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

/** 메시지 전송 완료 */
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

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_TAB, HomeFragment.TAG);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void dial119() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:119")));
    }
}
