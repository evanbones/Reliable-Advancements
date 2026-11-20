package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

//? if <26.1 {
import com.mojang.blaze3d.systems.RenderSystem;
//?}

public class ModdedTabEffects {
    private static final String CREATURE_FEATURE = "creaturefeature";
    private static final String NETHERMORE = "nethermore";
    private static final String MAN_CRITERION = "man_test_criteria_name";

    private static final ResourceLocation[] NM_BACKGROUND = {
            ResourceLocation.fromNamespaceAndPath(NETHERMORE, "textures/gui/sprites/misc/advancements_bg.png"),
            ResourceLocation.fromNamespaceAndPath(NETHERMORE, "textures/gui/sprites/misc/advancements_bg_2.png")
    };
    private static final ResourceLocation[] NM_FOREGROUND = {
            ResourceLocation.fromNamespaceAndPath(NETHERMORE, "textures/gui/sprites/misc/advancements_fg.png"),
            ResourceLocation.fromNamespaceAndPath(NETHERMORE, "textures/gui/sprites/misc/advancements_fg_2.png")
    };
    private static final ResourceLocation NM_GLOW = ResourceLocation.fromNamespaceAndPath(NETHERMORE, "textures/gui/sprites/misc/glow.png");
    private static final ResourceLocation NM_GLOW_2 = ResourceLocation.fromNamespaceAndPath(NETHERMORE, "textures/gui/sprites/misc/glow2.png");

    private static final ProxyField SEARCHING_FOR_HIM = new ProxyField("net.atired.creaturefeature.client.CreatureFeatureClient", "searchingForHim");
    private static final ProxyField NIHILO_NESS = new ProxyField("net.atired.nethermore.client.NethermoreClient", "nihiloNess");

    private float scroll = 0.0F;
    private float scrollSpeed = 1.0F;

    public boolean drawBackground(GuiGraphics g, ResourceLocation background, int scrollX, int scrollY, int width, int height, float fade) {
        return switch (background.getNamespace()) {
            case CREATURE_FEATURE -> SEARCHING_FOR_HIM.exists() && drawCreatureFeatureBackground(g, background, scrollX, scrollY, width, height);
            case NETHERMORE -> NIHILO_NESS.exists() && drawNethermoreBackground(g, scrollX, scrollY, width, height, fade);
            default -> false;
        };
    }

    public void drawOverlay(GuiGraphics g, ResourceLocation background, int width, int height, float fade) {
        if (background.getNamespace().equals(NETHERMORE) && NIHILO_NESS.exists()) {
            float scale = Math.max(1.0F, height / 113.0F);
            GuiCompat.push(g);
            GuiCompat.scale(g, scale, scale);
            drawNethermoreOverlay(g, (int) Math.ceil(width / scale), fade);
            GuiCompat.pop(g);
        }
    }

    private boolean drawCreatureFeatureBackground(GuiGraphics g, ResourceLocation background, int scrollX, int scrollY, int width, int height) {
        if (searchingOnNetherRoof()) {
            this.scrollSpeed *= 0.94F;
        } else if (this.scrollSpeed < 0.99F) {
            this.scrollSpeed = Math.min(this.scrollSpeed + 0.05F, 1.0F);
        }
        this.scroll = (this.scroll - 0.1F * this.scrollSpeed) % 64.0F;

        int k = Math.floorMod(scrollX, 64);
        int l = Math.floorMod(scrollY, 64);
        int columns = width / 64 + 3;
        int rows = height / 64 + 2;

        GuiCompat.push(g);
        GuiCompat.translate(g, this.scroll, this.scroll, 0);
        for (int m = -1; m <= columns; m++) {
            for (int n = -1; n <= rows; n++) {
                GuiCompat.blit(g, background, k + 64 * m, l + 64 * n, 0.0F, 0.0F, 64, 64, 64, 64);
            }
        }
        GuiCompat.pop(g);

        setColor(1.0F, this.scrollSpeed, this.scrollSpeed, 0.66F);
        GuiCompat.enableBlend();
        GuiCompat.push(g);
        GuiCompat.translate(g, this.scroll * 2.0F, (float) Math.sin(this.scroll / 4.0F * 3.14F), 0);
        for (int m = -1; m <= columns; m++) {
            for (int n = -1; n <= rows; n++) {
                GuiCompat.blit(g, background, k + 64 * m, l + 64 * n, 0.0F, 0.0F, 64, 64, 64, 64);
            }
        }
        GuiCompat.pop(g);
        GuiCompat.resetShaderColor();
        return true;
    }

