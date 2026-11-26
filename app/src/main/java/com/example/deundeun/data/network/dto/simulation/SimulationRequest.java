package com.example.deundeun.data.network.dto.simulation;

import java.util.List;

/** 재난 시뮬레이션 결과. 채점은 서버가 한다. */
public class SimulationRequest {

    public static final String EARTHQUAKE = "EARTHQUAKE";
    public static final String FIRE = "FIRE";

    /** 동작 이름. 서버 pose_steps.step_name 과 같다 */
    public static final String STEP_CROUCH = "CROUCH";
    public static final String STEP_COVER_HEAD = "COVER_HEAD";
    public static final String STEP_COVER_MOUTH = "COVER_MOUTH";

    private final String disaster;
    private final boolean success;
    private final long elapsedMs;
    private final List<String> passedSteps;
    private final int wrongChoices;

    /**
     * @param success     제한 시간 안에 올바른 자세를 유지했는지
     * @param elapsedMs   시작부터 성공(또는 시간 초과)까지 걸린 시간
     * @param passedSteps 한 번이라도 해낸 동작
     * @param wrongChoices 이야기 고르기 장면에서 잘못 고른 횟수. 서버가 한 번에 10점씩 깎는다
     */
    public SimulationRequest(String disaster, boolean success, long elapsedMs,
                             List<String> passedSteps, int wrongChoices) {
        this.disaster = disaster;
        this.success = success;
        this.elapsedMs = elapsedMs;
        this.passedSteps = passedSteps;
        this.wrongChoices = wrongChoices;
    }
}
