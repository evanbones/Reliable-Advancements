package com.evandev.reliable_advancements.gui.tabs;

import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import com.evandev.reliable_advancements.gui.widgets.SuggestingEditBox;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RewardsTab implements IEditorTab {
    private final Font font;
    private final List<GuiEventListener> widgets = new ArrayList<>();
    private final List<String> lootList = new ArrayList<>();
    private final List<String> recipeList = new ArrayList<>();
    private EditBox expBox;
    private SuggestingEditBox functionBox;
    private String exp = "", function = "";
    private int startX, expY, funcY, lootLabelY, recipeLabelY;

    public RewardsTab(Font font) {
        this.font = font;
    }

    @Override
    public void loadState(AdvancementDraft draft) {
        lootList.clear();
        recipeList.clear();

        if (draft.rootJson.has("rewards")) {
            JsonObject rewards = draft.rootJson.getAsJsonObject("rewards");
            if (rewards.has("experience")) exp = rewards.get("experience").getAsString();
            if (rewards.has("function")) function = rewards.get("function").getAsString();

            if (rewards.has("loot")) {
                for (JsonElement e : rewards.getAsJsonArray("loot")) {
                    lootList.add(e.getAsString());
                }
            }
            if (rewards.has("recipes")) {
                for (JsonElement e : rewards.getAsJsonArray("recipes")) {
                    recipeList.add(e.getAsString());
                }
            }
        }
    }

    @Override
    public void init(int x, int y, int width, int height, Runnable reinitScreen) {
        this.widgets.clear();
        this.startX = x;
        int currentY = y;

        this.expY = currentY - 11;
        expBox = new EditBox(font, x, currentY, width, 20, Component.literal("Experience"));
        expBox.setValue(exp);
        expBox.setResponder(s -> exp = s);
        widgets.add(expBox);

        currentY += 45;

        this.funcY = currentY - 11;
        functionBox = new SuggestingEditBox(font, x, currentY, width, 20, Component.literal("Function"), List::of);
        functionBox.setValue(function);
        functionBox.setResponder(s -> function = s);
        widgets.add(functionBox);

        currentY += 45;

        this.lootLabelY = currentY - 11;
        widgets.add(Button.builder(Component.literal("+ Add Loot Table"), b -> {
            lootList.add("");
            reinitScreen.run();
        }).pos(x, currentY).size(120, 20).build());

        currentY += 25;

        for (int i = 0; i < lootList.size(); i++) {
            final int index = i;
            SuggestingEditBox box = new SuggestingEditBox(font, x, currentY, width - 25, 20, Component.literal("Loot"), List::of);
            box.setValue(lootList.get(i));
            box.setResponder(s -> lootList.set(index, s));

            Button removeBtn = Button.builder(Component.literal("X"), b -> {
                lootList.remove(index);
                reinitScreen.run();
            }).pos(x + width - 20, currentY).size(20, 20).build();

            widgets.add(box);
            widgets.add(removeBtn);
            currentY += 25;
        }

        currentY += 20;

        this.recipeLabelY = currentY - 11;
        widgets.add(Button.builder(Component.literal("+ Add Recipe"), b -> {
            recipeList.add("");
            reinitScreen.run();
        }).pos(x, currentY).size(120, 20).build());

        currentY += 25;

        for (int i = 0; i < recipeList.size(); i++) {
            final int index = i;
            SuggestingEditBox box = new SuggestingEditBox(font, x, currentY, width - 25, 20, Component.literal("Recipe"), List::of);
            box.setValue(recipeList.get(i));
            box.setResponder(s -> recipeList.set(index, s));

            Button removeBtn = Button.builder(Component.literal("X"), b -> {
                recipeList.remove(index);
                reinitScreen.run();
            }).pos(x + width - 20, currentY).size(20, 20).build();

            widgets.add(box);
            widgets.add(removeBtn);
            currentY += 25;
        }
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        JsonObject rewards = new JsonObject();
        boolean hasRewards = false;

        if (!exp.trim().isEmpty()) {
            try {
                rewards.addProperty("experience", Integer.parseInt(exp.trim()));
                hasRewards = true;
            } catch (NumberFormatException ignored) {
            }
        }

        if (!function.trim().isEmpty()) {
            rewards.addProperty("function", function.trim());
            hasRewards = true;
        }

        JsonArray lootArr = new JsonArray();
        for (String s : lootList) {
            if (!s.trim().isEmpty()) lootArr.add(s.trim());
        }
        if (!lootArr.isEmpty()) {
            rewards.add("loot", lootArr);
            hasRewards = true;
        }

        JsonArray recipeArr = new JsonArray();
        for (String s : recipeList) {
            if (!s.trim().isEmpty()) recipeArr.add(s.trim());
        }
        if (!recipeArr.isEmpty()) {
            rewards.add("recipes", recipeArr);
            hasRewards = true;
        }

        if (hasRewards) {
            draft.rootJson.add("rewards", rewards);
        } else {
            draft.rootJson.remove("rewards");
        }
    }

    @Override
    public void render(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.text(font, "Experience (Number)", startX, expY, 0xFFA08060, false);
        gfx.text(font, "Function", startX, funcY, 0xFFA08060, false);
        gfx.text(font, "Loot Tables", startX, lootLabelY, 0xFFA08060, false);
        gfx.text(font, "Recipes", startX, recipeLabelY, 0xFFA08060, false);
    }

    @Override
    public List<GuiEventListener> getWidgets() {
        return widgets;
    }
}