package com.example.deundeun.data.network.dto.call;

import java.util.Collections;
import java.util.List;

public class CallResultResponse {

    private int score;
    private List<CallItemResultResponse> results;

    public int getScore() { return score; }

    public List<CallItemResultResponse> getResults() {
        return results == null ? Collections.emptyList() : results;
    }
}
