package com.ateliersteve.alchemy.trait;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class TraitCombinationRecipe {
    private final List<ResourceLocation> ingredients;

    public TraitCombinationRecipe(List<ResourceLocation> ingredients) {
        this.ingredients = List.copyOf(ingredients);
    }

    public List<ResourceLocation> getIngredients() {
        return ingredients;
    }
}
