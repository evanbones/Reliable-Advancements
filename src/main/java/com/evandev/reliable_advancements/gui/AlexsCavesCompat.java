package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AlexsCavesCompat {
    private static final String NAMESPACE = "alexscaves";

    private static boolean resolved;
    private static @Nullable Method setDimensions;
    private static @Nullable Method renderTabBackground;
    private static @Nullable Method setHoverType;
    private static @Nullable Method tick;
    private static Object[] types = new Object[0];
    private static Object[] typeRoots = new Object[0];
    private static @Nullable Object defaultType;

    public static boolean appliesTo(@Nullable AdvancementNode root) {
        return root != null && root.holder().id().getNamespace().equals(NAMESPACE) && resolve();
    }

    public static boolean drawBackground(GuiGraphics guiGraphics, @Nullable AdvancementNode root, int scrollX, int scrollY, int width, int height) {
        try {
            setDimensions.invoke(null, width, height);
            if (renderTabBackground.getParameterCount() == 6) {
                Object display = root == null ? null : root.advancement().display().orElse(null);
                renderTabBackground.invoke(null, guiGraphics, 0, 0, display, (double) scrollX, (double) scrollY);
            } else {
                renderTabBackground.invoke(null, guiGraphics, 0, 0, (double) scrollX, (double) scrollY);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
            return false;
        }
    }

    public static void onHovered(EnhancedAdvancementTab tab, EnhancedAdvancementWidget widget) {
        try {
            AdvancementNode biomeRoot = biomeRoot(widget.getAdvancement());
            Object type = biomeRoot == null ? defaultType : directType(biomeRoot);
            boolean unlocked = isDone(widget) || biomeRoot == null || isDone(tab.getWidget(biomeRoot.holder().id()));
            if (unlocked) {
                setHoverType.invoke(null, type);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
        }
    }

    public static void tick() {
        try {
            tick.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
        }
    }

    private static @Nullable AdvancementNode biomeRoot(AdvancementNode node) {
        for (AdvancementNode next = node; next != null; next = next.parent()) {
            Object type = directType(next);
            if (type != null && type != defaultType) return next;
        }
        return null;
    }

    private static @Nullable Object directType(AdvancementNode node) {
        Object id = node.holder().id();
        for (int i = 0; i < types.length; i++) {
            if (id.equals(typeRoots[i])) return types[i];
        }
        return null;
    }

    private static boolean isDone(@Nullable EnhancedAdvancementWidget widget) {
        return widget != null && widget.advancementProgress != null && widget.advancementProgress.isDone();
    }

    private static boolean resolve() {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> tabs = Class.forName("com.github.alexmodguy.alexscaves.client.gui.ACAdvancementTabs");
                Class<?> type = Class.forName("com.github.alexmodguy.alexscaves.client.gui.ACAdvancementTabs$Type");
                Field root = type.getDeclaredField("root");
                root.setAccessible(true);
                Object[] values = type.getEnumConstants();
                Object[] roots = new Object[values.length];
                for (int i = 0; i < values.length; i++) {
                    roots[i] = root.get(values[i]);
                }
                Method render = null;
                for (Method method : tabs.getMethods()) {
                    if (method.getName().equals("renderTabBackground")
                            && (method.getParameterCount() == 5 || method.getParameterCount() == 6)) {
                        render = method;
                    }
                }
                if (render == null) throw new NoSuchMethodException("ACAdvancementTabs.renderTabBackground");

                setDimensions = tabs.getMethod("setDimensions", int.class, int.class);
                setHoverType = tabs.getMethod("setHoverType", type);
                tick = tabs.getMethod("tick");
                defaultType = type.getField("DEFAULT").get(null);
                types = values;
                typeRoots = roots;
                renderTabBackground = render;
            } catch (ClassNotFoundException ignored) {
                // Alex's Caves not installed
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                Constants.LOG.warn("Could not hook Alex's Caves advancement backgrounds", e);
            }
        }
        return renderTabBackground != null;
    }

    private static void disable(Throwable e) {
        if (renderTabBackground != null) {
            Constants.LOG.warn("Disabling Alex's Caves advancement background compat", e);
            renderTabBackground = null;
        }
    }
}
