package com.example.deundeun.ui.simulation.pose;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * MediaPipe 랜드마크로 대피 자세를 판정한다.
 *
 * 지진: 몸 전체를 웅크리고 두 팔로 머리를 감싼다. 포즈 모델만 쓰고, 다리까지 보여야 한다.
 * 화재: 손으로 코와 입을 막는다. 포즈 모델로 코·입 위치를, 손 모델로 손 모양(21개 점)을 잡아
 *       입과 코-입 사이 지점이 손 윤곽 안에 들어오는지 본다. 얼굴만 보이면 된다.
 *
 * 좌표는 0~1 로 정규화된 이미지 좌표(y 는 아래로 갈수록 커짐)이다.
 * 기준값은 실제 기기로 시험해 보며 조정한다. Logcat 에서 태그 PoseEvaluator 로 값을 볼 수 있다.
 */
public class DisasterPoseEvaluator {

    private static final String TAG = "PoseEvaluator";
    /** 이 프레임 수마다 한 번 로그를 남긴다 */
    private static final int LOG_INTERVAL_FRAMES = 15;

    // MediaPipe Pose 33개 랜드마크 중 쓰는 것
    private static final int NOSE = 0;
    private static final int LEFT_EAR = 7;
    private static final int RIGHT_EAR = 8;
    private static final int MOUTH_LEFT = 9;
    private static final int MOUTH_RIGHT = 10;
    private static final int LEFT_SHOULDER = 11;
    private static final int RIGHT_SHOULDER = 12;
    private static final int LEFT_WRIST = 15;
    private static final int RIGHT_WRIST = 16;
    private static final int LEFT_HIP = 23;
    private static final int RIGHT_HIP = 24;
    private static final int LEFT_KNEE = 25;
    private static final int RIGHT_KNEE = 26;
    private static final int LEFT_ANKLE = 27;
    private static final int RIGHT_ANKLE = 28;

    private static final float MIN_VISIBILITY = 0.5f;
    /**
     * 지진: 팔로 머리를 감싸면 손목이 머리 뒤나 팔에 가려져 가시성이 0.0~0.7 로 낮게 나온다.
     * 위치 추정은 쓸 만해서 머리 감싸기 판정은 기준을 낮춰 쓴다
     */
    private static final float MIN_WRIST_VISIBILITY = 0.1f;

    /** 지진: 무릎이 이 각도보다 굽으면 웅크린 것으로 본다 */
    private static final double CROUCH_KNEE_ANGLE = 100.0;
    /** 지진: 손목이 머리 중심에서 어깨 너비의 이 배수 안에 있어야 머리를 감싼 것 */
    private static final float HEAD_COVER_DISTANCE = 1.2f;

    private final boolean fire;

    private int frameCount;

    public DisasterPoseEvaluator(boolean fire) {
        this.fire = fire;
    }

    /**
     * @param pose  포즈 랜드마크 33개. 사람이 없으면 null
     * @param hands 손마다 랜드마크 21개. 화재가 아니거나 손이 없으면 null 또는 빈 목록
     */
    public PoseCheck evaluate(@Nullable List<NormalizedLandmark> pose,
                              @Nullable List<List<NormalizedLandmark>> hands) {
        if (fire) {
            FireResult result = judgeFire(pose, hands);
            logFire(pose, hands, result);
            return result.check;
        }
        PoseCheck check = judgeEarthquake(pose);
        logEarthquake(pose, check);
        return check;
    }

    // ===== 지진 =====

    private PoseCheck judgeEarthquake(@Nullable List<NormalizedLandmark> pose) {
        if (pose == null || pose.size() < 33
                || !visible(pose, LEFT_SHOULDER, RIGHT_SHOULDER)) {
            return PoseCheck.NO_PERSON;
        }

        float shoulderWidth = distance(pose.get(LEFT_SHOULDER), pose.get(RIGHT_SHOULDER));
        if (shoulderWidth < 0.02f) {
            return PoseCheck.NO_PERSON;
        }

        Double leftKnee = kneeAngle(pose, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE);
        Double rightKnee = kneeAngle(pose, RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE);
        // 몸 전체를 웅크렸는지 봐야 하므로 다리가 보여야 판정할 수 있다
        if (leftKnee == null && rightKnee == null) {
            return PoseCheck.NO_PERSON;
        }
        if (!isCrouched(leftKnee, rightKnee)) {
            return PoseCheck.NOT_LOW;
        }
        return isHeadCovered(pose, shoulderWidth)
                ? PoseCheck.CORRECT : PoseCheck.NOT_PROTECTED;
    }

