package com.example.deundeun.ui.call;

/** 가상 통화의 대화 한 줄. */
public class CallMessage {

    private final String text;
    private final boolean mine;

    private CallMessage(String text, boolean mine) {
        this.text = text;
        this.mine = mine;
    }

    public static CallMessage fromOperator(String text) {
        return new CallMessage(text, false);
    }

    public static CallMessage fromMe(String text) {
        return new CallMessage(text, true);
    }

    public String getText() {
        return text;
    }

    public boolean isMine() {
        return mine;
    }
}
