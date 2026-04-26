package com.evandev.better_advancements.platform;

import com.evandev.better_advancements.api.IBetterAdvancementEntryGui;
import com.evandev.better_advancements.api.event.AdvancementDrawConnectionsEvent;
import com.evandev.better_advancements.api.event.IAdvancementDrawConnectionsEvent;
import com.evandev.better_advancements.api.event.IAdvancementMovedEvent;
import com.evandev.better_advancements.platform.services.IEventHelper;
import net.minecraft.advancements.AdvancementNode;

public class FabricEventHelper implements IEventHelper {
    @Override
    public IAdvancementMovedEvent postAdvancementMovementEvent(IBetterAdvancementEntryGui gui) {
        return null;
    }

    @Override
    public IAdvancementDrawConnectionsEvent postAdvancementDrawConnectionsEvent(AdvancementNode advancement) {
        IAdvancementDrawConnectionsEvent event = new AdvancementDrawConnectionsEvent(advancement);
        // TODO send event to other mods
        return event;
    }
}
