package com.example.deundeun.data;

import com.example.deundeun.data.model.AchievementRate;
import com.example.deundeun.data.model.GuardianNotice;
import com.example.deundeun.data.model.TrainingRecord;
import com.example.deundeun.data.model.UserProfile;
import com.example.deundeun.data.model.WeeklySummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 화면 개발용 임시 데이터 소스. 값은 Figma 화면설계서의 더미와 동일하다.
 *
 * <p>API 스펙이 확정되면 이 클래스만 Retrofit 구현으로 갈아끼우고
 * 화면 코드는 건드리지 않는다. 그래서 반환 타입을 서버 응답 DTO 가 아니라
 * 화면이 쓰는 도메인 모델로 두었다.
 */
public class FakeTrainingRepository {

    public WeeklySummary getWeeklySummary() {
        // Figma 29:57 / 29:58 / 29:59 의 막대 높이 비율(68.2 / 46 / 85.5)에 맞춘 값
        return new WeeklySummary(new int[]{35, 24, 44, 7, 0, 0, 0}, 3, 5);
    }

    /** 목록 화면에 한 번에 보여줄 최대 개수. 그보다 많으면 "전체 보기"로 넘긴다. */
    public static final int PREVIEW_LIMIT = 5;

    /** "전체 보기"에서 볼 수 있는 최대 개수. */
    public static final int FULL_LIMIT = 15;

    /** 홈·기록탭에 미리 보여줄 목록. 최대 {@link #PREVIEW_LIMIT}개. */
    public List<TrainingRecord> getRecentRecords() {
        return limit(getAllRecords(), PREVIEW_LIMIT);
    }

    /** "전체 보기" 화면용. 최대 {@link #FULL_LIMIT}개. */
    public List<TrainingRecord> getAllRecords() {
        List<TrainingRecord> records = new ArrayList<>();
        records.add(new TrainingRecord(
                "재난 시뮬레이션", "지진 대피", "7월 17일 10:05", 92,
                Arrays.asList("대피 자세")));
        records.add(new TrainingRecord(
                "119 신고 연습", "화재 상황", "7월 15일 16:24", 88,
                Arrays.asList("위치 전달", "상황 설명")));
        records.add(new TrainingRecord(
                "재난 시뮬레이션", "화재 탈출", "7월 14일 09:40", 95,
                Arrays.asList("대피 자세", "이동 경로")));
        records.add(new TrainingRecord(
                "119 신고 연습", "지진 상황", "7월 12일 14:10", 76,
                Arrays.asList("위치 전달")));
        records.add(new TrainingRecord(
                "재난 시뮬레이션", "지진 대피", "7월 11일 11:22", 84,
                Arrays.asList("대피 자세")));
        records.add(new TrainingRecord(
                "119 신고 연습", "화재 상황", "7월 9일 17:05", 90,
                Arrays.asList("위치 전달", "상황 설명")));
        records.add(new TrainingRecord(
                "재난 시뮬레이션", "화재 탈출", "7월 8일 10:31", 71,
                Arrays.asList("이동 경로")));
        records.add(new TrainingRecord(
                "119 신고 연습", "지진 상황", "7월 6일 15:48", 88,
                Arrays.asList("위치 전달", "상황 설명")));
        return limit(records, FULL_LIMIT);
    }

    private <T> List<T> limit(List<T> items, int max) {
        return items.size() <= max ? items : new ArrayList<>(items.subList(0, max));
    }

    // ----- 기록탭 -----

    /** 주차 라벨. 막대 차트와 짝을 이룬다. */
    public String[] getWeekLabels() {
        return new String[]{"6/16", "6/23", "6/30", "7/7", "7/14"};
    }

    /** 주차별 시뮬레이션 횟수 (시안 67:961 의 막대 높이). */
    public int[] getSimulationCounts() {
        return new int[]{2, 3, 1, 4, 3};
    }

    /**
     * 주차별 119 연습 횟수. 시안에는 막대가 그려져 있지 않아 0으로 둔다.
     * 값이 들어오면 시뮬레이션 막대 옆에 나란히 그려진다.
     */
    public int[] getEmergencyCallCounts() {
        return new int[]{0, 0, 0, 0, 0};
    }

    public String[] getScoreDateLabels() {
        return new String[]{"6/20", "6/25", "6/30", "7/5", "7/10", "7/14"};
    }

    /** 119 점수 추이 (시안 101:65 의 꺾은선). */
    public int[] getScoreTrend() {
        return new int[]{62, 68, 64, 78, 85, 88};
    }

    public List<AchievementRate> getAchievementRates() {
        List<AchievementRate> rates = new ArrayList<>();
        rates.add(new AchievementRate("주소 말하기", 95));
        rates.add(new AchievementRate("이름 말하기", 87));
        rates.add(new AchievementRate("상황 설명하기", 62));
        return rates;
    }

    // ----- 내정보 -----

    public UserProfile getUserProfile() {
        return new UserProfile(
                "김든든", "010-1234-5678", "2000년 1월 1일",
                // 장애 정도는 사용자가 직접 고르게 비워 둔다. 미리 값이 박혀 있으면
                // 고르지도 않은 등급이 저장될 수 있다.
                "지적장애", "",
                "엄마", "김든둔", "010-9876-5432");
    }

    /** 내정보의 보호자 알림. 최대 {@link #PREVIEW_LIMIT}개만 보여준다. */
    public List<GuardianNotice> getGuardianNotices() {
        List<GuardianNotice> notices = new ArrayList<>();
        notices.add(new GuardianNotice(
                "위치 공유", "SOS 요청 시 GPS 위치 전송", "7월 17일", "오전 10:12"));
        notices.add(new GuardianNotice(
                "훈련 완료", "재난 시뮬레이션 92점 달성", "7월 17일", "오전 10:07"));
        notices.add(new GuardianNotice(
                "훈련 완료", "119 신고 연습 88점 달성", "7월 15일", "오후 4:26"));
        notices.add(new GuardianNotice(
                "위치 공유", "SOS 요청 시 GPS 위치 전송", "7월 14일", "오전 9:41"));
        notices.add(new GuardianNotice(
                "훈련 완료", "재난 시뮬레이션 95점 달성", "7월 14일", "오전 9:38"));
        notices.add(new GuardianNotice(
                "훈련 완료", "119 신고 연습 76점 달성", "7월 12일", "오후 2:12"));
        return limit(notices, PREVIEW_LIMIT);
    }
}
