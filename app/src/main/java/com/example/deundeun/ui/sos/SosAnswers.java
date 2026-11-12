package com.example.deundeun.ui.sos;

import java.io.Serializable;

/**
 * SOS 질문 3가지의 답. 화면 사이로 넘겨 미리보기 문구를 만든다
 *
 * 어느 하나라도 비어 있으면 "다음"으로 넘어갈 수 없다
 */
public class SosAnswers implements Serializable {

    public static final String EXTRA = "sos_answers";

    /** 서버로 보내는 재난 코드. 질문 화면의 보기 순서와 같다. */
    private static final String[] SITUATION_CODES = {"FIRE", "EARTHQUAKE", "FLOOD", "UNKNOWN"};

    private Integer situationIndex;
    private Boolean trapped;
    private Boolean hurt;

    public Integer getSituationIndex() {
        return situationIndex;
    }

    public void setSituationIndex(Integer situationIndex) {
        this.situationIndex = situationIndex;
    }

    public Boolean getTrapped() {
        return trapped;
    }

    public void setTrapped(Boolean trapped) {
        this.trapped = trapped;
    }

    public Boolean getHurt() {
        return hurt;
    }

    public void setHurt(Boolean hurt) {
        this.hurt = hurt;
    }

    /** 답한 질문 수. 상단 진행 표시에 쓴다. */
    public int answeredCount() {
        int count = 0;
        if (situationIndex != null) {
            count++;
        }
        if (trapped != null) {
            count++;
        }
        if (hurt != null) {
            count++;
        }
        return count;
    }

    /** 고른 재난의 서버 코드. 아직 안 골랐으면 null */
    public String getSituationCode() {
        if (situationIndex == null || situationIndex < 0 || situationIndex >= SITUATION_CODES.length) {
            return null;
        }
        return SITUATION_CODES[situationIndex];
    }

    public boolean isComplete() {
        return answeredCount() == 3;
    }
}