    private boolean drawNethermoreBackground(GuiGraphics g, int scrollX, int scrollY, int width, int height, float fade) {
        float nihilo = NIHILO_NESS.getFloat();
        int k = Math.floorMod(scrollX, 64);
        int l = Math.floorMod(scrollY, 64);
        int columns = width / 64 + 1;
        int rows = height / 64 + 1;
        int pad = 4;

        GuiCompat.enableBlend();
        for (int m = -pad; m <= columns + pad; m++) {
            for (int n = -pad; n <= rows + pad; n++) {
                if (nihilo < 0.9F) {
                    GuiCompat.blit(g, NM_BACKGROUND[0], k + 64 * m, l + 64 * n, 0.0F, this.scroll, 64, 64, 64, 64);
                }
                if (nihilo > 0.1F) {
                    float glow = Math.clamp(1.0F - fade * 3.5F, 0.0F, 1.0F);
                    setColor(0.9F + glow * 0.1F, 0.3F + glow * 0.7F, 0.3F + glow * 0.7F, nihilo);
                    GuiCompat.push(g);
                    GuiCompat.rotateZ(g, 0.5F);
                    GuiCompat.blit(g, NM_BACKGROUND[1], k + 64 * m, l + 64 * n, this.scroll / 2.0F, this.scroll * 1.5F, 64, 64, 64, 64);
                    GuiCompat.pop(g);
                }
            }
        }

        if (nihilo > 0.1F) {
            float glow = Math.max(0.0F, 1.0F - fade * 3.0F);
            setColor(0.9F + glow * 0.1F, 0.3F + glow * 0.7F, 0.3F + glow * 0.7F, nihilo * 0.3F);
            for (int m = -pad; m <= columns + pad; m++) {
                for (int n = -pad; n <= rows + pad; n++) {
                    GuiCompat.push(g);
                    GuiCompat.rotateZ(g, -0.5F);
                    GuiCompat.blit(g, NM_BACKGROUND[1], k + 64 * m, l + 64 * n, -this.scroll / 2.0F, this.scroll * 1.5F, 64, 64, 64, 64);
                    GuiCompat.pop(g);
                }
            }
        }

        for (int m = -2; m <= columns + 2; m++) {
            for (int n = -2; n <= rows + 2; n++) {
                if (nihilo < 0.9F) {
                    GuiCompat.resetShaderColor();
                    GuiCompat.blit(g, NM_FOREGROUND[0], k + 64 * m, l + 64 * n, this.scroll, this.scroll / 2.0F, 64, 64, 64, 64);
                }
                if (nihilo > 0.1F) {
                    float glow = Math.max(0.0F, 1.0F - fade * 3.0F);
                    setColor(0.9F + glow * 0.1F, 0.3F + glow * 0.7F, 0.3F + glow * 0.7F, nihilo * glow);
                    GuiCompat.blit(g, NM_FOREGROUND[1], k + 64 * m, l + 64 * n, this.scroll / 2.0F + 8.0F, this.scroll * 2.0F + 8.0F, 128, 128, 128, 128);
                    GuiCompat.blit(g, NM_FOREGROUND[1], k + 64 * m, l + 64 * n, this.scroll / 2.0F + 40.0F, this.scroll * 2.0F + 40.0F, 128, 128, 128, 128);
                }
            }
        }

        GuiCompat.resetShaderColor();
        return true;
    }

    private void drawNethermoreOverlay(GuiGraphics g, int width, float fade) {
        float nihilo = NIHILO_NESS.getFloat();
        int strips = width / 234 + 1;

        GuiCompat.enableBlend();
        if (nihilo < 0.95F) {
            this.scroll -= 0.1F * (1.0F + fade * 3.0F);
            if (nihilo > 0.1F) {
                setColor(1.0F, 1.0F, 1.0F, 1.0F - nihilo);
            }
            GuiCompat.push(g);
            GuiCompat.translate(g, this.scroll / 64.0F * 234.0F % 234.0F, (1.0F - fade) * 30.0F, 200.0F);
            for (int s = -1; s <= strips; s++) {
                GuiCompat.blit(g, NM_GLOW, 234 * s, 0, 0.0F, 0.0F, 234, 113, 234, 113);
            }
            GuiCompat.pop(g);
        } else {
            this.scroll -= 0.1F;
            float faded = Math.min(0.33F, (float) Math.pow(fade * 3.0F, 2.0));
            GuiCompat.push(g);
            GuiCompat.translate(g, this.scroll / 256.0F * 234.0F % 234.0F, (1.0F - faded) * 48.0F, 200.0F);
            for (int s = -1; s <= strips; s++) {
                GuiCompat.blit(g, NM_GLOW_2, 234 * s, 0, 0.0F, 0.0F, 234, 113, 234, 113);
            }
            setColor(0.6F, 0.6F, 0.6F, 1.0F);
            GuiCompat.translate(g, -(this.scroll / 128.0F * 234.0F) % 234.0F, 16.0F, -1.0F);
            for (int s = -1; s <= strips; s++) {
                GuiCompat.blit(g, NM_GLOW_2, 234 * s, 0, 64.0F, 0.0F, 234, 113, 234, 113);
            }
            GuiCompat.pop(g);
        }
        GuiCompat.resetShaderColor();
        this.scroll %= 256.0F;
    }

