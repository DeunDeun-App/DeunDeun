package com.example.deundeun.ui.call;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.databinding.ActivityCallIntroBinding;
import com.example.deundeun.ui.common.InsetsUtil;

/** 신고 연습 진입. */
public class CallIntroActivity extends AppCompatActivity {

    private ActivityCallIntroBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCallIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.callIntroScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        binding.header.setOnBackClickListener(v -> finish());
        binding.btnStart.setOnClickListener(v -> {
            startActivity(new Intent(this, CallPracticeActivity.class));
            finish();
        });
    }
}
