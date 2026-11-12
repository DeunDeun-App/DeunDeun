package com.example.deundeun.ui.simulation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivitySimulationRunBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.TrainingCompleteActivity;
import com.example.deundeun.ui.simulation.pose.DisasterPoseEvaluator;
import com.example.deundeun.ui.simulation.pose.PoseCheck;
import com.example.deundeun.ui.simulation.pose.PoseLandmarkerHelper;
import com.example.deundeun.ui.simulation.pose.PoseTrainingSession;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 시뮬레이션 진행 화면.
 *
 * CameraX 로 카메라를 띄우고, 프레임마다 MediaPipe 로 자세를 인식해
 * {@link DisasterPoseEvaluator} 가 판정한 결과에 맞춰 짧은 안내 문장을 보여준다.
 * 올바른 자세를 몇 초 유지하면 완료, 제한 시간 안에 못 하면 재훈련을 권한다.
 */
public class SimulationRunActivity extends AppCompatActivity {

    private static final String TAG = "SimulationRun";

    private ActivitySimulationRunBinding binding;

    private boolean fire;
    private DisasterPoseEvaluator evaluator;
    private final PoseTrainingSession session = new PoseTrainingSession();

    private ExecutorService analysisExecutor;
    private PoseLandmarkerHelper poseHelper;
    private boolean frontCamera = true;
    private boolean permissionDenied;