    /** 보이는 무릎이 모두 굽어 있어야 한다 */
    private static boolean isCrouched(Double left, Double right) {
        return (left == null || left < CROUCH_KNEE_ANGLE)
                && (right == null || right < CROUCH_KNEE_ANGLE);
    }

    private boolean isHeadCovered(List<NormalizedLandmark> pose, float shoulderWidth) {
        float headX = headX(pose);
        float headY = headY(pose);
        float shoulderY = Math.min(pose.get(LEFT_SHOULDER).y(), pose.get(RIGHT_SHOULDER).y());
        int covered = 0;
        for (int wrist : new int[]{LEFT_WRIST, RIGHT_WRIST}) {
            if (visibility(pose, wrist) < MIN_WRIST_VISIBILITY) continue;
            NormalizedLandmark w = pose.get(wrist);
            if (w.y() < shoulderY
                    && Math.hypot(w.x() - headX, w.y() - headY)
                    < shoulderWidth * HEAD_COVER_DISTANCE) {
                covered++;
            }
        }
        return covered == 2;
    }

    // ===== 화재 =====

    private static final class FireResult {
        final PoseCheck check;
        /** 입이 손 윤곽 안에 있는지 */
        final boolean mouthCovered;
        /** 코와 입 사이 지점이 손 윤곽 안에 있는지 */
        final boolean noseCovered;

        FireResult(PoseCheck check, boolean mouthCovered, boolean noseCovered) {
            this.check = check;
            this.mouthCovered = mouthCovered;
            this.noseCovered = noseCovered;
        }
    }

    private FireResult judgeFire(@Nullable List<NormalizedLandmark> pose,
                                 @Nullable List<List<NormalizedLandmark>> hands) {
        // 얼굴이 보이는지만 본다. 코는 손에 가려질 수 있어서 귀로도 인정한다
        if (pose == null || pose.size() < 33
                || !(visible(pose, NOSE) || visible(pose, LEFT_EAR) || visible(pose, RIGHT_EAR))) {
            return new FireResult(PoseCheck.NO_PERSON, false, false);
        }
        if (hands == null || hands.isEmpty()) {
            return new FireResult(PoseCheck.NOT_PROTECTED, false, false);
        }

        // 손에 가려져도 포즈 모델이 추정한 코·입 위치는 쓸 만해서 가시성은 보지 않는다
        NormalizedLandmark nose = pose.get(NOSE);
        NormalizedLandmark l = pose.get(MOUTH_LEFT);
        NormalizedLandmark r = pose.get(MOUTH_RIGHT);
        float mouthX = (l.x() + r.x()) / 2f;
        float mouthY = (l.y() + r.y()) / 2f;
        float midX = (nose.x() + mouthX) / 2f;
        float midY = (nose.y() + mouthY) / 2f;

        boolean anyMouth = false;
        boolean anyNose = false;
        for (List<NormalizedLandmark> hand : hands) {
            float[][] hull = convexHull(hand);
            boolean mouth = contains(hull, mouthX, mouthY);
            boolean noseSide = contains(hull, midX, midY);
            if (mouth && noseSide) {
                return new FireResult(PoseCheck.CORRECT, true, true);
            }
            anyMouth |= mouth;
            anyNose |= noseSide;
        }
        return new FireResult(PoseCheck.NOT_PROTECTED, anyMouth, anyNose);
    }

