package com.evandev.better_advancements.gui.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AdvancementDraft {
    public JsonObject rootJson;

    public AdvancementDraft(String initialJson) {
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