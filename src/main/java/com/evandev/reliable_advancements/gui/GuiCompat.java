package com.evandev.reliable_advancements.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

//? if <26.1 {
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Quaternionf;
//?} else {
/*import net.minecraft.client.renderer.RenderPipelines;
*///?}

public final class GuiCompat {
    private GuiCompat() {
    }

    public static void push(GuiGraphics g) {
        //? if <26.1 {
        g.pose().pushPose();
        //?} else {
        /*g.pose().pushMatrix();
        *///?}
    }

    public static void pop(GuiGraphics g) {
        //? if <26.1 {
        g.pose().popPose();
        //?} else {
        /*g.pose().popMatrix();
        *///?}
    }

    public static void translate(GuiGraphics g, double x, double y, double z) {
        //? if <26.1 {
        g.pose().translate(x, y, z);
        //?} else {
        /*g.pose().translate((float) x, (float) y);
        *///?}
    }

    public static void scale(GuiGraphics g, float x, float y) {
        //? if <26.1 {
        g.pose().scale(x, y, 1.0F);
        //?} else {
        /*g.pose().scale(x, y);
        *///?}
    }

    public static void rotateZ(GuiGraphics g, float radians) {
        //? if <26.1 {
        g.pose().mulPose(new Quaternionf().rotateZ(radians));
        //?} else {
        /*g.pose().rotate(radians);
        *///?}
    }

    public static int drawString(GuiGraphics g, Font font, @Nullable String text, int x, int y, int color) {
        return drawString(g, font, text, x, y, color, true);
    }

    /**
     * Draws text and returns the x coordinate just past it.
     */
    public static int drawString(GuiGraphics g, Font font, @Nullable String text, int x, int y, int color, boolean shadow) {
        if (text == null) return 0;
        //? if <26.1 {
        return g.drawString(font, text, x, y, color, shadow);
        //?} else {
        /*g.text(font, text, x, y, opaque(color), shadow);
        return x + font.width(text) + (shadow ? 1 : 0);
        *///?}
    }

    public static int drawString(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        return drawString(g, font, text, x, y, color, true);
    }

    public static int drawString(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
        //? if <26.1 {
        return g.drawString(font, text, x, y, color, shadow);
        //?} else {
        /*g.text(font, text, x, y, opaque(color), shadow);
        return x + font.width(text) + (shadow ? 1 : 0);
        *///?}
    }

    public static int drawString(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color) {
        return drawString(g, font, text, x, y, color, true);
    }

    public static int drawString(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        //? if <26.1 {
        return g.drawString(font, text, x, y, color, shadow);
        //?} else {
        /*g.text(font, text, x, y, opaque(color), shadow);
        return x + font.width(text) + (shadow ? 1 : 0);
        *///?}
    }

    //? if >=26.1 {
    /*private static int opaque(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }
    *///?}

    /**
     * Draws a region of a 256x256 texture.
     */
    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int width, int height) {
        blit(g, texture, x, y, u, v, width, height, 256, 256);
    }

    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        //? if <26.1 {
        g.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        //?} else {
        /*g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        *///?}
    }

    /**
     * Draws a region of a texture multiplied by an opaque RGB color.
     */
    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, int color) {
        //? if <26.1 {
        setShaderColor(color);
        g.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        resetShaderColor();
        //?} else {
        /*g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight, 0xFF000000 | color);
        *///?}
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int width, int height) {
        //? if <26.1 {
        g.blitSprite(sprite, x, y, width, height);
        //?} else {
        /*g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
        *///?}
    }

    /**
     * Draws a sprite multiplied by an opaque RGB color.
     */
    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int width, int height, int color) {
        //? if <26.1 {
        setShaderColor(color);
        g.blitSprite(sprite, x, y, width, height);
        resetShaderColor();
        //?} else {
        /*g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, 0xFF000000 | color);
        *///?}
    }

    /**
     * Draws a sub-region of a sprite multiplied by an opaque RGB color.
     */
    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height, int color) {
        //? if <26.1 {
        setShaderColor(color);
        g.blitSprite(sprite, spriteWidth, spriteHeight, u, v, x, y, width, height);
        resetShaderColor();
        //?} else {
        /*g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, spriteWidth, spriteHeight, u, v, x, y, width, height, 0xFF000000 | color);
        *///?}
    }

    public static void raise(GuiGraphics g, double z) {
        //? if <26.1 {
        g.pose().translate(0, 0, z);
        //?} else {
        /*g.nextStratum();
        *///?}
    }

    public static void enableBlend() {
        //? if <26.1 {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        //?}
    }

    public static void disableBlend() {
        //? if <26.1 {
        RenderSystem.disableBlend();
        //?}
    }

    public static void defaultBlendFunc() {
        //? if <26.1 {
        RenderSystem.defaultBlendFunc();
        //?}
    }

    public static void enableDepthTest() {
        //? if <26.1 {
        RenderSystem.enableDepthTest();
        //?}
    }

    public static void disableDepthTest() {
        //? if <26.1 {
        RenderSystem.disableDepthTest();
        //?}
    }

    public static void resetShaderColor() {
        //? if <26.1 {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        //?}
    }

    public static boolean hasShiftDown() {
        //? if <26.1 {
        return Screen.hasShiftDown();
        //?} else {
        /*return Minecraft.getInstance().hasShiftDown();
        *///?}
    }

    public static boolean hasControlDown() {
        //? if <26.1 {
        return Screen.hasControlDown();
        //?} else {
        /*return Minecraft.getInstance().hasControlDown();
        *///?}
    }

    //? if <26.1 {
    private static void setShaderColor(int color) {
        RenderSystem.setShaderColor(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 1.0F);
    }
    //?}
}
