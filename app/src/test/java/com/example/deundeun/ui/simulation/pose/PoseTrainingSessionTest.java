package com.example.deundeun.ui.simulation.pose;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PoseTrainingSessionTest {

    @Test
    public void holdingCorrectPoseLongEnough_succeeds() {
        PoseTrainingSession session = new PoseTrainingSession();
        assertEquals(PoseTrainingSession.Phase.GUIDING, session.update(PoseCheck.NOT_LOW, 0));
        assertEquals(PoseTrainingSession.Phase.HOLDING, session.update(PoseCheck.CORRECT, 1_000));
        assertEquals(PoseTrainingSession.Phase.HOLDING, session.update(PoseCheck.CORRECT, 5_500));
        assertEquals(PoseTrainingSession.Phase.SUCCESS, session.update(PoseCheck.CORRECT, 6_000));
    }

    @Test
    public void anyBrokenFrame_restartsHold() {
        PoseTrainingSession session = new PoseTrainingSession();
        session.update(PoseCheck.CORRECT, 0);
        // 여유 시간이 없어서 한 프레임만 흐트러져도 유지가 끊긴다
        assertEquals(PoseTrainingSession.Phase.GUIDING, session.update(PoseCheck.NOT_PROTECTED, 100));
        assertEquals(PoseTrainingSession.Phase.HOLDING, session.update(PoseCheck.CORRECT, 200));
        assertEquals(PoseTrainingSession.Phase.HOLDING, session.update(PoseCheck.CORRECT, 5_100));
        assertEquals(PoseTrainingSession.Phase.SUCCESS, session.update(PoseCheck.CORRECT, 5_200));
    }

    @Test
    public void breakingPose_resetsHold() {
        PoseTrainingSession session = new PoseTrainingSession();
        session.update(PoseCheck.CORRECT, 0);
        session.update(PoseCheck.CORRECT, 1_000);
        assertEquals(PoseTrainingSession.Phase.GUIDING, session.update(PoseCheck.NOT_LOW, 1_600));
        assertEquals(PoseTrainingSession.Phase.HOLDING, session.update(PoseCheck.CORRECT, 2_500));
        assertEquals(5, session.remainingHoldSeconds(2_500));
    }

    @Test
    public void takingLong_neverEndsInFailure() {
        PoseTrainingSession session = new PoseTrainingSession();
        session.update(PoseCheck.NOT_LOW, 0);
        // 시간 제한이 없어서 오래 걸려도 계속 안내한다
        assertEquals(PoseTrainingSession.Phase.GUIDING, session.update(PoseCheck.NOT_LOW, 600_000));
        assertEquals(PoseTrainingSession.Phase.HOLDING, session.update(PoseCheck.CORRECT, 600_100));
        assertEquals(PoseTrainingSession.Phase.SUCCESS, session.update(PoseCheck.CORRECT, 605_100));
        assertEquals(605_100, session.elapsedMs(605_100));
    }
}
