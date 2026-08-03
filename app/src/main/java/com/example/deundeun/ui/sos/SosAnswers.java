package com.example.deundeun.ui.sos;

import java.io.Serializable;

/**
 * SOS 질문 3가지의 답. 화면 사이로 넘겨 미리보기 문구를 만든다.
 *
 * <p>어느 하나라도 비어 있으면 "다음"으로 넘어갈 수 없다. 위급 상황에서
 * 보호자에게 절반만 채워진 메시지가 가면 오히려 판단을 어렵게 만든다.
 */
public class SosAnswers implements Serializable {

    public static final String EXTRA = "sos_answers";

    /**
     * 위치 선택지의 순번. 라벨 문자열이 아니라 순번을 담는다.
     * 화면에 보이는 라벨("지하철역 근처예요")과 메시지에 들어갈 문장("지하철역 근처에 있어요.")이
     * 서로 달라서, 순번으로 두 배열을 함께 참조해야 한다.
     */
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
