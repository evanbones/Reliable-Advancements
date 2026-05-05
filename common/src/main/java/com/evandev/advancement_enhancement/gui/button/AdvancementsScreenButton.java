package com.evandev.advancement_enhancement.gui.button;

import com.evandev.advancement_enhancement.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.advancement_enhancement.reference.Resources;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class AdvancementsScreenButton extends AbstractButton {
    public static boolean addToInventory = true;
    public static InventoryButtonStyle style = InventoryButtonStyle.BUTTON;
    public static int offsetX = 0;
    public static int offsetY = 0;

    public static String customTexture = "advancement_enhancement:textures/gui/inventory_button.png";
    public static String customTextureHovered = "advancement_enhancement:textures/gui/inventory_button_highlighted.png";
    public static String customIcon = "";

    public AdvancementsScreenButton(int x, int y, Component buttonText) {
        super(
                style == InventoryButtonStyle.BUTTON ? x + offsetX : x - 28 + offsetX,
                style == InventoryButtonStyle.BUTTON ? y + offsetY : y - 28 + offsetY,
                style == InventoryButtonStyle.BUTTON ? 20 : 28,
                style == InventoryButtonStyle.BUTTON ? 18 : 32,
                buttonText
        );
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        Minecraft mc = Minecraft.getInstance();
        this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();

        if (style == InventoryButtonStyle.BUTTON) {
            if (customTexture == null || customTexture.isEmpty()) {
                ResourceLocation sprite = this.isHovered ? ResourceLocation.withDefaultNamespace("widget/button_highlighted") : ResourceLocation.withDefaultNamespace("widget/button");
                guiGraphics.blitSprite(sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
            } else {
                String texToUse = (this.isHovered && customTextureHovered != null && !customTextureHovered.isEmpty()) ? customTextureHovered : customTexture;
                ResourceLocation tex = ResourceLocation.parse(texToUse);

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                guiGraphics.blit(tex, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());

                RenderSystem.disableBlend();
            }

            if (customIcon != null && !customIcon.isEmpty()) {
                guiGraphics.renderFakeItem(getIconStack(), this.getX() + 2, this.getY() + 1);
            }
        } else {
            guiGraphics.blit(Resources.Gui.TABS, this.getX(), this.getY(), 56, 0, 28, 32);
            RenderSystem.defaultBlendFunc();
            guiGraphics.renderFakeItem(getIconStack(), this.getX() + 6, this.getY() + 10);
        }

        if (this.isHovered) {
            guiGraphics.renderTooltip(mc.font, Component.translatable("gui.advancements"), mouseX, mouseY);
        }
    }

    private ItemStack getIconStack() {
        if (customIcon == null || customIcon.isEmpty()) return ItemStack.EMPTY;
        try {
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(customIcon)));
        } catch (Exception e) {
            return new ItemStack(Items.BOOK);
        }
    }

    @Override
    public void onPress() {
        Minecraft.getInstance().setScreen(new EnhancedAdvancementsScreen(Minecraft.getInstance().player.connection.getAdvancements()));
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }
}