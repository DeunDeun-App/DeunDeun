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
 * Figma 81:336 — 재난문자 쉬운 말로 보기.
 *
 * <p>지금은 시안의 예시 문자를 고정으로 보여준다. 실제로는 수신한 재난문자를
 * 서버(또는 온디바이스 모델)에 넘겨 쉬운 말로 바꾼 결과를 받아 채운다.
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

        // 그림은 시안의 일러스트 자리. 에셋이 나오기 전까지 이모지로 대신한다.
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

        // 이모지가 따로 읽히면 방해가 되므로 카드 한 장을 한 덩어리로 묶는다.
        result.getRoot().setContentDescription(
                getString(titleRes) + ". " + getString(descRes));
    }
}
