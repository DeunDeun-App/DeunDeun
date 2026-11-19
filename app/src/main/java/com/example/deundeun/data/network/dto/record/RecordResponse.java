package com.example.deundeun.data.network.dto.record;

import java.util.Collections;
import java.util.List;

/** 홈·기록 화면 전체. 서버 {@code GET /records} 응답. 주별 횟수는 마지막 칸이 이번 주 */
public class RecordResponse {

    private List<String> weekLabels;
    private List<Integer> simulationCounts;
    private List<Integer> callCounts;
    private List<Integer> dayMinutes;
    private List<Item> records;
    private Report call;
    private Report simulation;

    /** 최근 5주 라벨 (각 주 월요일) */
    public List<String> getWeekLabels() { return orEmpty(weekLabels); }
    public List<Integer> getSimulationCounts() { return orEmpty(simulationCounts); }
    public List<Integer> getCallCounts() { return orEmpty(callCounts); }

    /** 이번 주 월~일 요일별 훈련 시간 (분, 7칸) */
    public List<Integer> getDayMinutes() { return orEmpty(dayMinutes); }

    /** 최근 훈련, 최신순 */
    public List<Item> getRecords() { return orEmpty(records); }
    public Report getCall() { return call; }
    public Report getSimulation() { return simulation; }

    /** 훈련 1회 */
    public static class Item {

        private long sessionId;
        private String trainingType;
        private String title;
        private String scenario;
        private String performedAt;
        private int score;
        private List<KeywordResponse> keywords;

        public long getSessionId() { return sessionId; }

        /** CALL119, POSE */
        public String getTrainingType() { return trainingType; }
        public String getTitle() { return title; }
        public String getScenario() { return scenario; }

        /** "2026-11-16T10:05" */
        public String getPerformedAt() { return performedAt; }
        public int getScore() { return score; }
        public List<KeywordResponse> getKeywords() { return orEmpty(keywords); }
    }

    /** 훈련 종류 하나의 점수 변화와 항목별 달성률 */
    public static class Report {

        private List<String> scoreLabels;
        private List<Integer> scores;
        private List<Rate> rates;

        /** 오래된 것부터 */
        public List<String> getScoreLabels() { return orEmpty(scoreLabels); }
        public List<Integer> getScores() { return orEmpty(scores); }
        public List<Rate> getRates() { return orEmpty(rates); }
    }

    public static class Rate {

        private String label;
        private int percent;

        public String getLabel() { return label; }
        public int getPercent() { return percent; }
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
