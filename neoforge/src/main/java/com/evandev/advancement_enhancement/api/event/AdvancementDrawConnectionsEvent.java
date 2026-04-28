package com.evandev.advancement_enhancement.api.event;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Event fired during advancement connection drawing.
 * <p>
 * Used for adding extra connection lines.
 */
public class AdvancementDrawConnectionsEvent extends Event implements IAdvancementDrawConnectionsEvent {
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
