package com.example.deundeun.ui.sos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.MainActivity;
import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivitySosSentBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.home.HomeFragment;

/** 메시지 전송 완료 */
public class SosSentActivity extends AppCompatActivity {

    /** 문자에 위치를 붙였는지. 없으면 true 로 본다 */
    public static final String EXTRA_HAS_LOCATION = "has_location";

    private ActivitySosSentBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySosSentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.sosSentRoot);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        String guardian = getIntent().getStringExtra(SosActivity.EXTRA_GUARDIAN_NAME);
        if (guardian == null) {
            guardian = getString(R.string.sos_guardian_fallback);
        }
        boolean hasLocation = getIntent().getBooleanExtra(EXTRA_HAS_LOCATION, true);
        binding.sentDesc.setText(getString(hasLocation
                ? R.string.sos_sent_desc : R.string.sos_sent_desc_no_location, guardian));

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
