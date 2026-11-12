package com.example.deundeun.ui.simulation.pose;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PoseTrainingSessionTest {

    @Test
    public void holdingCorrectPoseLongEnough_succeeds() {
        PoseTrainingSession session = new PoseTrainingSession();
        assertEquals(PoseTrainingSession.Phase.GUIDING, session.update(PoseCheck.NOT_LOW, 0));
        assertEquals(PoseTrainingSession.Phase.HOLDING, session.update(PoseCheck.CORRECT, 1_000));
        assertEquals(PoseTrainingSession.Phase.HOLDING, session.update(PoseCheck.CORRECT, 3_500));
        assertEquals(PoseTrainingSession.Phase.SUCCESS, session.update(PoseCheck.CORRECT, 4_000));
    }

    @Test
    public void shortGlitch_keepsHolding() {
        PoseTrainingSession session = new PoseTrainingSession();
        session.update(PoseCheck.CORRECT, 0);
        assertEquals(PoseTrainingSession.Phase.HOLDING, session.update(PoseCheck.NOT_PROTECTED, 800));
        assertEquals(PoseTrainingSession.Phase.SUCCESS, session.update(PoseCheck.CORRECT, 3_000));
    }

    @Test
    public void breakingPose_resetsHold() {
        PoseTrainingSession session = new PoseTrainingSession();
        session.update(PoseCheck.CORRECT, 0);
        session.update(PoseCheck.CORRECT, 1_000);
        assertEquals(PoseTrainingSession.Phase.GUIDING, session.update(PoseCheck.NOT_LOW, 2_100));
        assertEquals(PoseTrainingSession.Phase.HOLDING, session.update(PoseCheck.CORRECT, 2_500));
        assertEquals(3, session.remainingHoldSeconds(2_500));
    }

    @Test
    public void notCorrectWithinTimeout_asksRetry() {
        PoseTrainingSession session = new PoseTrainingSession();
        session.update(PoseCheck.NOT_LOW, 0);
        assertEquals(PoseTrainingSession.Phase.RETRY,
                session.update(PoseCheck.NOT_LOW, PoseTrainingSession.TIMEOUT_MS));

        session.restart();
        assertEquals(PoseTrainingSession.Phase.GUIDING, session.update(PoseCheck.NOT_LOW, 50_000));
    }
}
