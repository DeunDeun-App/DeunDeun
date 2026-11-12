package com.example.deundeun.data.network.dto.disaster;

import java.util.Collections;
import java.util.List;

public class EasyMessageResponse {

    private String id;
    private String disasterCode;
    private String disasterName;
    private List<Item> items;
    private List<String> actions;
    private String ttsText;
    private boolean aiGenerated;

    public String getId() { return id; }
    public String getDisasterCode() { return disasterCode; }
    public String getDisasterName() { return disasterName; }
    public List<Item> getItems() { return items == null ? Collections.emptyList() : items; }
    public List<String> getActions() { return actions == null ? Collections.emptyList() : actions; }
    public String getTtsText() { return ttsText; }
    public boolean isAiGenerated() { return aiGenerated; }

    public static class Item {

        private String pictogram;
        private String title;
        private String description;

        public String getPictogram() { return pictogram; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
    }
}
