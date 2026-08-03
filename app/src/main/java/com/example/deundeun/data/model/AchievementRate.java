package com.example.deundeun.data.model;

/** Figma 101:65 — "항목별 달성률" 한 줄. */
public class AchievementRate {

    private final String label;
    private final int percent;

    public AchievementRate(String label, int percent) {
        this.label = label;
        this.percent = percent;
    }

    public String getLabel() {
        return label;
    }

    public int getPercent() {
        return percent;
    }
}
