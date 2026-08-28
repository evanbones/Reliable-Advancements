package com.evandev.reliable_advancements.util;

/**
 * Adapted from Advancements Arranged by Seraphaestus, used under the MIT license.
 */
public final class ConnectionRouter {
    public static final int WIDGET_SIZE = 26;
    private static final int MAX_BEND_DISTANCE = 64;

    private ConnectionRouter() {
    }

    public static Route route(int startX, int startY, int endX, int endY, Side parentIncomingSide) {
        int dx = endX - startX;
        int dy = endY - startY;
        int absX = Math.abs(dx);
        int absY = Math.abs(dy);

        boolean verticalAnchors;
        if (absX < WIDGET_SIZE) {
            verticalAnchors = true;
        } else if (absY < WIDGET_SIZE) {
            verticalAnchors = false;
        } else {
            verticalAnchors = tieBreak(dx, dy, parentIncomingSide);
        }

        int endAnchorX = verticalAnchors ? endX : endX - bend(dx);
        int endAnchorY = !verticalAnchors ? endY : endY - bend(dy);

        int startAnchorX = verticalAnchors ? startX : endAnchorX;
        int startAnchorY = !verticalAnchors ? startY : endAnchorY;

        Side exitSide;
        Side entrySide;
        if (verticalAnchors) {
            exitSide = compareSide(endAnchorY, startY, Side.BOTTOM, Side.TOP);
            entrySide = compareSide(endY, endAnchorY, Side.TOP, Side.BOTTOM);
        } else {
            exitSide = compareSide(endAnchorX, startX, Side.RIGHT, Side.LEFT);
            entrySide = compareSide(endX, endAnchorX, Side.LEFT, Side.RIGHT);
        }

        return new Route(startX, startY, startAnchorX, startAnchorY, endAnchorX, endAnchorY, endX, endY,
                verticalAnchors, exitSide, entrySide);
    }

    private static boolean tieBreak(int dx, int dy, Side parentIncomingSide) {
        switch (parentIncomingSide) {
            case TOP, BOTTOM -> {
                Side exitSide = dy > 0 ? Side.BOTTOM : Side.TOP;
                if (exitSide != parentIncomingSide) return true;
            }
            case LEFT, RIGHT -> {
                Side exitSide = dx > 0 ? Side.RIGHT : Side.LEFT;
                if (exitSide != parentIncomingSide) return false;
            }
            default -> {
            }
        }
        return Math.abs(dx) > Math.abs(dy);
    }

    private static int bend(int delta) {
        if (delta >= 0) {
            return Math.min(delta / 2, MAX_BEND_DISTANCE);
        } else {
            return -Math.min((-delta + 1) / 2, MAX_BEND_DISTANCE);
        }
    }

    private static Side compareSide(int value, int against, Side ifGreater, Side ifLess) {
        if (value > against) return ifGreater;
        if (value < against) return ifLess;
        return Side.NONE;
    }

    public enum Side {
        NONE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    public record Route(int startX, int startY,
                        int startAnchorX, int startAnchorY,
                        int endAnchorX, int endAnchorY,
                        int endX, int endY,
                        boolean verticalAnchors,
                        Side exitSide,
                        Side entrySide) {

        public boolean shouldShowArrow() {
            int alongStub = verticalAnchors ? Math.abs(endY - startY) : Math.abs(endX - startX);
            int acrossStub = verticalAnchors ? Math.abs(endX - startX) : Math.abs(endY - startY);

            if (alongStub > WIDGET_SIZE + 7) return true;
            return acrossStub == 0 && alongStub > WIDGET_SIZE + 2;
        }
    }
}
