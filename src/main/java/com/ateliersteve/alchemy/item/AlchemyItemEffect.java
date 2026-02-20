package com.ateliersteve.alchemy.item;

import com.ateliersteve.alchemy.element.ElementComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record AlchemyItemEffect(String name, List<ElementComponent> bonusElements, List<ResourceLocation> grantCategories) {
    public static AlchemyItemEffect simple(String name) {
        return new AlchemyItemEffect(name, List.of(), List.of());
    }

    public static AlchemyItemEffect grantCategory(String name, ResourceLocation categoryId) {
        return new AlchemyItemEffect(name, List.of(), List.of(categoryId));
    }
}
