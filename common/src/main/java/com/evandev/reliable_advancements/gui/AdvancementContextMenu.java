package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.network.RequestAdvancementJsonPayload;
import com.evandev.reliable_advancements.network.ResetTabPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.util.PersistentData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class AdvancementContextMenu {
    private final EnhancedAdvancementsScreen parentScreen;
    private final EnhancedAdvancementWidget widget;
    private final int x, y;
    private final int width = 160;
    private final int height;
    private final List<ContextOption> options = new ArrayList<>();

    public AdvancementContextMenu(EnhancedAdvancementsScreen parentScreen, EnhancedAdvancementWidget widget, int mouseX, int mouseY) {
        this.parentScreen = parentScreen;
        this.widget = widget;

        if (widget != null) {
            this.options.add(new ContextOption("Edit Properties", false, () -> openEditor("Properties")));
            this.options.add(new ContextOption("Copy (Ctrl+C)", false, () -> {
                parentScreen.copyAdvancement(widget);
                parentScreen.closeContextMenu();
            }));
            this.options.add(new ContextOption("Link to Parent...", false, () -> parentScreen.startLinking(widget)));
            this.options.add(new ContextOption("Reset to Vanilla", true, () -> parentScreen.resetAdvancement(widget)));
            this.options.add(new ContextOption("Delete from Game", true, () -> parentScreen.deleteAdvancement(widget)));
        } else {
            this.options.add(new ContextOption("Create New Advancement", false, () -> parentScreen.createNewAdvancement(mouseX, mouseY)));
            this.options.add(new ContextOption("Create New Tab", false, () -> parentScreen.createNewTab()));
            this.options.add(new ContextOption("Paste (Ctrl+V)", false, () -> {
                parentScreen.pasteAdvancement(mouseX, mouseY);
                parentScreen.closeContextMenu();
            }));
            this.options.add(new ContextOption("Edit Tab Properties", false, parentScreen::editTabProperties));
            this.options.add(new ContextOption("Reset Entire Tab", true, this::resetEntireTab));
        }

        this.height = this.options.size() * 20 + 4;

        this.x = Math.min(mouseX, parentScreen.width - this.width - 5);
        this.y = Math.min(mouseY, parentScreen.height - this.height - 5);
    }

    private void openEditor(String tabName) {
        Identifier id = widget.getAdvancement().holder().id();
        RequestAdvancementJsonPayload request = new RequestAdvancementJsonPayload(id, tabName);

        Services.PLATFORM.sendAdvancementJsonRequest(request);

        parentScreen.closeContextMenu();
    }

    private void resetEntireTab() {
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        if (parentScreen.selectedTab != null) {
                            List<Identifier> idsToDelete = new ArrayList<>();

                            for (EnhancedAdvancementWidget w : parentScreen.selectedTab.getWidgets().values()) {
                                idsToDelete.add(w.getAdvancement().holder().id());
                                PersistentData.removePosition(w.getAdvancement().holder().id());
                            }
                            PersistentData.removeTabProperties(parentScreen.selectedTab.getRootNode().holder().id());

                            Services.PLATFORM.sendResetTab(new ResetTabPayload(idsToDelete));
                        }
                    }
                    Minecraft.getInstance().setScreen(parentScreen);
                },
                Component.literal("Reset Entire Tab?"),
                Component.literal("Are you sure you want to reset ALL advancements in this tab? This cannot be undone.")
        ));
        parentScreen.closeContextMenu();
    }

    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.nextStratum();

        guiGraphics.fillGradient(x, y, x + width, y + height, 0xF0101010, 0xF0101010);
        guiGraphics.outline(x, y, width, height, 0xFF505050);

        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < options.size(); i++) {
            ContextOption option = options.get(i);
            int optY = y + 2 + i * 20;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= optY && mouseY < optY + 20;

            if (hovered) {
                guiGraphics.fill(x + 1, optY, x + width - 1, optY + 20, option.isDestructive ? 0x80AA3333 : 0x80505050);
            }

            int textColor = hovered ? (option.isDestructive ? 0xFF5555 : 0xFFFFAA) : 0xFFFFFF;
            guiGraphics.text(font, option.label, x + 6, optY + 6, textColor);
        }

        guiGraphics.pose().popMatrix();
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

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