package com.example.deundeun.data.network.dto.call;

public class CallItemResponse {

    private long itemId;
    private int itemOrder;
    private String itemType;
    private String label;
    private String question;

    public long getItemId() { return itemId; }
    public int getItemOrder() { return itemOrder; }
    public String getItemType() { return itemType; }
    public String getLabel() { return label; }
    public String getQuestion() { return question; }
}
