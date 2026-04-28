package com.evandev.advancement_enhancement.platform;

import com.evandev.advancement_enhancement.api.IAdvancementEntryGui;
import com.evandev.advancement_enhancement.api.event.AdvancementDrawConnectionsEvent;
import com.evandev.advancement_enhancement.api.event.IAdvancementDrawConnectionsEvent;
import com.evandev.advancement_enhancement.api.event.IAdvancementMovedEvent;
import com.evandev.advancement_enhancement.platform.services.IEventHelper;
import net.minecraft.advancements.AdvancementNode;

public class FabricEventHelper implements IEventHelper {
    @Override
    public IAdvancementMovedEvent postAdvancementMovementEvent(IAdvancementEntryGui gui) {
        return null;
    }

    @Override
    public IAdvancementDrawConnectionsEvent postAdvancementDrawConnectionsEvent(AdvancementNode advancement) {
        IAdvancementDrawConnectionsEvent event = new AdvancementDrawConnectionsEvent(advancement);
        // TODO send event to other mods
        return event;
    }
}
