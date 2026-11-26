package com.example.deundeun.ui.notification;

import androidx.annotation.Nullable;

import com.example.deundeun.data.network.dto.record.RecordResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 알림 목록을 만드는 규칙. 화면과 떼어 두어 단위 테스트로 확인한다.
 *
 * - 재난문자 도착: 내 지역의 가장 최근 재난문자를 "호우주의보 - 수원시" 처럼 줄여 보여준다
 * - 오늘 훈련: 지진 대피, 화재 대피를 하루에 각각 {@link #DAILY_GOAL} 번씩 하는 것이 목표다
 */
final class NotificationRules {

    /** 하루 목표 횟수 (재난 종류마다) */
    static final int DAILY_GOAL = 3;

    /** 서버 pose_scenarios.pose_title 과 같은 이름 */
    static final String EARTHQUAKE_TITLE = "지진 대피";
    static final String FIRE_TITLE = "화재 대피";

    private static final String TYPE_POSE = "POSE";

    // "호우주의보", "대설경보", "폭염특보" 처럼 끝이 주의보·경보·특보인 낱말
    private static final Pattern WARNING = Pattern.compile("([가-힣]{1,6}(?:주의보|경보|특보))");

    // 주의보·경보 이름이 없을 때 본문에서 찾을 재난 이름. 앞에 있을수록 먼저 본다
    private static final String[] KINDS = {
            "지진", "해일", "화재", "산불", "홍수", "호우", "태풍", "폭염", "한파", "대설", "강풍",
            "황사", "미세먼지", "정전", "단수", "감염병", "실종"};

    private NotificationRules() {
    }

    /** 오늘(yyyy-MM-dd) 끝낸 자세 훈련 중 시나리오가 {@code scenarioTitle} 인 것의 수 */
    static int countToday(List<RecordResponse.Item> records, String scenarioTitle, String today) {
        int count = 0;
        for (RecordResponse.Item item : records) {
            String at = item.getPerformedAt();
            if (TYPE_POSE.equals(item.getTrainingType())
                    && scenarioTitle.equals(item.getScenario())
                    && at != null && at.startsWith(today)) {
                count++;
            }
        }
        return count;
    }

    static int remaining(int done) {
        return Math.max(0, DAILY_GOAL - done);
    }

    /** 재난문자 본문에서 재난 이름을 찾는다. 못 찾으면 {@code step} (예: 안전안내), 그것도 없으면 null */
    @Nullable
    static String disasterKind(@Nullable String text, @Nullable String step) {
        if (text != null) {
            Matcher matcher = WARNING.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
            for (String kind : KINDS) {
                if (text.contains(kind)) {
                    return kind;
                }
            }
        }
        return step == null || step.trim().isEmpty() ? null : step.trim();
    }

    /** "경기도 수원시" → "수원시". 여러 지역이면 첫 지역만 본다 */
    @Nullable
    static String shortRegion(@Nullable String region) {
        if (region == null || region.trim().isEmpty()) {
            return null;
        }
        String first = region.split(",")[0].trim();
        String[] tokens = first.split("\\s+");
        return tokens[tokens.length - 1];
    }

    /** 서버 재난문자 발송 시각 "yyyy/MM/dd HH:mm:ss" 를 밀리초로. 못 읽으면 -1 */
    static long parseSentAt(@Nullable String sentAt) {
        if (sentAt == null) {
            return -1;
        }
        try {
            Date date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.KOREA).parse(sentAt.trim());
            return date == null ? -1 : date.getTime();
        } catch (ParseException e) {
            return -1;
        }
    }

    static String today(long nowMs) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date(nowMs));
    }
}
