package com.ateliersteve.alchemy.recipe;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlchemyRecipeReloadListener extends SimplePreparableReloadListener<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlchemyRecipeReloadListener.class);

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Reloading alchemy recipe data...");

        AlchemyRecipeRegistry.clear();
        AlchemyRecipeDataLoader.load(resourceManager);

        LOGGER.info("Alchemy recipe data reloaded: {} definitions registered", AlchemyRecipeRegistry.getAll().size());
    }
}
