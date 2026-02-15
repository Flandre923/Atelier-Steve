package com.ateliersteve.event;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.ingredient.AlchemyIngredientReloadListener;
import com.ateliersteve.alchemy.trait.TraitReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Event handler for registering resource reload listeners.
 */
@EventBusSubscriber(modid = AtelierSteve.MODID)
public class ResourceReloadHandler {

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new TraitReloadListener());
        event.addListener(new AlchemyIngredientReloadListener());
        AtelierSteve.LOGGER.info("Registered trait reload listener");
        AtelierSteve.LOGGER.info("Registered alchemy ingredient reload listener");
    }
}
