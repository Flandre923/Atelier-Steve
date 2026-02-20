package com.ateliersteve.alchemy.category;

import net.minecraft.resources.ResourceLocation;

public record AlchemyCategoryDefinition(
        ResourceLocation id,
        ResourceLocation icon,
        String translationKey
) {
}
