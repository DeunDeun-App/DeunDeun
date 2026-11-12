package com.example.deundeun.data.network.dto.call;

/** 119 신고 연습 답변. 회원은 로그인 토큰으로 확인하므로 담지 않는다. */
public class CallRequest {

    private final Long itemId;
    private final String answer;

    public CallRequest(Long itemId, String answer) {
        this.itemId = itemId;
        this.answer = answer;
    }
}
