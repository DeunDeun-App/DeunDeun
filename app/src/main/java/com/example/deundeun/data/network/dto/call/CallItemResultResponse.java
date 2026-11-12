package com.example.deundeun.data.network.dto.call;

public class CallItemResultResponse {

    private long itemId;
    private String label;
    private String statLabel;
    private boolean passed;

    public long getItemId() { return itemId; }
    public String getLabel() { return label; }
    public String getStatLabel() { return statLabel; }
    public boolean isPassed() { return passed; }
}
