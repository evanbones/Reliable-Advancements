package com.evandev.reliable_advancements.gui.tabs;

import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import com.evandev.reliable_advancements.gui.widgets.EditorForm;
import com.evandev.reliable_advancements.gui.widgets.ModernButton;
import com.evandev.reliable_advancements.gui.widgets.SuggestingEditBox;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class RewardsTab implements IEditorTab {
    private final Font font;
    private final EditorForm form;
    private final List<String> lootList = new ArrayList<>();
    private final List<String> recipeList = new ArrayList<>();
    private final List<SuggestingEditBox> lootBoxes = new ArrayList<>();
    private final List<SuggestingEditBox> recipeBoxes = new ArrayList<>();

    private EditBox expBox;
    private SuggestingEditBox functionBox;
    private String exp = "", function = "";

    public RewardsTab(Font font) {
        this.font = font;
        this.form = new EditorForm(font);
    }

    public static List<String> getLootTableSuggestions() {
        Set<String> results = new TreeSet<>();
        try {
            Minecraft mc = Minecraft.getInstance();

            if (mc.getSingleplayerServer() != null) {
                var lookup = mc.getSingleplayerServer().reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE);
                lookup.ifPresent(l -> l.listElementIds().map(k -> k.identifier().toString()).forEach(results::add));
            }

            if (mc.level != null) {
                var lookup = mc.level.registryAccess().lookup(Registries.LOOT_TABLE);
                lookup.ifPresent(l -> l.listElementIds().map(k -> k.identifier().toString()).forEach(results::add));
            }
            if (mc.getConnection() != null) {
                var reg = mc.getConnection().registryAccess().lookup(Registries.LOOT_TABLE);
                reg.ifPresent(r -> r.listElementIds().map(k -> k.identifier().toString()).forEach(results::add));
            }

            BuiltInLootTables.all().forEach(key -> results.add(key.identifier().toString()));

            for (Identifier blockId : BuiltInRegistries.BLOCK.keySet()) {
                results.add(Identifier.fromNamespaceAndPath(blockId.getNamespace(), "blocks/" + blockId.getPath()).toString());
            }

            for (Identifier entityId : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                results.add(Identifier.fromNamespaceAndPath(entityId.getNamespace(), "entities/" + entityId.getPath()).toString());
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(results);
    }

    public static List<String> getFunctionSuggestions() {
        Set<String> results = new TreeSet<>();
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSingleplayerServer() != null) {
                for (Identifier loc : mc.getSingleplayerServer().getFunctions().getFunctionNames()) {
                    results.add(loc.toString());
                }
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(results);
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
        form.clear();
        lootBoxes.clear();
        recipeBoxes.clear();

        form.addSection("Experience & Function");
        expBox = form.addTextField("Experience Points", "Amount of experience granted upon completion", exp, s -> exp = s);
        functionBox = form.addSuggestingField("Function", "Mcfunction to run upon completion, e.g. namespace:my_function", function, RewardsTab::getFunctionSuggestions, s -> function = s);

        form.addSection("Loot Tables");
        ModernButton addLootBtn = ModernButton.modernBuilder(Component.literal("+ Add Loot Table"), b -> {
            syncDynamicLists();
            lootList.add("");
            reinitScreen.run();
        }).style(ModernButton.Style.SECONDARY).pos(0, 0).size(130, 20).build();
        form.addCustomWidget("", addLootBtn, 24);

        for (int i = 0; i < lootList.size(); i++) {
            final int index = i;
            SuggestingEditBox box = new SuggestingEditBox(font, 0, 0, 100, 20, Component.literal("Loot Table"), RewardsTab::getLootTableSuggestions, SuggestingEditBox::lootTableIconResolver);
            box.setMaxLength(512);
            box.setValue(lootList.get(i));
            box.setTooltip(Tooltip.create(Component.literal("Loot table ID, e.g. minecraft:chests/simple_dungeon")));
            box.setResponder(s -> {
                if (index < lootList.size()) lootList.set(index, s);
            });
            lootBoxes.add(box);

            ModernButton removeBtn = ModernButton.modernBuilder(Component.literal("x"), b -> {
                        syncDynamicLists();
                        lootList.remove(index);
                        reinitScreen.run();
                    }).style(ModernButton.Style.DANGER).pos(0, 0).size(20, 20)
                    .tooltip(Tooltip.create(Component.literal("Remove Loot Table")))
                    .build();

            form.addCustomRow(new EditorForm.DynamicEntryRow(box, removeBtn), List.of(box, removeBtn));
        }

        form.addSection("Recipes");
        ModernButton addRecipeBtn = ModernButton.modernBuilder(Component.literal("+ Add Recipe"), b -> {
                    syncDynamicLists();
                    recipeList.add("");
                    reinitScreen.run();
                }).style(ModernButton.Style.SECONDARY).pos(0, 0).size(130, 20)
                .tooltip(Tooltip.create(Component.literal("Add recipe unlock reward")))
                .build();
        form.addCustomWidget("", addRecipeBtn, 24);

        for (int i = 0; i < recipeList.size(); i++) {
            final int index = i;
            SuggestingEditBox box = makeRecipeRewardBox(i, index);
            recipeBoxes.add(box);

            ModernButton removeBtn = ModernButton.modernBuilder(Component.literal("x"), b -> {
                        syncDynamicLists();
                        recipeList.remove(index);
                        reinitScreen.run();
                    }).style(ModernButton.Style.DANGER).pos(0, 0).size(20, 20)
                    .tooltip(Tooltip.create(Component.literal("Remove Recipe")))
                    .build();

            form.addCustomRow(new EditorForm.DynamicEntryRow(box, removeBtn), List.of(box, removeBtn));
        }

        form.init(x, y, width, height);
    }

    private @NotNull SuggestingEditBox makeRecipeRewardBox(int i, int index) {
        SuggestingEditBox box = new SuggestingEditBox(font, 0, 0, 100, 20, Component.literal("Recipe"), () -> {
            if (Minecraft.getInstance().level != null) {
                var lookup = Minecraft.getInstance().level.registryAccess().lookup(Registries.RECIPE);
                if (lookup.isPresent()) {
                    return lookup.get().listElementIds().map(k -> k.identifier().toString()).sorted().collect(Collectors.toList());
                }
            }
            return List.of();
        }, SuggestingEditBox::defaultItemIconResolver);
        box.setMaxLength(512);
        box.setValue(recipeList.get(i));
        box.setTooltip(Tooltip.create(Component.literal("Recipe ID to unlock")));
        box.setResponder(s -> {
            if (index < recipeList.size()) recipeList.set(index, s);
        });
        return box;
    }

    private void syncDynamicLists() {
        for (int i = 0; i < lootBoxes.size(); i++) {
            if (i < lootList.size()) lootList.set(i, lootBoxes.get(i).getValue());
        }
        for (int i = 0; i < recipeBoxes.size(); i++) {
            if (i < recipeList.size()) recipeList.set(i, recipeBoxes.get(i).getValue());
        }
    }

    @Override
    public void syncFromWidgets() {
        if (expBox != null) this.exp = expBox.getValue();
        if (functionBox != null) this.function = functionBox.getValue();
        syncDynamicLists();
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        syncFromWidgets();

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
        form.render(gfx, mouseX, mouseY, partialTicks);
    }

    @Override
    public List<GuiEventListener> getWidgets() {
        return form.getWidgets();
    }

    @Override
    public EditorForm getForm() {
        return form;
    }
}