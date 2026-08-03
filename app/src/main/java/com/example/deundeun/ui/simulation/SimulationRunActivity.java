package com.example.deundeun.ui.simulation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivitySimulationRunBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.TrainingCompleteActivity;

/**
 * Figma 74:42(인식 중) / 81:101(인식 성공) — 시뮬레이션 진행 화면.
 *
 * <p>두 시안은 같은 화면의 두 상태다. 대피 자세를 바르게 취하면 AI 가이드와 하단 버튼이
 * 주황에서 초록으로 바뀌고, 그때 "훈련 완료"를 눌러야 완료 화면으로 넘어간다.
 *
 * <p>카메라 프리뷰와 포즈 인식은 아직 붙지 않았다. 지금은 하단 버튼을 눌러 상태를 넘긴다.
 * MediaPipe 가 붙으면 {@link #onStatusClicked()} 의 첫 단계를 인식 결과로 바꾸면 된다.
 */
public class SimulationRunActivity extends AppCompatActivity {

    private static final String STATE_POSE_DETECTED = "pose_detected";

    private ActivitySimulationRunBinding binding;

    /** 대피 자세를 인식했는지. 인식 전에는 완료 화면으로 넘어갈 수 없다. */
    private boolean poseDetected;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySimulationRunBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyVerticalInsets(binding.simRunRoot);

        String scenario = getIntent().getStringExtra(SimulationIntroActivity.EXTRA_SCENARIO);
        if (scenario == null) {
            scenario = getString(R.string.sim_scenario_earthquake);
        }
        binding.badgeScenario.setText(getString(R.string.sim_running_badge, scenario));

        if (savedInstanceState != null) {
            poseDetected = savedInstanceState.getBoolean(STATE_POSE_DETECTED, false);
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnStatus.setOnClickListener(v -> onStatusClicked());

        applyState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_POSE_DETECTED, poseDetected);
    }

    private void onStatusClicked() {
        if (poseDetected) {
            finishTraining();
            return;
        }
        // TODO: 포즈 인식이 붙으면 이 전환은 인식 결과가 일으킨다.
        poseDetected = true;
        applyState();
    }

    private void applyState() {
        int accent = ContextCompat.getColor(this,
                poseDetected ? R.color.camera_success : R.color.orange_primary);

        binding.guideCard.setBackgroundResource(poseDetected
                ? R.drawable.bg_camera_guide_success : R.drawable.bg_camera_guide);
        binding.guideLabel.setTextColor(accent);
        binding.guideMessage.setText(poseDetected
                ? R.string.sim_ai_guide_success : R.string.sim_ai_guide_message);

        binding.btnStatus.setText(poseDetected ? R.string.sim_finish : R.string.sim_detecting);
        binding.btnStatus.setBackgroundTintList(
                ContextCompat.getColorStateList(this,
                        poseDetected ? R.color.camera_success : R.color.orange_primary));

        if (poseDetected) {
            // 색만으로는 성공을 알 수 없다. 스크린리더에도 알리고 소리로도 짚어준다.
            binding.guideCard.announceForAccessibility(
                    getString(R.string.cd_sim_pose_detected));
        }
        binding.guideCard.setContentDescription(getString(R.string.sim_ai_guide) + ", "
                + binding.guideMessage.getText());
    }

    private void finishTraining() {
        Intent intent = new Intent(this, TrainingCompleteActivity.class);
        intent.putExtra(TrainingCompleteActivity.EXTRA_TITLE_RES, R.string.complete_training);
        startActivity(intent);
        finish();
    }
}
