package com.evandev.reliable_advancements.platform;

import com.evandev.reliable_advancements.api.IAdvancementEntryGui;
import com.evandev.reliable_advancements.api.event.AdvancementDrawConnectionsEvent;
import com.evandev.reliable_advancements.api.event.IAdvancementDrawConnectionsEvent;
import com.evandev.reliable_advancements.api.event.IAdvancementMovedEvent;
import com.evandev.reliable_advancements.platform.services.IEventHelper;
import net.minecraft.advancements.AdvancementNode;

public class FabricEventHelper implements IEventHelper {
    @Override
    public IAdvancementMovedEvent postAdvancementMovementEvent(IAdvancementEntryGui gui) {
        return null;
    }

    @Override
    public IAdvancementDrawConnectionsEvent postAdvancementDrawConnectionsEvent(AdvancementNode advancement) {
        IAdvancementDrawConnectionsEvent event = new AdvancementDrawConnectionsEvent(advancement);
        return event;
    }
}
