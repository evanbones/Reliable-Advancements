package com.evandev.reliable_advancements.platform;

import com.evandev.reliable_advancements.api.IAdvancementEntryGui;
import com.evandev.reliable_advancements.api.event.AdvancementDrawConnectionsEvent;
import com.evandev.reliable_advancements.api.event.AdvancementMovedEvent;
import com.evandev.reliable_advancements.api.event.IAdvancementDrawConnectionsEvent;
import com.evandev.reliable_advancements.api.event.IAdvancementMovedEvent;
import com.evandev.reliable_advancements.platform.services.IEventHelper;
import net.minecraft.advancements.AdvancementNode;
import net.neoforged.neoforge.common.NeoForge;

public class NeoForgeEventHelper implements IEventHelper {
    @Override
    public IAdvancementMovedEvent postAdvancementMovementEvent(IAdvancementEntryGui gui) {
        final AdvancementMovedEvent event = new AdvancementMovedEvent(gui);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    @Override
    public IAdvancementDrawConnectionsEvent postAdvancementDrawConnectionsEvent(AdvancementNode advancement) {
        final AdvancementDrawConnectionsEvent event = new AdvancementDrawConnectionsEvent(advancement);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }
}