    /** 손 랜드마크를 감싸는 볼록 다각형(반시계 방향). Andrew 의 monotone chain */
    private static float[][] convexHull(List<NormalizedLandmark> hand) {
        float[][] points = new float[hand.size()][];
        for (int i = 0; i < hand.size(); i++) {
            points[i] = new float[]{hand.get(i).x(), hand.get(i).y()};
        }
        Arrays.sort(points, (a, b) -> a[0] != b[0] ? Float.compare(a[0], b[0]) : Float.compare(a[1], b[1]));

        List<float[]> hull = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            int start = hull.size();
            for (int i = 0; i < points.length; i++) {
                float[] p = points[pass == 0 ? i : points.length - 1 - i];
                while (hull.size() >= start + 2
                        && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), p) <= 0) {
                    hull.remove(hull.size() - 1);
                }
                hull.add(p);
            }
            hull.remove(hull.size() - 1);
        }
        return hull.toArray(new float[0][]);
    }

    /** 볼록 다각형 안에 점이 있는지. 모든 변의 왼쪽에 있으면 안이다 */
    private static boolean contains(float[][] hull, float x, float y) {
        if (hull.length < 3) return false;
        float[] p = {x, y};
        for (int i = 0; i < hull.length; i++) {
            if (cross(hull[i], hull[(i + 1) % hull.length], p) < 0) return false;
        }
        return true;
    }

    private static float cross(float[] o, float[] a, float[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    // ===== 로그 =====

    private void logEarthquake(@Nullable List<NormalizedLandmark> pose, PoseCheck check) {
        if (frameCount++ % LOG_INTERVAL_FRAMES != 0) return;
        if (pose == null || pose.size() < 33) {
            Log.d(TAG, "check=" + check + " (포즈 없음)");
            return;
        }
        Log.d(TAG, String.format(Locale.US,
                "check=%s knees=%s/%s shoulders=%.2f/%.2f wrists=%.2f/%.2f",
                check,
                kneeAngle(pose, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE),
                kneeAngle(pose, RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE),
                visibility(pose, LEFT_SHOULDER), visibility(pose, RIGHT_SHOULDER),
                visibility(pose, LEFT_WRIST), visibility(pose, RIGHT_WRIST)));
    }

    private void logFire(@Nullable List<NormalizedLandmark> pose,
                         @Nullable List<List<NormalizedLandmark>> hands, FireResult result) {
        if (frameCount++ % LOG_INTERVAL_FRAMES != 0) return;
        if (pose == null || pose.size() < 33) {
            Log.d(TAG, "check=" + result.check + " (포즈 없음)");
            return;
        }
        Log.d(TAG, String.format(Locale.US,
                "check=%s hands=%d mouthCovered=%b noseCovered=%b nose=%.2f ears=%.2f/%.2f",
                result.check,
                hands == null ? 0 : hands.size(),
                result.mouthCovered, result.noseCovered,
                visibility(pose, NOSE),
                visibility(pose, LEFT_EAR), visibility(pose, RIGHT_EAR)));
    }

    // ===== 공통 =====

    /** 가려진 점도 MediaPipe 가 추정한 좌표를 주므로 가시성과 상관없이 평균을 낸다 */
    private static float headX(List<NormalizedLandmark> pose) {
        return (pose.get(NOSE).x() + pose.get(LEFT_EAR).x() + pose.get(RIGHT_EAR).x()) / 3f;
    }

    private static float headY(List<NormalizedLandmark> pose) {
        return (pose.get(NOSE).y() + pose.get(LEFT_EAR).y() + pose.get(RIGHT_EAR).y()) / 3f;
    }

    private static float visibility(List<NormalizedLandmark> pose, int index) {
        return pose.get(index).visibility().orElse(0f);
    }

    /** 엉덩이-무릎-발목 각도. 셋 중 하나라도 안 보이면 null */
    private static Double kneeAngle(List<NormalizedLandmark> pose, int hip, int knee, int ankle) {
        if (!visible(pose, hip, knee, ankle)) return null;
        NormalizedLandmark h = pose.get(hip);
        NormalizedLandmark k = pose.get(knee);
        NormalizedLandmark a = pose.get(ankle);
        double v1x = h.x() - k.x(), v1y = h.y() - k.y();
        double v2x = a.x() - k.x(), v2y = a.y() - k.y();
        double cos = (v1x * v2x + v1y * v2y)
                / (Math.hypot(v1x, v1y) * Math.hypot(v2x, v2y) + 1e-6);
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cos))));
    }

    private static boolean visible(List<NormalizedLandmark> pose, int... indices) {
        for (int i : indices) {
            if (pose.get(i).visibility().orElse(0f) < MIN_VISIBILITY) return false;
        }
        return true;
    }

    private static float distance(NormalizedLandmark a, NormalizedLandmark b) {
        return (float) Math.hypot(a.x() - b.x(), a.y() - b.y());
    }
}
