package com.ateliersteve.alchemy.trait;

/**
 * Registry for trait effect types.
 * This allows for extensible effect system.
 */
public interface TraitEffectType<T extends TraitEffect> {
    /**
     * Get the unique identifier for this effect type.
     */
    String getId();

    /**
     * Create an instance of this effect from parameters.
     */
    T create(Object... params);
}
