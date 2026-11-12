package com.example.deundeun.ui.simulation.pose;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.ByteBufferExtractor;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Optional;

/**
 * CameraX 프레임을 MediaPipe 에 넘기는 래퍼.
 *
 * 몸 자세는 PoseLandmarker 로 본다. 손 모델을 켜면(화재) 같은 프레임을 HandLandmarker 에도 넘긴다.
 * 두 결과를 한 프레임으로 묶어야 해서 VIDEO 모드로 분석 스레드에서 차례로 돌린다.
 * 결과는 분석 스레드에서 {@link Listener} 로 돌아온다.
 */
public class PoseLandmarkerHelper {

    public interface Listener {
        /**
         * @param handResult 손 모델을 켜지 않았으면 null
         * @param person     사람만 남기고 나머지를 투명하게 만든 프레임. 사람 오려내기를 켜지 않았거나
         *                   마스크가 없으면 null
         */
        void onResult(PoseLandmarkerResult poseResult, @Nullable HandLandmarkerResult handResult,
                      @Nullable Bitmap person, int imageWidth, int imageHeight);

        void onError(String message);
    }

    private static final String TAG = "PoseLandmarkerHelper";
    private static final String POSE_MODEL_PATH = "pose_landmarker_lite.task";
    private static final String HAND_MODEL_PATH = "hand_landmarker.task";
    /**
     * 손 모델을 같이 돌릴 때 포즈 모델은 이 프레임 수마다 한 번만 돌린다.
     * 화재에서 포즈는 코·입 위치만 쓰는데 얼굴은 손보다 덜 움직여서 이전 결과를 다시 써도 된다
     */
    private static final int POSE_EVERY_N_FRAMES_WITH_HANDS = 3;

    private final Listener listener;
    private final boolean segmentPerson;
    private PoseLandmarker poseLandmarker;
    private HandLandmarker handLandmarker;
    private long lastTimestampMs;
    private int frameIndex;
    private PoseLandmarkerResult lastPoseResult;

    /**
     * @param detectHands   손 모델도 돌린다 (화재)
     * @param segmentPerson 사람만 오려낸 프레임을 만든다 (지진의 가상 배경)
     */
    public PoseLandmarkerHelper(Context context, boolean detectHands, boolean segmentPerson,
                                Listener listener) {
        this.listener = listener;
        this.segmentPerson = segmentPerson;
        try {
            PoseLandmarker.PoseLandmarkerOptions poseOptions =
                    PoseLandmarker.PoseLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions(POSE_MODEL_PATH))
                            .setRunningMode(RunningMode.VIDEO)
                            .setNumPoses(1)
                            .setOutputSegmentationMasks(segmentPerson)
                            .setMinPoseDetectionConfidence(0.5f)
                            .setMinTrackingConfidence(0.5f)
                            .setMinPosePresenceConfidence(0.5f)
                            .build();
            poseLandmarker = PoseLandmarker.createFromOptions(context, poseOptions);

            if (detectHands) {
                HandLandmarker.HandLandmarkerOptions handOptions =
                        HandLandmarker.HandLandmarkerOptions.builder()
                                .setBaseOptions(baseOptions(HAND_MODEL_PATH))
                                .setRunningMode(RunningMode.VIDEO)
                                .setNumHands(2)
                                // 얼굴과 겹친 손은 신뢰도가 낮게 나와서 기본값(0.5)보다 낮춘다
                                .setMinHandDetectionConfidence(0.3f)
                                .setMinHandPresenceConfidence(0.3f)
                                .setMinTrackingConfidence(0.3f)
                                .build();
                handLandmarker = HandLandmarker.createFromOptions(context, handOptions);
            }
        } catch (RuntimeException | LinkageError e) {
            // 네이티브 라이브러리를 못 찾으면 LinkageError 가 난다. 앱을 끄지 않고 안내만 한다
            Log.e(TAG, "MediaPipe 초기화 실패", e);
            close();
            listener.onError(e.getMessage());
        }
    }

    private static BaseOptions baseOptions(String modelPath) {
        return BaseOptions.builder()
                .setModelAssetPath(modelPath)
                // 에뮬레이터는 GPU 위임이 불안정해서 CPU 로 고정
                .setDelegate(Delegate.CPU)
                .build();
    }

    /** 분석 스레드에서 호출. imageProxy 는 여기서 닫는다. */
    public void detect(@NonNull ImageProxy imageProxy, boolean mirror) {
        if (poseLandmarker == null) {
            imageProxy.close();
            return;
        }

        Bitmap frame = imageProxy.toBitmap();
        Matrix matrix = new Matrix();
        matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
        if (mirror) {
            // 전면 카메라 프리뷰는 거울처럼 보이므로 분석 이미지도 맞춰 뒤집는다
            matrix.postScale(-1f, 1f, frame.getWidth(), frame.getHeight());
        }
        imageProxy.close();

        Bitmap rotated = Bitmap.createBitmap(frame, 0, 0,
                frame.getWidth(), frame.getHeight(), matrix, true);
        MPImage mpImage = new BitmapImageBuilder(rotated).build();

        // VIDEO 모드는 타임스탬프가 반드시 늘어나야 한다
        long timestampMs = Math.max(SystemClock.uptimeMillis(), lastTimestampMs + 1);
        lastTimestampMs = timestampMs;

        try {
            boolean runPose = handLandmarker == null || lastPoseResult == null
                    || frameIndex % POSE_EVERY_N_FRAMES_WITH_HANDS == 0;
            frameIndex++;
            PoseLandmarkerResult poseResult = runPose
                    ? poseLandmarker.detectForVideo(mpImage, timestampMs) : lastPoseResult;
            lastPoseResult = poseResult;
            HandLandmarkerResult handResult = handLandmarker != null
                    ? handLandmarker.detectForVideo(mpImage, timestampMs) : null;
            Bitmap person = segmentPerson && runPose ? cutOutPerson(rotated, poseResult) : null;
            listener.onResult(poseResult, handResult, person,
                    mpImage.getWidth(), mpImage.getHeight());
        } catch (RuntimeException e) {
            Log.e(TAG, "인식 실패", e);
            listener.onError(e.getMessage());
        }
    }

    /** 사람 마스크(0~1)를 알파로 써서 사람 부분만 남긴 비트맵. 마스크가 없으면 null */
    @Nullable
    private static Bitmap cutOutPerson(Bitmap frame, PoseLandmarkerResult result) {
        Optional<List<MPImage>> masks = result.segmentationMasks();
        if (!masks.isPresent() || masks.get().isEmpty()) return null;

        MPImage mask = masks.get().get(0);
        int width = frame.getWidth();
        int height = frame.getHeight();
        if (mask.getWidth() != width || mask.getHeight() != height) {
            Log.w(TAG, "마스크 크기가 프레임과 다름: " + mask.getWidth() + "x" + mask.getHeight());
            return null;
        }

        FloatBuffer alpha = ByteBufferExtractor.extract(mask)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        int[] pixels = new int[width * height];
        frame.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i++) {
            int a = (int) (Math.max(0f, Math.min(1f, alpha.get(i))) * 255);
            pixels[i] = (a << 24) | (pixels[i] & 0x00FFFFFF);
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    public void close() {
        if (poseLandmarker != null) {
            poseLandmarker.close();
            poseLandmarker = null;
        }
        if (handLandmarker != null) {
            handLandmarker.close();
            handLandmarker = null;
        }
    }
}
