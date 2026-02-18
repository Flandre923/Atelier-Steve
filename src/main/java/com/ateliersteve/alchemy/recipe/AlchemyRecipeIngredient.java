package com.ateliersteve.alchemy.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record AlchemyRecipeIngredient(Type type, Optional<ResourceLocation> itemId, Optional<TagKey<Item>> tag, int count) {
    public enum Type {
        SPECIFIC,
        TAG
    }
}
