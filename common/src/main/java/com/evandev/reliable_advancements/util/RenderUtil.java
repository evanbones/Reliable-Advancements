package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.reference.Resources;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

public class RenderUtil {
    private static final int ARROW_SIZE = 9;
    private static final int FRAME_SIZE = 26;

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

        // Builds the same skewed parallelogram quad as the old vertex-based line renderer:
        // corners offset by `width` along Y (not the line's true normal), which only looks
        // like a clean perpendicular stripe for the ~45-degree diagonals advancement trees use.
        boolean xHigh = x < x2;

        float v0x = x;
        float v0y = xHigh ? y + width : y;
        float v1x = x2;
        float v1y = xHigh ? y2 + width : y2;
        float v3x = x + width;
        float v3y = xHigh ? y : y + width;

        float basisUx = v3x - v0x;
        float basisUy = v3y - v0y;
        float basisVx = v1x - v0x;
        float basisVy = v1y - v0y;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().mul(new Matrix3x2f(basisUx, basisUy, basisVx, basisVy, v0x, v0y));
        guiGraphics.fill(0, 0, 1, 1, color);
        guiGraphics.pose().popMatrix();
    }

    public static void line(GuiGraphicsExtractor guiGraphics, int x0, int y0, int x1, int y1, int thickness, int color) {
        if (x0 == x1 || y0 == y1) {
            int minX = Math.min(x0, x1) - thickness;
            int minY = Math.min(y0, y1) - thickness;
            int maxX = Math.max(x0, x1) + 1 + thickness;
            int maxY = Math.max(y0, y1) + 1 + thickness;
            guiGraphics.fill(minX, minY, maxX, maxY, color);
            return;
        }

        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x0, y0);
        guiGraphics.pose().rotate(angle);

        guiGraphics.fill(-thickness, -thickness, (int) Math.ceil(length) + 1 + thickness, 1 + thickness, color);

        guiGraphics.pose().popMatrix();
    }

    public static void drawRotatedArrow(GuiGraphicsExtractor guiGraphics, float x, float y, float angle, int color) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().rotate(angle);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.ARROWS, -ARROW_SIZE / 2, -ARROW_SIZE / 2, 9f, 9f, ARROW_SIZE, ARROW_SIZE, 18, 18, color);

        guiGraphics.pose().popMatrix();
    }

    public static void drawArrow(GuiGraphicsExtractor guiGraphics, int x, int y, int anchorX, int anchorY, boolean verticalAnchors, boolean goalFrame, int color) {
        int edgeDistance = FRAME_SIZE / 2 + ARROW_SIZE / 2;
        int edgeDistanceX = goalFrame ? edgeDistance - 1 : edgeDistance;
        int edgeDistanceY = goalFrame ? edgeDistance + 1 : edgeDistance;
        int width = ARROW_SIZE;
        int height = ARROW_SIZE;
        float u, v;

        if (verticalAnchors) {
            boolean childIsAbove = (y < anchorY);
            y = moveTowards(y, anchorY, edgeDistanceY);
            x -= ARROW_SIZE / 2;

            u = childIsAbove ? 9f : 0f;
            v = 0f;
            y -= ARROW_SIZE / 2;
        } else {
            boolean childIsLeft = (x < anchorX);
            x = moveTowards(x, anchorX, edgeDistanceX);
            y -= ARROW_SIZE / 2;

            u = childIsLeft ? 0f : 9f;
            v = 9f;
            x -= ARROW_SIZE / 2;
        }

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.ARROWS, x, y, u, v, width, height, 18, 18, color);
    }

    public static void drawDiagonalArrow(GuiGraphicsExtractor guiGraphics, float x, float y, float dx, float dy, boolean goalFrame, int color) {
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
