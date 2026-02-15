package com.ateliersteve.alchemy.ingredient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class AlchemyIngredientReloadListener extends SimplePreparableReloadListener<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlchemyIngredientReloadListener.class);

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Reloading alchemy ingredient data...");

        AlchemyIngredientRegistry.clear();
        AlchemyIngredientDataLoader.load(resourceManager);

        LOGGER.info("Alchemy ingredient data reloaded: {} definitions registered", AlchemyIngredientRegistry.getAll().size());
    }
}
