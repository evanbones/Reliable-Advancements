package com.evandev.better_advancements.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class AdvancementContextMenu {
    private final BetterAdvancementsScreen parentScreen;
    private final BetterAdvancementWidget widget;
    private final int x, y;
    private final int width = 145;
    private final int height;
    private final List<ContextOption> options = new ArrayList<>();

    public AdvancementContextMenu(BetterAdvancementsScreen parentScreen, BetterAdvancementWidget widget, int mouseX, int mouseY) {
        this.parentScreen = parentScreen;
        this.widget = widget;

        if (widget != null) {
            this.options.add(new ContextOption("Edit Properties", false, () -> openEditor(AdvancementEditorScreen.EditorTab.PROPERTIES)));
            this.options.add(new ContextOption("Edit Layout (Pos)", false, () -> openEditor(AdvancementEditorScreen.EditorTab.LAYOUT)));
            this.options.add(new ContextOption("Edit Criteria", false, () -> openEditor(AdvancementEditorScreen.EditorTab.CRITERIA)));
            this.options.add(new ContextOption("Link to Parent...", false, () -> parentScreen.startLinking(widget)));
            this.options.add(new ContextOption("Delete Advancement", true, this::deleteAdvancement));
        } else {
            this.options.add(new ContextOption("Create New Advancement", false, () -> parentScreen.createNewAdvancement(mouseX, mouseY)));
        }

        this.height = this.options.size() * 20 + 4;

        this.x = Math.min(mouseX, parentScreen.width - this.width - 5);
        this.y = Math.min(mouseY, parentScreen.height - this.height - 5);
    }

    private void openEditor(AdvancementEditorScreen.EditorTab tab) {
        Minecraft.getInstance().setScreen(new AdvancementEditorScreen(this.parentScreen, this.widget, tab));
    }

    private void deleteAdvancement() {
        Minecraft.getInstance().player.displayClientMessage(
                Component.literal("Delete advancement payload coming in next phase: " + widget.getAdvancement().holder().id()), false
        );
        Minecraft.getInstance().setScreen(parentScreen);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1000);

        guiGraphics.fillGradient(x, y, x + width, y + height, 0xF0101010, 0xF0101010);
        guiGraphics.renderOutline(x, y, width, height, 0xFF505050);

        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < options.size(); i++) {
            ContextOption option = options.get(i);
            int optY = y + 2 + i * 20;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= optY && mouseY < optY + 20;

            if (hovered) {
                guiGraphics.fill(x + 1, optY, x + width - 1, optY + 20, option.isDestructive ? 0x80AA3333 : 0x80505050);
            }

            int textColor = hovered ? (option.isDestructive ? 0xFF5555 : 0xFFFFAA) : 0xFFFFFF;
            guiGraphics.drawString(font, option.label, x + 6, optY + 6, textColor);
        }

        guiGraphics.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            if (button == 0) {
                int index = (int) ((mouseY - y - 2) / 20);
                if (index >= 0 && index < options.size()) {
                    options.get(index).action.run();
                }
            }
            return true;
        }
        return false;
    }

    private record ContextOption(String label, boolean isDestructive, Runnable action) {
    }
}