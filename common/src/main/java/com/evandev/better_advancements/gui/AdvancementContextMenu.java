package com.evandev.better_advancements.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class AdvancementContextMenu {
    private final BetterAdvancementsScreen parentScreen;
    private final BetterAdvancementWidget widget;
    private final int x, y;
    private final int width = 140;
    private final int height;
    private final List<ContextOption> options = new ArrayList<>();

    public AdvancementContextMenu(BetterAdvancementsScreen parentScreen, BetterAdvancementWidget widget, int mouseX, int mouseY) {
        this.parentScreen = parentScreen;
        this.widget = widget;

        this.options.add(new ContextOption("Edit Properties", () -> openEditor(AdvancementEditorScreen.EditorTab.PROPERTIES)));
        this.options.add(new ContextOption("Edit Layout (Pos)", () -> openEditor(AdvancementEditorScreen.EditorTab.LAYOUT)));
        this.options.add(new ContextOption("Edit Criteria", () -> openEditor(AdvancementEditorScreen.EditorTab.CRITERIA)));

        this.height = this.options.size() * 20 + 4;

        this.x = Math.min(mouseX, parentScreen.width - this.width - 5);
        this.y = Math.min(mouseY, parentScreen.height - this.height - 5);
    }

    private void openEditor(AdvancementEditorScreen.EditorTab tab) {
        Minecraft.getInstance().setScreen(new AdvancementEditorScreen(this.parentScreen, this.widget, tab));
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1000); // Render heavily on top

        guiGraphics.fillGradient(x, y, x + width, y + height, 0xF0101010, 0xF0101010);
        guiGraphics.renderOutline(x, y, width, height, 0xFF505050);

        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < options.size(); i++) {
            ContextOption option = options.get(i);
            int optY = y + 2 + i * 20;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= optY && mouseY < optY + 20;

            if (hovered) {
                guiGraphics.fill(x + 1, optY, x + width - 1, optY + 20, 0x80505050); // Hover overlay
            }

            guiGraphics.drawString(font, option.label, x + 6, optY + 6, hovered ? 0xFFFFAA : 0xFFFFFF);
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

    private record ContextOption(String label, Runnable action) {
    }
}