package com.example.deundeun.data.network.dto.simulation;

import com.example.deundeun.data.network.dto.record.KeywordResponse;

import java.util.Collections;
import java.util.List;

public class SimulationResultResponse {

    private long sessionId;
    private int score;
    private List<KeywordResponse> keywords;

    public long getSessionId() { return sessionId; }
    public int getScore() { return score; }

    public List<KeywordResponse> getKeywords() {
        return keywords == null ? Collections.emptyList() : keywords;
    }
}
