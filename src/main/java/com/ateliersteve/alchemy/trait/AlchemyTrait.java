package com.ateliersteve.alchemy.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for alchemy traits that can be applied to items.
 * This is now a wrapper around TraitDefinition for backward compatibility.
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
     * @return The grade/tier of this trait (1-70).
     */
    default int getGrade() {
        var definition = TraitRegistry.get(getId());
        return definition != null ? definition.getGrade() : 1;
    }

    /**
     * Apply this trait's effect to an entity.
     * Called when the item with this trait is used or equipped.
     *
     * @param entity The entity to apply the effect to.
     * @param stack The item stack with this trait.
     */
    void apply(LivingEntity entity, ItemStack stack);

    /**
     * Remove this trait's effect from an entity.
     * Called when the item with this trait is unequipped.
     *
     * @param entity The entity to remove the effect from.
     * @param stack The item stack with this trait.
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
     * Get the trait definition for this trait.
     */
    default TraitDefinition getDefinition() {
        return TraitRegistry.get(getId());
    }
}