    /** 같은 문장을 화면 낭독기로 반복해서 읽지 않으려고 기억해 둔다 */
    private String lastAnnounced;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                permissionDenied = !granted;
                if (granted) {
                    startCamera();
                } else {
                    showMessage(R.string.sim_camera_permission_denied);
                    applyButton();
                }
            });

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

        fire = getIntent().getBooleanExtra(SimulationIntroActivity.EXTRA_FIRE, false);
        evaluator = new DisasterPoseEvaluator(fire);

        if (!fire) {
            // 지진: 실제 방 대신 지진 난 방 사진을 배경으로 쓰고,
            // 화면이 흔들리다가 올바른 대피 자세를 취하면 멈춘다
            binding.virtualBackground.setVisibility(View.VISIBLE);
            binding.quakeEffect.setVisibility(View.VISIBLE);
            binding.quakeEffect.setShakeTarget(binding.cameraPreview);
            binding.quakeEffect.setShaking(true);
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnStatus.setOnClickListener(v -> onStatusClicked());

        showMessage(R.string.sim_camera_loading);
        applyButton();

        analysisExecutor = Executors.newSingleThreadExecutor();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 분석 스레드에서 도는 작업이 끝난 뒤 닫히도록 같은 스레드에 넣는다
        analysisExecutor.execute(() -> {
            if (poseHelper != null) poseHelper.close();
        });
        analysisExecutor.shutdown();
    }

    private void startCamera() {
        // 모델 로딩이 무거워서 분석 스레드에서 만든다
        // 화재는 손이 코와 입을 덮었는지 봐야 해서 손 모델도 같이 돌린다
        // 지진은 가상 배경을 위해 사람을 오려낸다
        analysisExecutor.execute(() -> poseHelper = new PoseLandmarkerHelper(this, fire, !fire,
                new PoseLandmarkerHelper.Listener() {
                    @Override
                    public void onResult(PoseLandmarkerResult poseResult,
                                         @Nullable HandLandmarkerResult handResult,
                                         @Nullable Bitmap person, int w, int h) {
                        runOnUiThread(() -> {
                            if (!fire) binding.virtualBackground.setPerson(person);
                            onPoseResult(poseResult, handResult, w, h);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "pose error: " + message);
                        runOnUiThread(() -> showMessage(R.string.sim_pose_error));
                    }
                }));

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                bindCamera(future.get());
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "카메라 시작 실패", e);
                showMessage(R.string.sim_pose_error);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(ProcessCameraProvider provider) {
        CameraSelector selector = CameraSelector.DEFAULT_FRONT_CAMERA;
        try {
            // 에뮬레이터에서 웹캠을 후면에만 연결했을 수도 있으니 없으면 후면으로
            if (!provider.hasCamera(selector)) {
                selector = CameraSelector.DEFAULT_BACK_CAMERA;
                frontCamera = false;
            }
        } catch (CameraInfoUnavailableException e) {
            Log.w(TAG, "카메라 정보를 읽지 못함", e);
        }

        ResolutionSelector resolution = new ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build();

        Preview preview = new Preview.Builder()
                .setResolutionSelector(resolution)
                .build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setResolutionSelector(resolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();
        boolean mirror = frontCamera;
        analysis.setAnalyzer(analysisExecutor, image -> {
            if (poseHelper == null) {
                image.close();
                return;
            }
            poseHelper.detect(image, mirror);
        });

        provider.unbindAll();
        provider.bindToLifecycle(this, selector, preview, analysis);
    }

    private void onPoseResult(PoseLandmarkerResult poseResult,
                              @Nullable HandLandmarkerResult handResult,
                              int imageWidth, int imageHeight) {
        if (isFinishing() || isDestroyed()) return;

        List<NormalizedLandmark> pose = poseResult.landmarks().isEmpty()
                ? null : poseResult.landmarks().get(0);
        List<List<NormalizedLandmark>> hands = handResult != null ? handResult.landmarks() : null;
        binding.poseOverlay.setLandmarks(pose, hands, imageWidth, imageHeight);

        PoseTrainingSession.Phase before = session.getPhase();
        if (before == PoseTrainingSession.Phase.SUCCESS
                || before == PoseTrainingSession.Phase.RETRY) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        PoseCheck check = evaluator.evaluate(pose, hands);
        PoseTrainingSession.Phase phase = session.update(check, now);
        applyState(phase, check, now);
    }

    private void applyState(PoseTrainingSession.Phase phase, PoseCheck check, long now) {
        boolean success = phase == PoseTrainingSession.Phase.SUCCESS;
        int accent = ContextCompat.getColor(this, success || phase == PoseTrainingSession.Phase.HOLDING
                ? R.color.camera_success : R.color.orange_primary);

        binding.guideCard.setBackgroundResource(success || phase == PoseTrainingSession.Phase.HOLDING
                ? R.drawable.bg_camera_guide_success : R.drawable.bg_camera_guide);
        binding.guideLabel.setTextColor(accent);
        binding.poseOverlay.setAccent(accent);
        if (!fire) {
            binding.quakeEffect.setShaking(phase == PoseTrainingSession.Phase.GUIDING);
        }

        switch (phase) {
            case SUCCESS:
                showMessage(fire ? R.string.sim_fire_success : R.string.sim_ai_guide_success);
                break;
            case RETRY:
                showMessage(R.string.sim_retry_message);
                break;
            case HOLDING:
                showMessage(getString(R.string.sim_pose_hold, session.remainingHoldSeconds(now)),
                        false);
                break;
            default:
                showMessage(messageFor(check));
                break;
        }
        applyButton();
    }

    @StringRes
    private int messageFor(PoseCheck check) {
        switch (check) {
            case NO_PERSON:
                return fire ? R.string.sim_pose_fire_no_person : R.string.sim_pose_no_person;
            case NOT_LOW:
                return R.string.sim_pose_quake_low;
            default:
                return fire ? R.string.sim_pose_fire_protect : R.string.sim_pose_quake_protect;
        }
    }

    private void applyButton() {
        PoseTrainingSession.Phase phase = session.getPhase();
        int textRes;
        int colorRes = R.color.orange_primary;
        if (permissionDenied) {
            textRes = R.string.sim_camera_permission_retry;
        } else if (phase == PoseTrainingSession.Phase.SUCCESS) {
            textRes = R.string.sim_finish;
            colorRes = R.color.camera_success;
        } else if (phase == PoseTrainingSession.Phase.RETRY) {
            textRes = R.string.sim_retry;
        } else {
            textRes = R.string.sim_detecting;
        }
        binding.btnStatus.setText(textRes);
        binding.btnStatus.setBackgroundTintList(ContextCompat.getColorStateList(this, colorRes));
    }

    private void onStatusClicked() {
        if (permissionDenied) {
            permissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        switch (session.getPhase()) {
            case SUCCESS:
                finishTraining();
                break;
            case RETRY:
                restartTraining();
                break;
            default:
                // 인식 중에는 버튼으로 넘길 수 없다
                break;
        }
    }

    private void restartTraining() {
        session.restart();
        lastAnnounced = null;
        if (!fire) {
            binding.quakeEffect.setShaking(true);
        }
        binding.guideCard.setBackgroundResource(R.drawable.bg_camera_guide);
        binding.guideLabel.setTextColor(ContextCompat.getColor(this, R.color.orange_primary));
        showMessage(messageFor(fire ? PoseCheck.NOT_PROTECTED : PoseCheck.NOT_LOW));
        applyButton();
    }

    private void showMessage(@StringRes int res) {
        showMessage(getString(res), true);
    }

    private void showMessage(String message, boolean announce) {
        binding.guideMessage.setText(message);
        binding.guideCard.setContentDescription(getString(R.string.sim_ai_guide) + ", " + message);
        if (announce && !message.equals(lastAnnounced)) {
            lastAnnounced = message;
            binding.guideCard.announceForAccessibility(message);
        }
    }

    private void finishTraining() {
        Intent intent = new Intent(this, TrainingCompleteActivity.class);
        intent.putExtra(TrainingCompleteActivity.EXTRA_TITLE_RES, R.string.complete_training);
        startActivity(intent);
        finish();
    }
}
