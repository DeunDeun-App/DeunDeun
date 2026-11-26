package com.example.deundeun.ui.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.deundeun.data.network.dto.record.RecordResponse;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class NotificationRulesTest {

    private static final Gson GSON = new Gson();

    private static RecordResponse.Item record(String type, String scenario, String performedAt) {
        return GSON.fromJson("{\"trainingType\":\"" + type + "\",\"scenario\":\"" + scenario
                + "\",\"performedAt\":\"" + performedAt + "\"}", RecordResponse.Item.class);
    }

    @Test
    public void countToday_onlyCountsTodaysPoseTrainingOfThatScenario() {
        List<RecordResponse.Item> records = Arrays.asList(
                record("POSE", "지진 대피", "2026-11-26T09:00"),
                record("POSE", "지진 대피", "2026-11-26T10:30"),
                record("POSE", "지진 대피", "2026-11-25T23:59"),   // 어제
                record("POSE", "화재 대피", "2026-11-26T11:00"),
                record("CALL119", "화재 신고", "2026-11-26T12:00")); // 119 연습

        assertEquals(2, NotificationRules.countToday(records, NotificationRules.EARTHQUAKE_TITLE, "2026-11-26"));
        assertEquals(1, NotificationRules.countToday(records, NotificationRules.FIRE_TITLE, "2026-11-26"));
    }

    @Test
    public void remaining_neverGoesBelowZero() {
        assertEquals(3, NotificationRules.remaining(0));
        assertEquals(1, NotificationRules.remaining(2));
        assertEquals(0, NotificationRules.remaining(5));
    }

    @Test
    public void disasterKind_prefersWarningName() {
        assertEquals("호우주의보",
                NotificationRules.disasterKind("[수원시] 오늘 21시 호우주의보 발효. 하천 접근 금지", "안전안내"));
        assertEquals("대설경보", NotificationRules.disasterKind("대설경보 발령, 외출 자제", null));
    }

    @Test
    public void disasterKind_fallsBackToKeywordThenStep() {
        assertEquals("지진", NotificationRules.disasterKind("경북 경주 규모 3.1 지진 발생", "긴급재난"));
        assertEquals("안전안내", NotificationRules.disasterKind("어르신을 찾습니다", "안전안내"));
        assertNull(NotificationRules.disasterKind(null, " "));
    }

    @Test
    public void shortRegion_keepsLastPartOfFirstRegion() {
        assertEquals("수원시", NotificationRules.shortRegion("경기도 수원시"));
        assertEquals("수원시", NotificationRules.shortRegion("경기도 수원시, 경기도 용인시"));
        assertNull(NotificationRules.shortRegion(""));
    }

    @Test
    public void parseSentAt_readsServerFormat() {
        assertTrue(NotificationRules.parseSentAt("2026/11/26 09:30:00") > 0);
        assertEquals(-1, NotificationRules.parseSentAt("bad"));
        assertEquals("2026-11-26",
                NotificationRules.today(NotificationRules.parseSentAt("2026/11/26 09:30:00")));
    }
}
