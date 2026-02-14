package com.ateliersteve.alchemy.trait;

import com.ateliersteve.AtelierSteve;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Interface for alchemy traits that can be applied to items.
 */
public interface AlchemyTrait {
    /**
     * @return The unique identifier for this trait.
     */
    ResourceLocation getId();

    /**
     * @return The display name of this trait.
     */
    Component getName();

    /**
     * @return A description of what this trait does.
     */
    Component getDescription();

    /**
     * Apply this trait's effect to an entity.
     * Called when the item with this trait is used or equipped.
     *
     * @param entity The entity to apply the effect to.
     */
    void apply(LivingEntity entity);

    /**
     * Remove this trait's effect from an entity.
     * Called when the item with this trait is unequipped.
     *
     * @param entity The entity to remove the effect from.
     */
    void remove(LivingEntity entity);
}
