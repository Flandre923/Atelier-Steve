package com.ateliersteve.alchemy.trait;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Represents an effect that a trait can apply.
 * This is designed to be extensible - new effect types can be added easily.
 */
public interface TraitEffect {
    /**
     * Get the type of this effect.
     */
    TraitEffectType<?> getType();

    /**
     * Apply this effect when the item is used or equipped.
     * @param entity The entity using/wearing the item
     * @param stack The item stack with this trait
     */
    void apply(LivingEntity entity, ItemStack stack);

    /**
     * Remove this effect when the item is unequipped.
     * @param entity The entity that was using/wearing the item
     * @param stack The item stack with this trait
     */
    void remove(LivingEntity entity, ItemStack stack);

    /**
     * Called every tick while the item is equipped/active.
     * @param entity The entity using/wearing the item
     * @param stack The item stack with this trait
     */
    default void tick(LivingEntity entity, ItemStack stack) {
        // Default: do nothing
    }

    /**
     * Get a description of this effect for tooltips.
     */
    String getDescription();
}
