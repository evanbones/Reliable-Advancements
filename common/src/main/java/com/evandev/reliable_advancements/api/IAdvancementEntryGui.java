package com.evandev.reliable_advancements.api;

import net.minecraft.advancements.Advancement;

public interface IAdvancementEntryGui {
    Advancement getAdvancement();

    int getX();

    int getY();
}
