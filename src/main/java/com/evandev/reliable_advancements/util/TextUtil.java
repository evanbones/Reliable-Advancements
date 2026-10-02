package com.evandev.reliable_advancements.util;

public final class TextUtil {
    private TextUtil() {
    }

    public static String titleCase(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(raw.length());
        for (String part : raw.split("[_\\s]+")) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return !sb.isEmpty() ? sb.toString() : raw;
    }

    public static String humanize(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return titleCase(raw.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " "));
    }
}
