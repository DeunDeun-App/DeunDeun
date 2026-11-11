package com.example.deundeun.ui.simulation.pose;

/**
 * 프레임별 판정을 시간 흐름에 따라 묶어 훈련 단계를 정한다.
 *
 * 올바른 자세를 {@link #HOLD_MS} 동안 유지하면 성공, {@link #TIMEOUT_MS} 안에 못 하면 재훈련.
 * 시간은 밖에서 넣어줘서 단위 테스트가 쉽게 했다.
 */
public class PoseTrainingSession {

    public enum Phase { GUIDING, HOLDING, SUCCESS, RETRY }

    public static final long HOLD_MS = 3_000L;
    public static final long TIMEOUT_MS = 60_000L;
    /**
     * 인식이 잠깐 튀어도 유지 시간을 깎지 않는 여유.
     * 에뮬레이터처럼 초당 몇 프레임밖에 안 나오면 한두 프레임 놓쳐도 0.5초를 넘겨서 1초로 잡았다
     */
    private static final long GRACE_MS = 1_000L;

    private long startedAt = -1;
    private long holdStartedAt = -1;
    private long lastCorrectAt = -1;
    private Phase phase = Phase.GUIDING;
    private PoseCheck lastCheck = PoseCheck.NO_PERSON;

    public void restart() {
        startedAt = -1;
        holdStartedAt = -1;
        lastCorrectAt = -1;
        phase = Phase.GUIDING;
        lastCheck = PoseCheck.NO_PERSON;
    }

    public Phase update(PoseCheck check, long nowMs) {
        if (phase == Phase.SUCCESS || phase == Phase.RETRY) {
            return phase;
        }
        if (startedAt < 0) {
            startedAt = nowMs;
        }
        lastCheck = check;

        if (check == PoseCheck.CORRECT) {
            if (holdStartedAt < 0) holdStartedAt = nowMs;
            lastCorrectAt = nowMs;
        } else if (holdStartedAt >= 0 && nowMs - lastCorrectAt > GRACE_MS) {
            holdStartedAt = -1;
        }

        if (holdStartedAt >= 0 && nowMs - holdStartedAt >= HOLD_MS) {
            phase = Phase.SUCCESS;
        } else if (nowMs - startedAt >= TIMEOUT_MS) {
            phase = Phase.RETRY;
        } else {
            phase = holdStartedAt >= 0 ? Phase.HOLDING : Phase.GUIDING;
        }
        return phase;
    }

    public Phase getPhase() {
        return phase;
    }

    public PoseCheck getLastCheck() {
        return lastCheck;
    }

    /** 유지 단계에서 남은 초 (올림) */
    public int remainingHoldSeconds(long nowMs) {
        if (holdStartedAt < 0) return (int) (HOLD_MS / 1000);
        long left = HOLD_MS - (nowMs - holdStartedAt);
        return (int) Math.max(1, (left + 999) / 1000);
    }
}