    public static void onHovered(EnhancedAdvancementWidget widget) {
        if (!isMan(widget) || isDone(widget) || !SEARCHING_FOR_HIM.exists() || SEARCHING_FOR_HIM.getBoolean()) return;
        SEARCHING_FOR_HIM.setBoolean(true);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.BELL_RESONATE, 0.2F, 2.0F);
        }
    }

    public static void tickWidget(EnhancedAdvancementWidget widget) {
        if (EnhancedAdvancementsScreen.canEdit() || !isMan(widget) || isDone(widget) || !SEARCHING_FOR_HIM.exists()) return;
        if (widget.driftSpeed > -0.1F) {
            widget.driftOffset += (int) (3.0F * widget.driftSpeed);
            widget.driftSpeed *= 1.1F;
            if (widget.driftSpeed > 20.0F) {
                widget.driftSpeed = -1.0F;
            }
        }
    }

    public static @Nullable Component descriptionOverride(EnhancedAdvancementWidget widget) {
        if (!isMan(widget) || !SEARCHING_FOR_HIM.exists()) return null;
        if (isDone(widget)) {
            return Component.literal("Well, the man thanked you, then he bowed, and left.");
        }
        return searchingOnNetherRoof() ? Component.literal("I WILL SHOW YOU A GREAT VESTIGE TO THE EAST.") : null;
    }

    private static boolean isMan(EnhancedAdvancementWidget widget) {
        List<List<String>> requirements = widget.getAdvancement().advancement().requirements().requirements();
        return !requirements.isEmpty() && !requirements.getFirst().isEmpty() && MAN_CRITERION.equals(requirements.getFirst().getFirst());
    }

    private static boolean isDone(EnhancedAdvancementWidget widget) {
        return widget.advancementProgress != null && widget.advancementProgress.getPercent() >= 1.0F;
    }

    private static boolean searchingOnNetherRoof() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        //? if <26.1 {
        return SEARCHING_FOR_HIM.getBoolean() && mc.player != null && mc.player.getY() > 127.8
                && level != null && level.dimensionType().respawnAnchorWorks();
        //?} else {
        /*// Creature Feature only exists for 1.21.1.
        return false;
        *///?}
    }

    private static void setColor(float r, float g, float b, float a) {
        //? if <26.1 {
        RenderSystem.setShaderColor(r, g, b, a);
        //?}
    }

    private static final class ProxyField {
        private final String ownerClass;
        private final String fieldName;
        private boolean resolved;
        private @Nullable Object proxy;
        private @Nullable Field field;

        ProxyField(String ownerClass, String fieldName) {
            this.ownerClass = ownerClass;
            this.fieldName = fieldName;
        }

        boolean exists() {
            if (!this.resolved) {
                this.resolved = true;
                try {
                    Class<?> owner = Class.forName(this.ownerClass);
                    Object proxy = owner.getField("PROXY").get(null);
                    if (proxy != null) {
                        this.field = proxy.getClass().getField(this.fieldName);
                        this.proxy = proxy;
                    }
                } catch (ClassNotFoundException ignored) {
                    // Mod not installed.
                } catch (ReflectiveOperationException | LinkageError e) {
                    Constants.LOG.warn("Could not hook {}.PROXY.{} for advancement screen compat", this.ownerClass, this.fieldName, e);
                }
            }
            return this.field != null;
        }

        boolean getBoolean() {
            try {
                return exists() && this.field.getBoolean(this.proxy);
            } catch (IllegalAccessException e) {
                return false;
            }
        }

        void setBoolean(boolean value) {
            try {
                if (exists()) this.field.setBoolean(this.proxy, value);
            } catch (IllegalAccessException ignored) {
            }
        }

        float getFloat() {
            try {
                return exists() ? this.field.getFloat(this.proxy) : 0.0F;
            } catch (IllegalAccessException e) {
                return 0.0F;
            }
        }
    }
}
