package com.evandev.advancement_enhancement.api.event;

import net.minecraft.advancements.AdvancementNode;

public interface IAdvancementMovedEvent {
    AdvancementNode getAdvancement();

    int getX();

    int getY();
}
