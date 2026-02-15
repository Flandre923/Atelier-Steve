package com.ateliersteve.alchemy.trait;

import com.ateliersteve.AtelierSteve;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Registry for all alchemy traits.
 * Traits are registered here and can be looked up by ID.
 */
public class TraitRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraitRegistry.class);
    private static final Map<ResourceLocation, TraitDefinition> TRAITS = new HashMap<>();
    private static final Map<Integer, List<TraitDefinition>> TRAITS_BY_GRADE = new HashMap<>();

    /**
     * Register a trait definition.
     */
    public static void register(TraitDefinition trait) {
        if (TRAITS.containsKey(trait.getId())) {
            LOGGER.warn("Trait {} is already registered, overwriting", trait.getId());
        }
        TRAITS.put(trait.getId(), trait);
        TRAITS_BY_GRADE.computeIfAbsent(trait.getGrade(), k -> new ArrayList<>()).add(trait);
        LOGGER.debug("Registered trait: {} (Grade {})", trait.getId(), trait.getGrade());
    }

    /**
     * Get a trait by its ID.
     */
    public static TraitDefinition get(ResourceLocation id) {
        return TRAITS.get(id);
    }

    /**
     * Get all traits with a specific grade.
     */
    public static List<TraitDefinition> getByGrade(int grade) {
        return TRAITS_BY_GRADE.getOrDefault(grade, List.of());
    }

    /**
     * Get all registered traits.
     */
    public static Collection<TraitDefinition> getAll() {
        return Collections.unmodifiableCollection(TRAITS.values());
    }

    /**
     * Check if a trait is registered.
     */
    public static boolean has(ResourceLocation id) {
        return TRAITS.containsKey(id);
    }

    /**
     * Clear all registered traits (for testing/reloading).
     */
    public static void clear() {
        TRAITS.clear();
        TRAITS_BY_GRADE.clear();
    }

    /**
     * Initialize and register all built-in traits.
     * This should be called during mod initialization.
     */
    public static void init() {
        LOGGER.info("Initializing trait registry...");
        // Traits will be loaded from data files via TraitDataLoader
        // during resource reload
        LOGGER.info("Trait registry initialized");
    }
}
