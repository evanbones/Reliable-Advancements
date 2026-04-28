package com.evandev.advancement_enhancement.platform.services;

import com.evandev.advancement_enhancement.api.IAdvancementEntryGui;
import com.evandev.advancement_enhancement.api.event.IAdvancementDrawConnectionsEvent;
import com.evandev.advancement_enhancement.api.event.IAdvancementMovedEvent;
import net.minecraft.advancements.AdvancementNode;

public interface IEventHelper {
    IAdvancementMovedEvent postAdvancementMovementEvent(IAdvancementEntryGui gui);

    IAdvancementDrawConnectionsEvent postAdvancementDrawConnectionsEvent(AdvancementNode advancement);
}
