package com.evandev.reliable_advancements.api.event;

import net.minecraft.advancements.Advancement;

public interface IAdvancementMovedEvent {
    Advancement getAdvancement();

    int getX();

    int getY();
}
