package com.example.deundeun.data.network.dto.record;

import androidx.annotation.Nullable;

/** 훈련 1회의 키워드 하나. 완료 화면과 기록 카드에 ✓/✗ 로 보여준다. */
public class KeywordResponse {

    private String label;
    private boolean passed;
    private String feedback;

    public String getLabel() { return label; }
    public boolean isPassed() { return passed; }

    /** 못 했을 때 보여줄 한 줄. 해냈으면 null */
    @Nullable
    public String getFeedback() { return feedback; }
}
