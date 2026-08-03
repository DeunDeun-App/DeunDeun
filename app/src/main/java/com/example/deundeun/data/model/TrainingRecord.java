package com.example.deundeun.data.model;

import java.util.Collections;
import java.util.List;

/** 훈련 1회의 결과. 홈 "최근 훈련 기록"과 기록탭이 공유한다. */
public class TrainingRecord {

    private final String title;
    private final String scenario;
    private final String performedAt;
    private final int score;
    private final List<String> achievements;

    public TrainingRecord(String title, String scenario, String performedAt, int score,
                          List<String> achievements) {
        this.title = title;
        this.scenario = scenario;
        this.performedAt = performedAt;
        this.score = score;
        this.achievements = achievements == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(achievements);
    }

    public String getTitle() {
        return title;
    }

    /** 예: "지진 대피" */
    public String getScenario() {
        return scenario;
    }

    /** 예: "7월 17일 10:05". 서버 연동 시 ISO-8601 을 받아 여기서 포맷한다. */
    public String getPerformedAt() {
        return performedAt;
    }

    public int getScore() {
        return score;
    }

    /** 예: ["대피 자세"], ["위치 전달", "상황 설명"] */
    public List<String> getAchievements() {
        return achievements;
    }

    /** 목록에 표시하는 "지진 대피 · 7월 17일 10:05" 형태의 한 줄. */
    public String getMetaLine() {
        return scenario + " · " + performedAt;
    }
}
