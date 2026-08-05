package com.example.deundeun.ui.translate;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivityDisasterTranslateBinding;
import com.example.deundeun.databinding.ViewTranslateResultBinding;
import com.example.deundeun.ui.common.InsetsUtil;

/**
 * 재난문자 쉬운 말로 보기
 */
public class DisasterTranslateActivity extends AppCompatActivity {

    private ActivityDisasterTranslateBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityDisasterTranslateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyVerticalInsets(binding.translateScroll);

        binding.header.setOnBackClickListener(v -> finish());

        bindResult(binding.result1, "☔",
                R.string.translate_r1_title, R.string.translate_r1_desc);
        bindResult(binding.result2, "🏃",
                R.string.translate_r2_title, R.string.translate_r2_desc);
        bindResult(binding.result3, "🌊",
                R.string.translate_r3_title, R.string.translate_r3_desc);
    }

    private void bindResult(ViewTranslateResultBinding result, String emoji,
                            @StringRes int titleRes, @StringRes int descRes) {
        result.resultEmoji.setText(emoji);
        result.resultTitle.setText(titleRes);
        result.resultDesc.setText(descRes);

        result.getRoot().setContentDescription(
                getString(titleRes) + ". " + getString(descRes));
    }
}
