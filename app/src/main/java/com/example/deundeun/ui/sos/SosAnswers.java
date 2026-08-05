package com.example.deundeun.ui.sos;

import java.io.Serializable;

/**
 * SOS 질문 3가지의 답. 화면 사이로 넘겨 미리보기 문구를 만든다
 *
 * 어느 하나라도 비어 있으면 "다음"으로 넘어갈 수 없다
 */
public class SosAnswers implements Serializable {

    public static final String EXTRA = "sos_answers";
    private Integer whereIndex;
    private Boolean scared;
    private Boolean hurt;

    public Integer getWhereIndex() {
        return whereIndex;
    }

    public void setWhereIndex(Integer whereIndex) {
        this.whereIndex = whereIndex;
    }

    public Boolean getScared() {
        return scared;
    }

    public void setScared(Boolean scared) {
        this.scared = scared;
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
        if (whereIndex != null) {
            count++;
        }
        if (scared != null) {
            count++;
        }
        if (hurt != null) {
            count++;
        }
        return count;
    }

    public boolean isComplete() {
        return answeredCount() == 3;
    }
}
