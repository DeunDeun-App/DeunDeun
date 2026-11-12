package com.example.deundeun.data.network.dto.call;

/** 답변 판정 결과. */
public class CallAnswerResponse {

    private boolean passed;
    private int attempts;
    private boolean retryable;
    private String feedback;
    private String followUp;

    public boolean isPassed() { return passed; }
    public int getAttempts() { return attempts; }

    /** 같은 질문에 한 번 더 답할 수 있는지. */
    public boolean isRetryable() { return retryable; }

    /** 실패했을 때 도움말에 보여줄 힌트. 통과면 {@code null}. */
    public String getFeedback() { return feedback; }

    /** 상황 단서를 일부만 말했을 때 상담원이 되묻는 말. 없으면 {@code null}. */
    public String getFollowUp() { return followUp; }
}
