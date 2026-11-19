package com.example.deundeun.data.model;

import androidx.annotation.Nullable;

import com.example.deundeun.data.network.dto.record.RecordResponse;

import java.util.List;

/** 홈 상단 "이번 주 훈련 기록" 카드에 쓰이는 주간 집계. */
public class WeeklySummary {

    /** 월~일 7칸. 단위는 분. */
    private final int[] minutesByDay;
    private final int simulationCount;
    private final int emergencyCallCount;

    public WeeklySummary(int[] minutesByDay, int simulationCount, int emergencyCallCount) {
        if (minutesByDay == null || minutesByDay.length != 7) {
            throw new IllegalArgumentException("minutesByDay 는 월~일 7칸이어야 합니다.");
        }
        this.minutesByDay = minutesByDay.clone();
        this.simulationCount = simulationCount;
        this.emergencyCallCount = emergencyCallCount;
    }

    /** 서버 기록으로 만든다. null 이면 모두 0 */
    public static WeeklySummary from(@Nullable RecordResponse data) {
        int[] minutes = new int[7];
        if (data == null) {
            return new WeeklySummary(minutes, 0, 0);
        }
        List<Integer> dayMinutes = data.getDayMinutes();
        for (int day = 0; day < 7 && day < dayMinutes.size(); day++) {
            Integer value = dayMinutes.get(day);
            minutes[day] = value == null ? 0 : value;
        }
        return new WeeklySummary(minutes, thisWeek(data.getSimulationCounts()),
                thisWeek(data.getCallCounts()));
    }

    /** 주별 횟수의 마지막 칸이 이번 주 */
    private static int thisWeek(List<Integer> counts) {
        if (counts.isEmpty()) {
            return 0;
        }
        Integer value = counts.get(counts.size() - 1);
        return value == null ? 0 : value;
    }

    public int[] getMinutesByDay() {
        return minutesByDay.clone();
    }

    public int getTotalMinutes() {
        int total = 0;
        for (int minutes : minutesByDay) {
            total += minutes;
        }
        return total;
    }

    /** 막대 높이 비율 계산의 기준값. 모두 0이면 1을 돌려 0으로 나누는 것을 막는다. */
    public int getMaxMinutes() {
        int max = 0;
        for (int minutes : minutesByDay) {
            max = Math.max(max, minutes);
        }
        return max == 0 ? 1 : max;
    }

    public int getSimulationCount() {
        return simulationCount;
    }

    public int getEmergencyCallCount() {
        return emergencyCallCount;
    }
}
