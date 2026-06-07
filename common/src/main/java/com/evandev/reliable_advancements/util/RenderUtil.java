package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.reference.Resources;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class RenderUtil {
    private RenderUtil() {
    }

    public static void renderRepeating(Identifier texture, GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int textureX, int textureY, int textureWidth, int textureHeight) {
        for (int i = 0; i < width; i += textureWidth) {
            int drawX = x + i;
            int drawWidth = Math.min(textureWidth, width - i);

            for (int l = 0; l < height; l += textureHeight) {
                int drawY = y + l;
                int drawHeight = Math.min(textureHeight, height - l);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, drawX, drawY, textureX, textureY, drawWidth, drawHeight, 256, 256);
            }
        }
    }

    public static void drawRect(GuiGraphicsExtractor guiGraphics, float x, float y, float x2, float y2, float width, int color) {
        if (y > y2) {
            float tempY = y;
            float tempX = x;
            y = y2;
            x = x2;
            y2 = tempY;
            x2 = tempX;
        }
        float minX = Math.min(x, x2);
        float maxX = Math.max(x, x2) + width;
        float minY = y;
        float maxY = y2 + width;

        guiGraphics.fill((int) minX, (int) minY, (int) maxX, (int) maxY, color);
    }

    public static void line(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1, int thickness, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x0, y0);
        guiGraphics.pose().rotate(angle);

        int halfThickness = thickness / 2;
        int extraThickness = thickness % 2;

        guiGraphics.fill(0, -halfThickness, (int) Math.ceil(length), halfThickness + extraThickness, color);

        guiGraphics.pose().popMatrix();
    }

    public static void drawRotatedArrow(GuiGraphicsExtractor guiGraphics, float x, float y, float angle, int color) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().rotate(angle);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.ARROWS, -4, -4, 9f, 9f, 9, 9, 18, 18, color);

        guiGraphics.pose().popMatrix();
    }

    public static void drawArrow(GuiGraphicsExtractor guiGraphics, int x, int y, int anchorX, int anchorY, boolean verticalAnchors, int edgeDistanceX, int edgeDistanceY, int color) {
        int width = 9;
        int height = 9;
        float u, v;

        if (verticalAnchors) {
            boolean childIsAbove = (y < anchorY);
            y = moveTowards(y, anchorY, edgeDistanceY);
            x -= 4;

            u = childIsAbove ? 9f : 0f;
            v = 0f;
            y -= 4;
        } else {
            boolean childIsLeft = (x < anchorX);
            x = moveTowards(x, anchorX, edgeDistanceX);
            y -= 4;

            u = childIsLeft ? 0f : 9f;
            v = 9f;
            x -= 4;
        }

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.ARROWS, x, y, u, v, width, height, 18, 18, color);
    }

    public static int moveTowards(int a, int b, int distance) {
        return (b > a) ? Math.min(a + distance, b) : Math.max(a - distance, b);
    }
}