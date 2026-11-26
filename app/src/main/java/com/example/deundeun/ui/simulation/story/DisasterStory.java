package com.example.deundeun.ui.simulation.story;

import com.example.deundeun.R;
import com.example.deundeun.ui.simulation.story.StoryStep.Choice;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 지진·화재 이야기 대본.
 *
 * 발달장애인이 따라가기 쉽도록 짧게 만들었다.
 * - 장면은 네 개, 선택지는 두 개뿐이다
 * - 시간 제한이나 점수, 실패로 끝나는 일이 없다. 틀리면 이유를 듣고 다시 고른다
 * - 가운데에 카메라로 대피 자세를 직접 해 보는 장면이 하나 있다
 *
 * 내용은 행정안전부 국민재난안전포털의 행동요령을 쉬운 말로 줄였다.
 */
public final class DisasterStory {

    private DisasterStory() {
    }

    public static List<StoryStep> steps(boolean fire) {
        return fire ? FIRE : EARTHQUAKE;
    }

    private static final List<StoryStep> EARTHQUAKE = Collections.unmodifiableList(Arrays.asList(
            StoryStep.choice(R.drawable.story_quake_1, R.string.story_quake_1, true,
                    new Choice(R.string.story_quake_1_good, true, R.string.story_quake_1_good_fb),
                    new Choice(R.string.story_quake_1_bad, false, R.string.story_quake_1_bad_fb)),
            StoryStep.pose(R.string.story_quake_pose, true),
            StoryStep.choice(R.drawable.story_quake_3, R.string.story_quake_3, false,
                    new Choice(R.string.story_quake_3_bad, false, R.string.story_quake_3_bad_fb),
                    new Choice(R.string.story_quake_3_good, true, R.string.story_quake_3_good_fb)),
            StoryStep.choice(R.drawable.story_quake_4, R.string.story_quake_4, false,
                    new Choice(R.string.story_quake_4_good, true, R.string.story_quake_4_good_fb),
                    new Choice(R.string.story_quake_4_bad, false, R.string.story_quake_4_bad_fb))
    ));

    private static final List<StoryStep> FIRE = Collections.unmodifiableList(Arrays.asList(
            StoryStep.choice(R.drawable.story_fire_1, R.string.story_fire_1, false,
                    new Choice(R.string.story_fire_1_bad, false, R.string.story_fire_1_bad_fb),
                    new Choice(R.string.story_fire_1_good, true, R.string.story_fire_1_good_fb)),
            StoryStep.pose(R.string.story_fire_pose, false),
            StoryStep.choice(R.drawable.story_fire_3, R.string.story_fire_3, false,
                    new Choice(R.string.story_fire_3_good, true, R.string.story_fire_3_good_fb),
                    new Choice(R.string.story_fire_3_bad, false, R.string.story_fire_3_bad_fb)),
            StoryStep.choice(R.drawable.story_fire_4, R.string.story_fire_4, false,
                    new Choice(R.string.story_fire_4_bad, false, R.string.story_fire_4_bad_fb),
                    new Choice(R.string.story_fire_4_good, true, R.string.story_fire_4_good_fb))
    ));
}
