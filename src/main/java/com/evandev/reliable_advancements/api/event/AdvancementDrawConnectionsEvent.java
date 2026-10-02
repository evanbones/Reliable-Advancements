package com.evandev.reliable_advancements.api.event;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;

import java.util.ArrayList;
import java.util.List;

//? if neoforge {
import net.neoforged.bus.api.Event;
//?}

/**
 * Event fired during advancement connection drawing.
 * <p>
 * Used for adding extra connection lines. Posted on the NeoForge event bus; not posted on Fabric.
 */
//? if neoforge {
public class AdvancementDrawConnectionsEvent extends Event implements IAdvancementDrawConnectionsEvent {
//?} else {
/*public class AdvancementDrawConnectionsEvent implements IAdvancementDrawConnectionsEvent {
*///?}
    /**
     * Advancement having its connection lines drawn.
     */
    private final AdvancementNode advancement;
    /**
     * Extra connections to draw lines to.
     */
    private final List<AdvancementHolder> extraConnections;

    public AdvancementDrawConnectionsEvent(AdvancementNode advancement) {
        this.advancement = advancement;
        this.extraConnections = new ArrayList<>();
    }

    public AdvancementNode getAdvancement() {
        return this.advancement;
    }

    public List<AdvancementHolder> getExtraConnections() {
        return this.extraConnections;
    }
}
