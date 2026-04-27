package com.evandev.better_advancements.gui;

import com.evandev.better_advancements.gui.screens.BetterAdvancementsScreen;
import com.evandev.better_advancements.network.EditAdvancementPayload;
import com.evandev.better_advancements.network.RequestAdvancementJsonPayload;
import com.evandev.better_advancements.platform.Services;
import com.evandev.better_advancements.util.PersistentData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class AdvancementContextMenu {
    private final BetterAdvancementsScreen parentScreen;
    private final BetterAdvancementWidget widget;
    private final int x, y;
    private final int width = 160;
    private final int height;
    private final List<ContextOption> options = new ArrayList<>();

    public AdvancementContextMenu(BetterAdvancementsScreen parentScreen, BetterAdvancementWidget widget, int mouseX, int mouseY) {
        this.parentScreen = parentScreen;
        this.widget = widget;

        if (widget != null) {
            this.options.add(new ContextOption("Edit Properties", false, () -> openEditor("Properties")));
            this.options.add(new ContextOption("Edit Layout (Pos)", false, () -> openEditor("Layout")));
            this.options.add(new ContextOption("Edit Criteria", false, () -> openEditor("Criteria")));
            this.options.add(new ContextOption("Link to Parent...", false, () -> parentScreen.startLinking(widget)));
            this.options.add(new ContextOption("Reset to Vanilla", true, this::deleteAdvancement));
        } else {
            this.options.add(new ContextOption("Create New Advancement", false, () -> parentScreen.createNewAdvancement(mouseX, mouseY)));
            this.options.add(new ContextOption("Edit Tab Properties", false, parentScreen::editTabProperties));
            this.options.add(new ContextOption("Reset Entire Tab", true, this::resetEntireTab));
        }

        this.height = this.options.size() * 20 + 4;

        this.x = Math.min(mouseX, parentScreen.width - this.width - 5);
        this.y = Math.min(mouseY, parentScreen.height - this.height - 5);
    }

    private void openEditor(String tabName) {
        ResourceLocation id = widget.getAdvancement().holder().id();
        RequestAdvancementJsonPayload request = new RequestAdvancementJsonPayload(id, tabName);

        Services.PLATFORM.sendAdvancementJsonRequest(request);

        parentScreen.closeContextMenu();
    }

    private void deleteAdvancement() {
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        EditAdvancementPayload payload = new EditAdvancementPayload(widget.getAdvancement().holder().id(), "{}", true);
                        if (Services.PLATFORM.canSendAdvancementEdit()) {
                            Services.PLATFORM.sendAdvancementEdit(payload);
                        }
                        PersistentData.removePosition(widget.getAdvancement().holder().id());
                        if (parentScreen.selectedTab != null && widget.getAdvancement().holder().id().equals(parentScreen.selectedTab.getRootNode().holder().id())) {
                            PersistentData.removeTabProperties(parentScreen.selectedTab.getRootNode().holder().id());
                        }
                        parentScreen.removeWidgetFromClient(widget);
                    }
                    Minecraft.getInstance().setScreen(parentScreen);
                },
                Component.literal("Reset Advancement?"),
                Component.literal("Are you sure you want to reset this advancement to vanilla? This cannot be undone.")
        ));
        parentScreen.closeContextMenu();
    }

    private void resetEntireTab() {
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        if (parentScreen.selectedTab != null) {
                            for (BetterAdvancementWidget w : new java.util.ArrayList<>(parentScreen.selectedTab.getWidgets().values())) {
                                EditAdvancementPayload payload = new EditAdvancementPayload(w.getAdvancement().holder().id(), "{}", true);
                                if (Services.PLATFORM.canSendAdvancementEdit()) {
                                    Services.PLATFORM.sendAdvancementEdit(payload);
                                }
                                PersistentData.removePosition(w.getAdvancement().holder().id());
                            }
                            PersistentData.removeTabProperties(parentScreen.selectedTab.getRootNode().holder().id());
                            parentScreen.selectedTab.getWidgets().clear();
                        }
                    }
                    Minecraft.getInstance().setScreen(parentScreen);
                },
                Component.literal("Reset Entire Tab?"),
                Component.literal("Are you sure you want to reset ALL advancements in this tab? This cannot be undone.")
        ));
        parentScreen.closeContextMenu();
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