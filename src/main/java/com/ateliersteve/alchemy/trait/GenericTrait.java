package com.ateliersteve.alchemy.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A generic trait implementation that uses TraitDefinition.
 * This allows traits to be data-driven.
 */
public class GenericTrait implements AlchemyTrait {
    private final TraitDefinition definition;

    public GenericTrait(TraitDefinition definition) {
        this.definition = definition;
    }

    @Override
    public ResourceLocation getId() {
        return definition.getId();
    }

    @Override
    public Component getName() {
        return definition.getName();
    }

    @Override
    public Component getDescription() {
        return definition.getDescription();
    }

    @Override
    public int getGrade() {
        return definition.getGrade();
    }

    @Override
    public void apply(LivingEntity entity, ItemStack stack) {
        for (TraitEffect effect : definition.getEffects()) {
            effect.apply(entity, stack);
        }
    }

    @Override
    public void remove(LivingEntity entity, ItemStack stack) {
        for (TraitEffect effect : definition.getEffects()) {
            effect.remove(entity, stack);
        }
    }

    @Override
    public void tick(LivingEntity entity, ItemStack stack) {
        for (TraitEffect effect : definition.getEffects()) {
            effect.tick(entity, stack);
        }
    }

    @Override
    public TraitDefinition getDefinition() {
        return definition;
    }

    /**
     * Create a GenericTrait from a TraitDefinition.
     */
    public static GenericTrait from(TraitDefinition definition) {
        return new GenericTrait(definition);
    }

    /**
     * Create a GenericTrait from a trait ID.
     */
    public static GenericTrait from(ResourceLocation id) {
        TraitDefinition definition = TraitRegistry.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown trait: " + id);
        }
        return new GenericTrait(definition);
    }
}
