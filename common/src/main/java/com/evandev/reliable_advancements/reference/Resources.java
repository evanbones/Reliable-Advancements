package com.evandev.reliable_advancements.reference;

import net.minecraft.resources.Identifier;

public final class Resources {
    private static Identifier Identifier(String location) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, location);
    }

    public static final class Gui {
        public static final Identifier WINDOW = Identifier(Textures.Gui.WINDOW);
        public static final Identifier TABS = Identifier(Textures.Gui.TABS);
        public static final Identifier WIDGETS = Identifier(Textures.Gui.WIDGETS);
        public static final Identifier ARROWS = Identifier(Textures.Gui.ARROWS);
    }
}