package com.evandev.better_advancements.platform.services;

import com.evandev.better_advancements.api.IBetterAdvancementEntryGui;
import com.evandev.better_advancements.api.event.IAdvancementDrawConnectionsEvent;
import com.evandev.better_advancements.api.event.IAdvancementMovedEvent;
import net.minecraft.advancements.AdvancementNode;

public interface IEventHelper {
    IAdvancementMovedEvent postAdvancementMovementEvent(IBetterAdvancementEntryGui gui);

    IAdvancementDrawConnectionsEvent postAdvancementDrawConnectionsEvent(AdvancementNode advancement);
}
