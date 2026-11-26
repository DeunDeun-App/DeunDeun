package com.example.deundeun.ui.simulation.pose;

/**
 * 프레임별 판정을 시간 흐름에 따라 묶어 훈련 단계를 정한다.
 *
 * 올바른 자세를 {@link #HOLD_MS} 동안 유지하면 성공. 시간 제한으로 끝내지는 않고,
 * 오래 걸리면 서버가 점수를 깎는다 (60초 초과 −20).
 * 시간은 밖에서 넣어줘서 단위 테스트가 쉽게 했다.
 */
public class PoseTrainingSession {

    public enum Phase { GUIDING, HOLDING, SUCCESS }

    /** 3초는 자세가 잠깐 흐트러져도 성공으로 넘어가기 쉬워서 5초로 늘렸다 */
    public static final long HOLD_MS = 5_000L;
    /**
     * 인식이 잠깐 튀어도 유지 시간을 깎지 않는 여유.
     * 자세가 흐트러져도 이어지는 경우가 많아 없앴다(0). 바르지 않은 프레임이 하나라도 나오면
     * 유지 시간을 처음부터 다시 센다. 인식이 자주 끊기면 이 값을 조금 늘린다
     */
    private static final long GRACE_MS = 0L;

    private long startedAt = -1;
    private long holdStartedAt = -1;
    private long lastCorrectAt = -1;
    private Phase phase = Phase.GUIDING;

    public void restart() {
        startedAt = -1;
        holdStartedAt = -1;
        lastCorrectAt = -1;
        phase = Phase.GUIDING;
    }

    public Phase update(PoseCheck check, long nowMs) {
        if (phase == Phase.SUCCESS) {
            return phase;
        }
        if (startedAt < 0) {
            startedAt = nowMs;
        }

        if (check == PoseCheck.CORRECT) {
            if (holdStartedAt < 0) holdStartedAt = nowMs;
            lastCorrectAt = nowMs;
        } else if (holdStartedAt >= 0 && nowMs - lastCorrectAt > GRACE_MS) {
            holdStartedAt = -1;
        }

        if (holdStartedAt >= 0 && nowMs - holdStartedAt >= HOLD_MS) {
            phase = Phase.SUCCESS;
        } else {
            phase = holdStartedAt >= 0 ? Phase.HOLDING : Phase.GUIDING;
        }
        return phase;
    }

    public Phase getPhase() {
        return phase;
    }

    /** 첫 프레임부터 지금까지 걸린 시간. 아직 시작 전이면 0 */
    public long elapsedMs(long nowMs) {
        return startedAt < 0 ? 0 : Math.max(0, nowMs - startedAt);
    }

    /** 유지 단계에서 남은 초 (올림) */
    public int remainingHoldSeconds(long nowMs) {
        if (holdStartedAt < 0) return (int) (HOLD_MS / 1000);
        long left = HOLD_MS - (nowMs - holdStartedAt);
        return (int) Math.max(1, (left + 999) / 1000);
    }
}
