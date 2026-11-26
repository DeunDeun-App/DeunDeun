package com.example.deundeun.ui.simulation.story;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class DisasterStoryTest {

    @Test
    public void earthquakeStory_isWellFormed() {
        assertWellFormed(DisasterStory.steps(false));
    }

    @Test
    public void fireStory_isWellFormed() {
        assertWellFormed(DisasterStory.steps(true));
    }

    @Test
    public void earthquakeShakes_onlyUntilThePoseIsDone() {
        List<StoryStep> steps = DisasterStory.steps(false);
        int pose = poseIndex(steps);
        for (int i = 0; i < steps.size(); i++) {
            assertEquals("step " + i, i <= pose, steps.get(i).shaking);
        }
    }

    @Test
    public void fireNeverShakes() {
        for (StoryStep step : DisasterStory.steps(true)) {
            assertFalse(step.shaking);
        }
    }

    /** 장면은 짧게, 따라 하기는 하나, 고르기는 두 개 중 맞는 답이 딱 하나 */
    private static void assertWellFormed(List<StoryStep> steps) {
        assertTrue(steps.size() >= 3 && steps.size() <= 5);
        poseIndex(steps);
        for (StoryStep step : steps) {
            if (step.type == StoryStep.Type.POSE) {
                assertTrue(step.choices.isEmpty());
                assertEquals(0, step.image);
                continue;
            }
            assertTrue("choice scene has a picture", step.image != 0);
            assertEquals(2, step.choices.size());
            int correct = 0;
            for (StoryStep.Choice choice : step.choices) {
                if (choice.correct) correct++;
            }
            assertEquals(1, correct);
        }
    }

    private static int poseIndex(List<StoryStep> steps) {
        int found = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).type == StoryStep.Type.POSE) {
                assertEquals("only one pose step", -1, found);
                found = i;
            }
        }
        assertTrue("has a pose step", found >= 0);
        return found;
    }
}
