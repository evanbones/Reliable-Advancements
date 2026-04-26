package com.evandev.better_advancements.gui;

import com.evandev.better_advancements.network.EditAdvancementPayload;
import com.evandev.better_advancements.platform.Services;
import com.evandev.better_advancements.reference.Constants;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AdvancementEditorScreen extends Screen {

    private final BetterAdvancementsScreen parentScreen;
    private final BetterAdvancementWidget widget;
    private final EditorTab initialTab;
    private EditBox titleBox;
    private EditBox descriptionBox;
    private EditBox iconBox;
    private EditBox parentBox;

    public AdvancementEditorScreen(BetterAdvancementsScreen parentScreen, BetterAdvancementWidget widget, EditorTab initialTab) {
        super(Component.literal("Edit Advancement: " + widget.getAdvancement().holder().id().toString()));
        this.parentScreen = parentScreen;
        this.widget = widget;
        this.initialTab = initialTab;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 50;

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose())
                .pos(centerX - 105, this.height - 40).size(100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parentScreen))
                .pos(centerX + 5, this.height - 40).size(100, 20).build());

        AdvancementNode node = widget.getAdvancement();
        DisplayInfo display = node.advancement().display().orElse(null);

        titleBox = new EditBox(this.font, centerX - 100, startY, 200, 20, Component.literal("Title"));
        titleBox.setMaxLength(256);
        titleBox.setValue(display != null ? display.getTitle().getString() : "");
        this.addRenderableWidget(titleBox);

        descriptionBox = new EditBox(this.font, centerX - 100, startY + 40, 200, 20, Component.literal("Description"));
        descriptionBox.setMaxLength(512);
        descriptionBox.setValue(display != null ? display.getDescription().getString() : "");
        this.addRenderableWidget(descriptionBox);

        iconBox = new EditBox(this.font, centerX - 100, startY + 80, 200, 20, Component.literal("Icon Item ID"));
        iconBox.setMaxLength(256);
        iconBox.setValue(display != null ? BuiltInRegistries.ITEM.getKey(display.getIcon().getItem()).toString() : "minecraft:stone");
        this.addRenderableWidget(iconBox);

        parentBox = new EditBox(this.font, centerX - 100, startY + 120, 200, 20, Component.literal("Parent ID"));
        parentBox.setMaxLength(256);
        parentBox.setValue(node.parent() != null ? node.parent().holder().id().toString() : "");
        this.addRenderableWidget(parentBox);
    }

    private void saveAndClose() {
        ResourceLocation advancementId = widget.getAdvancement().holder().id();
        EditAdvancementPayload payload = new EditAdvancementPayload(
                advancementId,
                titleBox.getValue(),
                descriptionBox.getValue(),
                iconBox.getValue(),
                parentBox.getValue()
        );

        if (Services.PLATFORM.canSendAdvancementEdit()) {
            Services.PLATFORM.sendAdvancementEdit(payload);
            Constants.LOG.info("Sent edit payload for advancement: {}", advancementId);
        } else {
            Constants.LOG.warn("Server does not support advancement editing. Changes are visual only.");
            this.minecraft.player.displayClientMessage(
                    Component.translatable("message.betteradvancements.no_server_support").withStyle(net.minecraft.ChatFormatting.RED), false
            );
        }

        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        int centerX = this.width / 2;
        int startY = 50;

        guiGraphics.drawString(this.font, "Title", centerX - 100, startY - 10, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Description", centerX - 100, startY + 30, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Icon (Item ID)", centerX - 100, startY + 70, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Parent ID", centerX - 100, startY + 110, 0xA0A0A0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.setScreen(parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public enum EditorTab {PROPERTIES, LAYOUT, CRITERIA}
}