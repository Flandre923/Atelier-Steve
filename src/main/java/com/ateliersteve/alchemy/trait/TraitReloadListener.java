package com.ateliersteve.alchemy.trait;

import com.ateliersteve.AtelierSteve;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource reload listener for trait data.
 * Loads trait definitions from JSON files when resources are reloaded.
 */
public class TraitReloadListener extends SimplePreparableReloadListener<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraitReloadListener.class);

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        // Preparation phase - can be done on background thread
        return null;
    }

    @Override
    protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        // Application phase - must be done on main thread
        LOGGER.info("Reloading trait data...");

        // Clear existing traits
        TraitRegistry.clear();

        // Load traits from data files
        TraitDataLoader.loadTraits(resourceManager);

        LOGGER.info("Trait data reloaded: {} traits registered", TraitRegistry.getAll().size());
    }
}
