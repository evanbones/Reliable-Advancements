package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.reference.Resources;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class EnhancedAdvancementTabType {
    public static final EnhancedAdvancementTabType ABOVE = new EnhancedAdvancementTabType(0, 0, 28, 32, AdvancementTabType.ABOVE);
    public static final EnhancedAdvancementTabType BELOW = new EnhancedAdvancementTabType(84, 0, 28, 32, AdvancementTabType.BELOW);
    public static final EnhancedAdvancementTabType LEFT = new EnhancedAdvancementTabType(0, 64, 32, 28, AdvancementTabType.LEFT);
    public static final EnhancedAdvancementTabType RIGHT = new EnhancedAdvancementTabType(96, 64, 32, 28, AdvancementTabType.RIGHT);
    // Below is not included in all as we will not be using it to leave space for pagination
    public static final List<EnhancedAdvancementTabType> ALL = List.of(ABOVE, RIGHT, LEFT);

    private final int textureX;
    private final int textureY;
    private final int width;
    private final int height;
    private final AdvancementTabType tabType;

    private EnhancedAdvancementTabType(int textureX, int textureY, int width, int height, AdvancementTabType tabType) {
        this.textureX = textureX;
        this.textureY = textureY;
        this.width = width;
        this.height = height;
        this.tabType = tabType;
    }

    public static EnhancedAdvancementTabType getTabType(int width, int height, int index) {
        int indexOnPage = index % getMaxTabs(width, height);

        int tabsAbove = ABOVE.getMax(width, height);
        int tabsRight = RIGHT.getMax(width, height);
        int tabsLeft = LEFT.getMax(width, height);

        if (indexOnPage < tabsAbove) {
            return ABOVE;
        } else if (indexOnPage < tabsAbove + tabsRight) {
            return RIGHT;
        } else if (indexOnPage < tabsAbove + tabsRight + tabsLeft) {
            return LEFT;
        }

        return null;
    }

    public static int getMaxTabs(int width, int height) {
        if (ModConfig.get().onlyUseAboveAdvancementTabs) {
            return ABOVE.getMax(width, height);
        }

        return ALL.stream().mapToInt(tab -> tab.getMax(width, height)).sum();
    }

    public void draw(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean selected, int index) {
        int i = this.textureX;
        index %= getMax(width, height);

        if (index > 0) {
            i += this.width;
        }

        if (x + this.width == width) {
            i += this.width;
        }

        int j = selected ? this.textureY + this.height : this.textureY;
        guiGraphics.blit(Resources.Gui.TABS, x + this.getX(index, width, height), y + this.getY(index, width, height), i, j, this.width, this.height);
    }

    public void drawIcon(GuiGraphics guiGraphics, int left, int top, int width, int height, int index, ItemStack stack) {
        int i = left + this.getX(index, width, height);
        int j = top + this.getY(index, width, height);

        switch (tabType) {
            case ABOVE -> {
                i += 6;
                j += 9;
            }
            case BELOW -> {
                i += 6;
                j += 6;
            }
            case LEFT -> {
                i += 10;
                j += 5;
            }
            case RIGHT -> {
                i += 6;
                j += 5;
            }
        }

        guiGraphics.renderFakeItem(stack, i, j);
    }

    public int getX(int index, int width, int height) {
        index %= getMax(width, height);
        return switch (tabType) {
            case ABOVE, BELOW -> (this.width + 4) * index;
            case LEFT -> -this.width + 4;
            case RIGHT -> width - 4;
        };
    }

    public int getY(int index, int width, int height) {
        index %= getMax(width, height);
        return switch (tabType) {
            case ABOVE -> -this.height + 4;
            case BELOW -> height - 4;
            case LEFT, RIGHT -> this.height * index;
        };
    }

    public boolean isMouseOver(int left, int top, int width, int height, int index, double mouseX, double mouseY) {
        int i = left + this.getX(index, width, height);
        int j = top + this.getY(index, width, height);
        return mouseX > i && mouseX < i + this.width && mouseY > j && mouseY < j + this.height;
    }

    private int getMax(int width, int height) {
        return switch (tabType) {
            case LEFT, RIGHT -> height / 32;
            case ABOVE, BELOW -> width / 32;
            default -> tabType.getMax();
        };
    }
}
