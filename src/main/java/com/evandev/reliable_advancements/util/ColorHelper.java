package com.evandev.reliable_advancements.util;

public class ColorHelper {
    public static int RGB(int r, int g, int b) {
        return RGBA(r, g, b, 255);
    }

    public static int RGBA(int r, int g, int b, int a) {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    public static int RGB(float red, float green, float blue) {
        return RGBA((int) (red * 255), (int) (green * 255), (int) (blue * 255), 255);
    }

    public static int RGB(float red, float green, float blue, float alpha) {
        return RGBA((int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));
    }

    public static int RGB(String colour) {
        if (!colour.startsWith("#") || !(colour.length() == 7))
            throw new IllegalArgumentException("Use #RRGGBB format");
        return RGB(Integer.parseInt(colour.substring(1, 3), 16), Integer.parseInt(colour.substring(3, 5), 16), Integer.parseInt(colour.substring(5, 7), 16));
    }

    public static String asRGBString(int colour) {
        return String.format("#%06X", colour & 0xFFFFFF);
    }
}
