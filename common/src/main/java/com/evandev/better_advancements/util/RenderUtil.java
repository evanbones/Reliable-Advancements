package com.evandev.better_advancements.util;

import com.evandev.better_advancements.reference.Resources;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class RenderUtil {
    private RenderUtil() {
    }

    public static void renderRepeating(ResourceLocation texture, GuiGraphics guiGraphics, int x, int y, int width, int height, int textureX, int textureY, int textureWidth, int textureHeight) {
        for (int i = 0; i < width; i += textureWidth) {
            int drawX = x + i;
            int drawWidth = Math.min(textureWidth, width - i);

            for (int l = 0; l < height; l += textureHeight) {
                int drawY = y + l;
                int drawHeight = Math.min(textureHeight, height - l);
                guiGraphics.blit(texture, drawX, drawY, textureX, textureY, drawWidth, drawHeight);
            }
        }
    }

    public static void setColor(int color) {
        RenderSystem.setShaderColor(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 1.0F);
    }

    public static void drawRect(GuiGraphics guiGraphics, float x, float y, float x2, float y2, float width, int color) {
        if (y > y2) {
            float tempY = y;
            float tempX = x;
            y = y2;
            x = x2;
            y2 = tempY;
            x2 = tempX;
        }
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderUtil.setColor(color);

        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        boolean xHigh = x < x2;

        bufferbuilder.addVertex(matrix, x, xHigh ? y + width : y, 0.0F);
        bufferbuilder.addVertex(matrix, x2, xHigh ? y2 + width : y2, 0.0F);
        bufferbuilder.addVertex(matrix, x2 + width, xHigh ? y2 : y2 + width, 0.0F);
        bufferbuilder.addVertex(matrix, x + width, xHigh ? y : y + width, 0.0F);

        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());

        RenderSystem.disableBlend();
    }

    public static void line(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int thickness, int color) {
        if (x0 > x1) {
            int temp = x0;
            x0 = x1;
            x1 = temp;
        }
        if (y0 > y1) {
            int temp = y0;
            y0 = y1;
            y1 = temp;
        }
        guiGraphics.fill(RenderType.gui(), x0 - thickness, y0 - thickness, x1 + 1 + thickness, y1 + 1 + thickness, color);
    }

    public static void drawRotatedArrow(GuiGraphics guiGraphics, float x, float y, float angle, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderUtil.setColor(color);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().mulPose(new Quaternionf().rotateZ(angle));

        guiGraphics.blit(Resources.Gui.ARROWS, -4, -4, 9, 9, 9, 9, 18, 18);

        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    public static void drawArrow(GuiGraphics guiGraphics, int x, int y, int anchorX, int anchorY, boolean verticalAnchors, int edgeDistanceX, int edgeDistanceY, int color) {
        int width = 9;
        int height = 9;
        float u, v;

        if (verticalAnchors) {
            boolean childIsAbove = (y < anchorY);
            y = moveTowards(y, anchorY, edgeDistanceY);
            x -= 4;

            if (childIsAbove) {
                u = 9;
            } else {
                u = 0;
            }
            v = 0;
            y -= 4;

        } else {
            boolean childIsLeft = (x < anchorX);
            x = moveTowards(x, anchorX, edgeDistanceX);
            y -= 4;

            if (childIsLeft) {
                u = 0;
            } else {
                u = 9;
            }
            v = 9;
            x -= 4;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderUtil.setColor(color);
        guiGraphics.blit(Resources.Gui.ARROWS, x, y, u, v, width, height, 18, 18);
        RenderSystem.disableBlend();
    }

    public static int moveTowards(int a, int b, int distance) {
        return (b > a) ? Math.min(a + distance, b) : Math.max(a - distance, b);
    }
}