package com.evandev.reliable_advancements.reference;

import net.minecraft.resources.Identifier;

public final class Resources {
    private static Identifier resourceLocation(String location) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, location);
    }

    public static final class Gui {
        public static final Identifier WINDOW = resourceLocation(Textures.Gui.WINDOW);
        public static final Identifier TABS = resourceLocation(Textures.Gui.TABS);
        public static final Identifier WIDGETS = resourceLocation(Textures.Gui.WIDGETS);
        public static final Identifier ARROWS = resourceLocation(Textures.Gui.ARROWS);
    }
}