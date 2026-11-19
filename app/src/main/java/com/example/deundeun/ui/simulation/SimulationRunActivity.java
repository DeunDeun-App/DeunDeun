package com.example.deundeun.ui.simulation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

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
import com.example.deundeun.data.network.dto.record.KeywordResponse;
import com.example.deundeun.data.network.dto.simulation.SimulationRequest;
import com.example.deundeun.data.network.dto.simulation.SimulationResultResponse;
import com.example.deundeun.data.repository.RecordRepository;
import com.example.deundeun.databinding.ActivitySimulationRunBinding;
import com.example.deundeun.ui.call.OperatorVoice;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.TrainingCompleteActivity;
import com.example.deundeun.ui.simulation.pose.DisasterPoseEvaluator;
import com.example.deundeun.ui.simulation.pose.PoseCheck;
import com.example.deundeun.ui.simulation.pose.PoseLandmarkerHelper;
import com.example.deundeun.ui.simulation.pose.PoseTrainingSession;
import com.example.deundeun.ui.simulation.story.DisasterStory;
import com.example.deundeun.ui.simulation.story.StoryStep;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 시뮬레이션 진행 화면.
 *
 * {@link DisasterStory} 의 짧은 이야기를 한 장면씩 보여준다.
 * - 고르기 장면: 두 행동 중 하나를 고른다. 틀리면 이유를 듣고 다시 고른다
 * - 따라 하기 장면: CameraX 로 카메라를 띄우고, 프레임마다 MediaPipe 로 자세를 인식해
 *   {@link DisasterPoseEvaluator} 가 판정한 결과에 맞춰 짧은 안내 문장을 보여준다.
 *   올바른 자세를 몇 초 유지하면 다음 장면, 제한 시간 안에 못 하면 재훈련을 권한다.
 * 카메라는 따라 하기 장면에서만 켠다. 자세 시도가 끝나면 결과를 서버에 기록한다.
 */
public class SimulationRunActivity extends AppCompatActivity {

    private static final String TAG = "SimulationRun";

    private ActivitySimulationRunBinding binding;

    private boolean fire;
    private String scenario;
    private DisasterPoseEvaluator evaluator;
    private final PoseTrainingSession session = new PoseTrainingSession();

    private ExecutorService analysisExecutor;
    private PoseLandmarkerHelper poseHelper;
    private boolean frontCamera = true;
    private boolean permissionDenied;
    /** 따라 하기 장면이라 카메라가 켜져 있어야 하는지 */
    private boolean cameraWanted;
    private boolean helperRequested;
    @Nullable
    private ProcessCameraProvider cameraProvider;

    private List<StoryStep> steps;
    private int stepIndex;
    /** 고르기 장면에서 맞는 답을 골랐는지 */
    private boolean choiceSolved;
    private MaterialButton[] choiceButtons;

    /** 장면과 안내를 소리로 읽어 준다. 화면 낭독기를 쓰는 사람에게는 낭독기가 읽으므로 쓰지 않는다 */
    private OperatorVoice voice;
    private boolean readAloud;

    /** 같은 문장을 화면 낭독기로 반복해서 읽지 않으려고 기억해 둔다 */
    private String lastAnnounced;

