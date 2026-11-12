package com.example.deundeun.data.network.dto.call;

import java.util.Collections;
import java.util.List;

public class CallSessionResponse {

    private long sessionId;
    private String scenarioTitle;
    private String situationText;
    private List<CallItemResponse> items;

    public long getSessionId() { return sessionId; }
    public String getScenarioTitle() { return scenarioTitle; }
    public String getSituationText() { return situationText; }

    public List<CallItemResponse> getItems() {
        return items == null ? Collections.emptyList() : items;
    }
}
