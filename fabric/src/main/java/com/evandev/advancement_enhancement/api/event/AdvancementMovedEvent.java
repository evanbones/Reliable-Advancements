package com.evandev.advancement_enhancement.api.event;

import com.evandev.advancement_enhancement.api.IAdvancementEntryGui;
import net.minecraft.advancements.AdvancementNode;

/**
 * Event fired after an advancement has been moved in the gui and the mouse button has been released.
 */
public class AdvancementMovedEvent implements IAdvancementMovedEvent {
    /**
     * Advancement that has been moved.
     */
    private final AdvancementNode advancement;
    /*
     * Coordinates the advancement was moved to.
     */
    private final int x, y;

    public AdvancementMovedEvent(IAdvancementEntryGui gui) {
        this.advancement = gui.getAdvancement();
        this.x = gui.getX();
        this.y = gui.getY();
    }

    public AdvancementNode getAdvancement() {
        return this.advancement;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
}
