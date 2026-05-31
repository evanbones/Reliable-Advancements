package com.evandev.reliable_advancements.api.event;

import net.minecraft.advancements.AdvancementNode;

public interface IAdvancementMovedEvent {
    AdvancementNode getAdvancement();

    int getX();

    int getY();
}
