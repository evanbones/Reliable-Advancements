package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.network.RequestAdvancementJsonPayload;
import com.evandev.reliable_advancements.network.ResetTabPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.util.PersistentData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class AdvancementContextMenu {
    private final EnhancedAdvancementsScreen parentScreen;
    private final EnhancedAdvancementWidget widget;
    private final int x, y;
    private final int width;
    private final int height;
    private final List<ContextOption> options = new ArrayList<>();

    public AdvancementContextMenu(EnhancedAdvancementsScreen parentScreen, EnhancedAdvancementWidget widget, int mouseX, int mouseY) {
        this.parentScreen = parentScreen;
        this.widget = widget;

        if (widget != null) {
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.edit_properties"), false, () -> openEditor("Properties")));
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.copy"), false, () -> {
                parentScreen.copyAdvancement(widget);
                parentScreen.closeContextMenu();
            }));
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.link_parents"), false, () -> parentScreen.startLinking(widget)));
            if (!widget.getParents().isEmpty()) {
                this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.unlink_all_parents"), false, () -> {
                    parentScreen.unlinkAllParents(widget);
                    parentScreen.closeContextMenu();
                }));
            }
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.reset_vanilla"), true, () -> parentScreen.resetAdvancement(widget)));
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.delete"), true, () -> parentScreen.deleteAdvancement(widget)));
        } else {
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.create_advancement"), false, () -> parentScreen.createNewAdvancement(mouseX, mouseY)));
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.create_tab"), false, () -> parentScreen.createNewTab(mouseX, mouseY)));
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.paste"), false, () -> {
                parentScreen.pasteAdvancement(mouseX, mouseY);
                parentScreen.closeContextMenu();
            }));
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.edit_tab_properties"), false, parentScreen::editTabProperties));
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.reset_tab"), true, this::resetEntireTab));
        }

        Font font = Minecraft.getInstance().font;
        int maxW = 160;
        for (ContextOption opt : this.options) {
            maxW = Math.max(maxW, font.width(opt.label()) + 16);
        }
        this.width = maxW;
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

    private void resetEntireTab() {
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        if (parentScreen.selectedTab != null) {
                            ResourceLocation rootId = parentScreen.selectedTab.getRootNode().holder().id();
                            List<ResourceLocation> idsToDelete = new ArrayList<>();

                            for (EnhancedAdvancementWidget w : parentScreen.selectedTab.getWidgets().values()) {
                                idsToDelete.add(w.getAdvancement().holder().id());
                            }
                            PersistentData.removePositions(idsToDelete);
                            PersistentData.removeTabProperties(rootId);
                            EnhancedAdvancementsScreen.setSavedSelectedTab(rootId);
                            parentScreen.setLoading(true);
                            Services.PLATFORM.sendResetTab(new ResetTabPayload(rootId, idsToDelete));
                        }
                    }
                    Minecraft.getInstance().setScreen(parentScreen);
                },
                Component.translatable("gui.reliable_advancements.dialog.reset_tab.title"),
                Component.translatable("gui.reliable_advancements.dialog.reset_tab.message")
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
            guiGraphics.drawString(font, option.label(), x + 6, optY + 6, textColor);
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

    private record ContextOption(Component label, boolean isDestructive, Runnable action) {
    }
}
