package com.example.deundeun.data.model;

import androidx.annotation.Nullable;

import com.example.deundeun.data.network.dto.record.KeywordResponse;
import com.example.deundeun.data.network.dto.record.RecordResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 훈련 1회의 결과. 홈 "최근 훈련 기록"과 기록탭이 공유한다. */
public class TrainingRecord {

    private final String title;
    private final String scenario;
    private final String performedAt;
    private final int score;
    private final List<String> achievements;
    private final List<String> missed;
    @Nullable
    private final String feedback;

    public TrainingRecord(String title, String scenario, String performedAt, int score,
                          List<String> achievements) {
        this(title, scenario, performedAt, score, achievements, null, null);
    }

    public TrainingRecord(String title, String scenario, String performedAt, int score,
                          List<String> achievements, @Nullable List<String> missed,
                          @Nullable String feedback) {
        this.title = title;
        this.scenario = scenario;
        this.performedAt = performedAt;
        this.score = score;
        this.achievements = unmodifiable(achievements);
        this.missed = unmodifiable(missed);
        this.feedback = feedback;
    }

    /** 서버 기록 한 건을 화면용으로 바꾼다. 피드백은 처음 못 한 키워드의 것을 쓴다 */
    public static TrainingRecord from(RecordResponse.Item item) {
        List<String> achievements = new ArrayList<>();
        List<String> missed = new ArrayList<>();
        String feedback = null;

        for (KeywordResponse keyword : item.getKeywords()) {
            if (keyword.isPassed()) {
                achievements.add(keyword.getLabel());
            } else {
                missed.add(keyword.getLabel());
                if (feedback == null) {
                    feedback = keyword.getFeedback();
                }
            }
        }

        return new TrainingRecord(item.getTitle(), item.getScenario(),
                formatPerformedAt(item.getPerformedAt()), item.getScore(),
                achievements, missed, feedback);
    }

    /** "2026-11-16T10:05" → "11월 16일 10:05". 형식이 다르면 받은 그대로 쓴다 */
    static String formatPerformedAt(@Nullable String iso) {
        if (iso == null || iso.length() < 16 || iso.charAt(10) != 'T') {
            return iso == null ? "" : iso;
        }
        try {
            int month = Integer.parseInt(iso.substring(5, 7));
            int day = Integer.parseInt(iso.substring(8, 10));
            return month + "월 " + day + "일 " + iso.substring(11, 16);
        } catch (NumberFormatException e) {
            return iso;
        }
    }

    private static List<String> unmodifiable(@Nullable List<String> list) {
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public String getTitle() {
        return title;
    }

    /** 예: "지진 대피" */
    public String getScenario() {
        return scenario;
    }

    /** 예: "7월 17일 10:05" */
    public String getPerformedAt() {
        return performedAt;
    }

    public int getScore() {
        return score;
    }

    /** 해낸 키워드. 예: ["대피 자세"], ["위치 전달", "상황 설명"] */
    public List<String> getAchievements() {
        return achievements;
    }

    public List<String> getMissed() {
        return missed;
    }

    /** 못 한 키워드의 피드백 한 줄. 다 해냈으면 null */
    @Nullable
    public String getFeedback() {
        return feedback;
    }

    /** 목록에 표시하는 "지진 대피 · 7월 17일 10:05" 형태의 한 줄. */
    public String getMetaLine() {
        return scenario + " · " + performedAt;
    }
}
