package com.evandev.reliable_advancements.api;

import net.minecraft.advancements.AdvancementNode;

public interface IAdvancementEntryGui {
    AdvancementNode getAdvancement();

    int getX();

    int getY();
}
