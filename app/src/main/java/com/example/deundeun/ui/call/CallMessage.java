package com.example.deundeun.ui.call;

/** Figma 125:187 — 가상 통화의 대화 한 줄. */
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

    /** true 면 오른쪽 주황 말풍선, false 면 왼쪽 회색 말풍선. */
    public boolean isMine() {
        return mine;
    }
}
