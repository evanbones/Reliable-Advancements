package com.evandev.advancement_enhancement.api;

import net.minecraft.advancements.AdvancementNode;

public interface IAdvancementEntryGui {
    AdvancementNode getAdvancement();

    int getX();

    int getY();
}
