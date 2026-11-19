package com.example.deundeun.ui.simulation.story;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 재난 이야기의 한 장면.
 *
 * 두 가지가 있다.
 * - 고르기: 상황을 보여주고 두 행동 중 하나를 고른다. 틀리면 이유를 알려주고 다시 고르게 한다.
 * - 따라 하기: 카메라 앞에서 대피 자세를 취한다. 판정은 기존 자세 인식이 한다.
 */
public final class StoryStep {

    public enum Type { CHOICE, POSE }

    /** 고르기 장면의 선택지 하나 */
    public static final class Choice {
        @StringRes public final int label;
        public final boolean correct;
        /** 고른 뒤 보여줄 말. 맞으면 칭찬, 틀리면 왜 위험한지 */
        @StringRes public final int feedback;

        public Choice(@StringRes int label, boolean correct, @StringRes int feedback) {
            this.label = label;
            this.correct = correct;
            this.feedback = feedback;
        }
    }

    public final Type type;
    /** 장면 그림. 따라 하기 장면은 카메라를 보여주므로 0 */
    @DrawableRes public final int image;
    /** 상황 설명. 따라 하기 장면에서는 자세를 시작하기 전에 읽어 준다 */
    @StringRes public final int scene;
    /** 이 장면 동안 지진 흔들림을 보여줄지 */
    public final boolean shaking;
    @NonNull public final List<Choice> choices;

    private StoryStep(Type type, @DrawableRes int image, @StringRes int scene, boolean shaking,
                      @NonNull List<Choice> choices) {
        this.type = type;
        this.image = image;
        this.scene = scene;
        this.shaking = shaking;
        this.choices = choices;
    }

    public static StoryStep choice(@DrawableRes int image, @StringRes int scene, boolean shaking,
                                   Choice... choices) {
        return new StoryStep(Type.CHOICE, image, scene, shaking,
                Collections.unmodifiableList(Arrays.asList(choices)));
    }

    public static StoryStep pose(@StringRes int scene, boolean shaking) {
        return new StoryStep(Type.POSE, 0, scene, shaking, Collections.emptyList());
    }
}