    private RecordRepository recordRepository;
    /** 이번 시도에서 한 번이라도 해낸 동작. 실패해도 해낸 동작은 점수로 인정한다 */
    private boolean crouched;
    private boolean covered;
    /** 서버가 채점한 이번 시도 결과. 완료 화면 체크리스트에 쓴다 */
    @Nullable
    private SimulationResultResponse lastResult;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                permissionDenied = !granted;
                if (granted) {
                    if (cameraWanted) startCamera();
                } else {
                    if (isPoseStep()) showMessage(R.string.sim_camera_permission_denied);
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
        this.scenario = scenario;

        fire = getIntent().getBooleanExtra(SimulationIntroActivity.EXTRA_FIRE, false);
        evaluator = new DisasterPoseEvaluator(fire);
        steps = DisasterStory.steps(fire);
        recordRepository = new RecordRepository(this);

        if (!fire) {
            // 지진: 실제 방 대신 지진 난 방 사진을 배경으로 쓰고,
            // 흔들리는 장면에서는 화면이 흔들리다가 올바른 대피 자세를 취하면 멈춘다
            binding.virtualBackground.setVisibility(View.VISIBLE);
            binding.quakeEffect.setVisibility(View.VISIBLE);
        }
        binding.scenePanel.setBackgroundTintList(ContextCompat.getColorStateList(this,
                fire ? R.color.story_scene_fire : R.color.story_scene_quake));

        voice = new OperatorVoice(this);
        AccessibilityManager am = getSystemService(AccessibilityManager.class);
        readAloud = am == null || !am.isTouchExplorationEnabled();

        choiceButtons = new MaterialButton[]{binding.btnChoice1, binding.btnChoice2};
        for (int i = 0; i < choiceButtons.length; i++) {
            int index = i;
            choiceButtons[i].setOnClickListener(v -> onChoiceClicked(index));
        }
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnStatus.setOnClickListener(v -> onStatusClicked());

        analysisExecutor = Executors.newSingleThreadExecutor();
        showStep(0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        voice.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        voice.shutdown();
        // 분석 스레드에서 도는 작업이 끝난 뒤 닫히도록 같은 스레드에 넣는다
        analysisExecutor.execute(() -> {
            if (poseHelper != null) poseHelper.close();
        });
        analysisExecutor.shutdown();
    }

    /** 따라 하기 장면에 들어왔을 때. 권한이 없으면 이때 처음 묻는다 */
    private void ensureCamera() {
        cameraWanted = true;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            permissionDenied = false;
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /** 고르기 장면으로 넘어가면 카메라를 끈다. 인식 모델은 다시 쓸 수 있게 남겨 둔다 */
    private void stopCamera() {
        cameraWanted = false;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private void startCamera() {
        if (!helperRequested) {
            helperRequested = true;
            createPoseHelper();
        }

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                // 준비하는 사이 다른 장면으로 넘어갔으면 켜지 않는다
                if (cameraWanted && !isFinishing()) {
                    bindCamera(cameraProvider);
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "카메라 시작 실패", e);
                showMessage(R.string.sim_pose_error);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void createPoseHelper() {
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

        // 고르기 장면에서는 자세를 판정하지 않는다
        if (!isPoseStep()) return;

        PoseTrainingSession.Phase before = session.getPhase();
        if (before == PoseTrainingSession.Phase.SUCCESS
                || before == PoseTrainingSession.Phase.RETRY) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        PoseCheck check = evaluator.evaluate(pose, hands);
        rememberDoneSteps(check);
        PoseTrainingSession.Phase phase = session.update(check, now);
        applyState(phase, check, now);

        if (phase == PoseTrainingSession.Phase.SUCCESS || phase == PoseTrainingSession.Phase.RETRY) {
            submitResult(phase == PoseTrainingSession.Phase.SUCCESS, session.elapsedMs(now));
        }
    }

    /** 지진은 웅크린 뒤에 머리 보호를 보므로, 머리 보호 단계까지 왔으면 웅크린 것이다 */
    private void rememberDoneSteps(PoseCheck check) {
        if (check == PoseCheck.NOT_PROTECTED || check == PoseCheck.CORRECT) {
            crouched = true;
        }
        if (check == PoseCheck.CORRECT) {
            covered = true;
        }
    }

    /** 성공이나 시간 초과로 한 번의 시도가 끝나면 서버에 채점을 맡기고 기록으로 남긴다 */
    private void submitResult(boolean success, long elapsedMs) {
        lastResult = null;
        if (!recordRepository.isLoggedIn()) {
            return;
        }

        List<String> done = new ArrayList<>();
        if (fire) {
            if (covered) done.add(SimulationRequest.STEP_COVER_MOUTH);
        } else {
            if (crouched) done.add(SimulationRequest.STEP_CROUCH);
            if (covered) done.add(SimulationRequest.STEP_COVER_HEAD);
        }

        SimulationRequest request = new SimulationRequest(
                fire ? SimulationRequest.FIRE : SimulationRequest.EARTHQUAKE,
                success, elapsedMs, done);
        recordRepository.saveSimulation(request, result -> {
            if (result.isSuccess()) {
                lastResult = result.getData();
            } else {
                Log.w(TAG, "시뮬레이션 결과 저장 실패: " + result.getCode());
            }
        });
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
            binding.quakeEffect.setShaking(currentStep().shaking
                    && phase == PoseTrainingSession.Phase.GUIDING);
        }

        switch (phase) {
            case SUCCESS:
                String done = getString(fire
                        ? R.string.story_fire_pose_done : R.string.story_quake_pose_done);
                showMessage(done, true);
                speak(done);
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
        if (!isPoseStep()) {
            // 고르기 장면: 맞는 답을 고르기 전에는 선택지만, 고른 뒤에는 다음 버튼만 보여준다
            binding.btnStatus.setVisibility(choiceSolved ? View.VISIBLE : View.GONE);
            binding.btnStatus.setText(isLastStep() ? R.string.sim_finish : R.string.story_next);
            binding.btnStatus.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.camera_success));
            return;
        }
        binding.btnStatus.setVisibility(View.VISIBLE);

        PoseTrainingSession.Phase phase = session.getPhase();
        int textRes;
        int colorRes = R.color.orange_primary;
        if (permissionDenied) {
            textRes = R.string.sim_camera_permission_retry;
        } else if (phase == PoseTrainingSession.Phase.SUCCESS) {
            textRes = isLastStep() ? R.string.sim_finish : R.string.story_next;
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
        if (!isPoseStep()) {
            if (choiceSolved) nextStep();
            return;
        }
        if (permissionDenied) {
            permissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        switch (session.getPhase()) {
            case SUCCESS:
                nextStep();
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
        crouched = false;
        covered = false;
        lastResult = null;
        if (!fire) {
            binding.quakeEffect.setShaking(currentStep().shaking);
        }
        setGuideAccent(false);
        showMessage(messageFor(fire ? PoseCheck.NOT_PROTECTED : PoseCheck.NOT_LOW));
        applyButton();
    }

    // ---- 이야기 진행 ----

    private StoryStep currentStep() {
        return steps.get(stepIndex);
    }

    private boolean isPoseStep() {
        return currentStep().type == StoryStep.Type.POSE;
    }

    private boolean isLastStep() {
        return stepIndex >= steps.size() - 1;
    }

    private void nextStep() {
        if (isLastStep()) {
            finishTraining();
        } else {
            showStep(stepIndex + 1);
        }
    }

    private void showStep(int index) {
        stepIndex = index;
        StoryStep step = currentStep();
        boolean pose = step.type == StoryStep.Type.POSE;
        choiceSolved = false;
        lastAnnounced = null;

        binding.badgeScenario.setText(getString(R.string.sim_running_badge_step,
                scenario, index + 1, steps.size()));
        if (step.image != 0) {
            binding.storyImage.setImageResource(step.image);
        }
        // 고르기 장면에서는 상황 문장은 그림 위에, 피드백은 그림 밑에 쓰므로 안내 카드는 숨긴다
        binding.guideCard.setVisibility(pose ? View.VISIBLE : View.GONE);
        binding.storyFeedback.setText(null);
        binding.storyFeedback.setVisibility(pose ? View.GONE : View.INVISIBLE);
        // 선택지를 고른 뒤 버튼 수가 줄어도 그림 크기가 바뀌지 않게 자리를 잡아 둔다
        binding.ctaArea.setMinimumHeight(pose ? 0
                : getResources().getDimensionPixelSize(R.dimen.story_choice_area_height));
        // 카메라는 자세를 해 볼 때만 보여주고, 고르는 동안에는 장면 카드를 보여준다
        binding.scenePanel.setVisibility(pose ? View.GONE : View.VISIBLE);
        binding.cameraPreview.setVisibility(pose ? View.VISIBLE : View.INVISIBLE);
        setGuideAccent(false);
        if (!fire) {
            // 흔들림 효과는 지금 보이는 쪽을 흔든다
            binding.quakeEffect.setShakeTarget(pose ? binding.cameraPreview : binding.scenePanel);
            binding.quakeEffect.setShaking(step.shaking);
        }
        if (pose) {
            ensureCamera();
        } else {
            stopCamera();
        }

        for (int i = 0; i < choiceButtons.length; i++) {
            MaterialButton button = choiceButtons[i];
            boolean has = !pose && i < step.choices.size();
            button.setVisibility(has ? View.VISIBLE : View.GONE);
            if (has) {
                String label = getString(step.choices.get(i).label);
                button.setText(label);
                button.setContentDescription(label);
                button.setEnabled(true);
                button.setAlpha(1f);
            }
        }

        if (pose) {
            session.restart();
        }
        String scene = getString(step.scene);
        binding.sceneCaption.setText(scene);
        showMessage(pose && permissionDenied
                ? getString(R.string.sim_camera_permission_denied) : scene, true);
        speak(scene);
        applyButton();
    }

    private void onChoiceClicked(int index) {
        if (isPoseStep() || choiceSolved) return;
        StoryStep.Choice choice = currentStep().choices.get(index);
        String feedback = getString(choice.feedback);

        if (choice.correct) {
            choiceSolved = true;
            for (MaterialButton button : choiceButtons) {
                button.setVisibility(View.GONE);
            }
            setGuideAccent(true);
            showFeedback(feedback, true);
            speak(feedback);
        } else {
            // 틀린 답은 흐리게 두어 다시 고르지 않게 한다. 실패로 끝나지 않는다
            MaterialButton button = choiceButtons[index];
            button.setEnabled(false);
            button.setAlpha(0.4f);
            button.setContentDescription(getString(R.string.cd_story_choice_wrong,
                    getString(choice.label)));
            String message = feedback + "\n" + getString(R.string.story_try_again);
            showFeedback(message, false);
            speak(message);
        }
        applyButton();
    }

    /** 고르기 장면에서 고른 결과를 그림 밑에 보여준다. 맞으면 초록, 틀리면 연한 주황 */
    private void showFeedback(String message, boolean good) {
        binding.storyFeedback.setText(message);
        binding.storyFeedback.setTextColor(ContextCompat.getColor(this,
                good ? R.color.camera_success : R.color.orange_pale));
        binding.storyFeedback.setVisibility(View.VISIBLE);
        showMessage(message, true);
    }

    private void setGuideAccent(boolean good) {
        binding.guideCard.setBackgroundResource(good
                ? R.drawable.bg_camera_guide_success : R.drawable.bg_camera_guide);
        binding.guideLabel.setTextColor(ContextCompat.getColor(this,
                good ? R.color.camera_success : R.color.orange_primary));
    }

    private void speak(String text) {
        if (readAloud) {
            voice.speak(text.replace("\n", " "), () -> { });
        }
    }

    private void showMessage(@StringRes int res) {
        showMessage(getString(res), true);
    }

    private void showMessage(String message, boolean announce) {
        binding.guideMessage.setText(message);
        binding.guideCard.setContentDescription(getString(R.string.sim_ai_guide) + ", " + message);
        if (announce && !message.equals(lastAnnounced)) {
            lastAnnounced = message;
            // 안내 카드가 숨겨진 고르기 장면에서도 읽히도록 화면 전체에서 알린다
            binding.simRunRoot.announceForAccessibility(message);
        }
    }

    private void finishTraining() {
        Intent intent = new Intent(this, TrainingCompleteActivity.class);
        intent.putExtra(TrainingCompleteActivity.EXTRA_TITLE_RES, R.string.complete_training);
        // 서버 채점이 아직 안 왔거나 실패했으면 체크리스트 없이 보여준다
        if (lastResult != null && !lastResult.getKeywords().isEmpty()) {
            List<KeywordResponse> keywords = lastResult.getKeywords();
            String[] labels = new String[keywords.size()];
            boolean[] passed = new boolean[keywords.size()];
            for (int i = 0; i < keywords.size(); i++) {
                labels[i] = keywords.get(i).getLabel();
                passed[i] = keywords.get(i).isPassed();
            }
            intent.putExtra(TrainingCompleteActivity.EXTRA_CHECK_LABEL_TEXTS, labels);
            intent.putExtra(TrainingCompleteActivity.EXTRA_CHECK_RESULTS, passed);
        }
        startActivity(intent);
        finish();
    }
}
