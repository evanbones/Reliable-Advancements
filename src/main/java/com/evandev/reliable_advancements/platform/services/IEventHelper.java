package com.evandev.reliable_advancements.platform.services;

import com.evandev.reliable_advancements.api.IAdvancementEntryGui;
import com.evandev.reliable_advancements.api.event.IAdvancementDrawConnectionsEvent;
import com.evandev.reliable_advancements.api.event.IAdvancementMovedEvent;
import net.minecraft.advancements.AdvancementNode;

public interface IEventHelper {
    IAdvancementMovedEvent postAdvancementMovementEvent(IAdvancementEntryGui gui);

    IAdvancementDrawConnectionsEvent postAdvancementDrawConnectionsEvent(AdvancementNode advancement);
}
