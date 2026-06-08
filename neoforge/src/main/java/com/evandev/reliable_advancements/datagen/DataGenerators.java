package com.evandev.reliable_advancements.datagen;

import com.evandev.reliable_advancements.reference.Constants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.getGenerator().addProvider(
                event.includeDev(),
                new TriggerSchemaProvider(event.getGenerator().getPackOutput())
        );
    }
}