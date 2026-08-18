package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.reference.Resources;
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
    private static final int ARROW_SIZE = 9;
    private static final int FRAME_SIZE = 26;

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
        if (x0 == x1 || y0 == y1) {
            int minX = Math.min(x0, x1) - thickness;
            int minY = Math.min(y0, y1) - thickness;
            int maxX = Math.max(x0, x1) + 1 + thickness;
            int maxY = Math.max(y0, y1) + 1 + thickness;
            guiGraphics.fill(RenderType.gui(), minX, minY, maxX, maxY, color);
            return;
        }

        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x0, y0, 0);
        guiGraphics.pose().mulPose(new Quaternionf().rotateZ(angle));
        guiGraphics.fill(RenderType.gui(), -thickness, -thickness, (int) Math.ceil(length) + 1 + thickness, 1 + thickness, color);
        guiGraphics.pose().popPose();
    }

    public static void drawRotatedArrow(GuiGraphics guiGraphics, float x, float y, float angle, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderUtil.setColor(color);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().mulPose(new Quaternionf().rotateZ(angle));

        guiGraphics.blit(Resources.Gui.ARROWS, -ARROW_SIZE / 2, -ARROW_SIZE / 2, 9, 9, ARROW_SIZE, ARROW_SIZE, 18, 18);

        guiGraphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    public static void drawArrow(GuiGraphics guiGraphics, int x, int y, int anchorX, int anchorY, boolean verticalAnchors, boolean goalFrame, int color) {
        int edgeDistance = FRAME_SIZE / 2 + ARROW_SIZE / 2;
        int edgeDistanceX = goalFrame ? edgeDistance - 1 : edgeDistance;
        int edgeDistanceY = goalFrame ? edgeDistance + 1 : edgeDistance;
        float u, v;

        if (verticalAnchors) {
            boolean childIsAbove = (y < anchorY);
            y = moveTowards(y, anchorY, edgeDistanceY);
            x -= ARROW_SIZE / 2;
            u = childIsAbove ? 9 : 0;
            v = 0;
            y -= ARROW_SIZE / 2;
        } else {
            boolean childIsLeft = (x < anchorX);
            x = moveTowards(x, anchorX, edgeDistanceX);
            y -= ARROW_SIZE / 2;
            u = childIsLeft ? 0 : 9;
            v = 9;
            x -= ARROW_SIZE / 2;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderUtil.setColor(color);
        guiGraphics.blit(Resources.Gui.ARROWS, x, y, u, v, ARROW_SIZE, ARROW_SIZE, 18, 18);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    public static void drawDiagonalArrow(GuiGraphics guiGraphics, float x, float y, float dx, float dy, boolean goalFrame, int color) {
        float radius = FRAME_SIZE / 2.0F + ARROW_SIZE / 2.0F;
        float offsetX, offsetY;

        if (goalFrame) {
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            offsetX = (dx / distance) * (radius + 1.0F);
            offsetY = (dy / distance) * (radius + 1.0F);
        } else {
            float maxAxis = Math.max(Math.abs(dx), Math.abs(dy));
            offsetX = (dx / maxAxis) * radius;
            offsetY = (dy / maxAxis) * radius;
        }

        drawRotatedArrow(guiGraphics, x - offsetX, y - offsetY, (float) Math.atan2(dy, dx), color);
    }

    public static int moveTowards(int a, int b, int distance) {
        return (b > a) ? Math.min(a + distance, b) : Math.max(a - distance, b);
    }
}