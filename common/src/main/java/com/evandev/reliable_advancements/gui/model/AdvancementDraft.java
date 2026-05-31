package com.evandev.reliable_advancements.gui.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AdvancementDraft {
    public JsonObject rootJson;
    public String id;
    public boolean isNew;

    public AdvancementDraft(String initialJson, String id, boolean isNew) {
        this.id = id;
        this.isNew = isNew;
        try {
            this.rootJson = JsonParser.parseString(initialJson).getAsJsonObject();
        } catch (Exception e) {
            this.rootJson = new JsonObject();
        }
    }

    public String serialize() {
        return rootJson.toString();
    }
}