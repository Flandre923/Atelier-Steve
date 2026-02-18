package com.ateliersteve.alchemy.recipe;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlchemyRecipeRegistry {
    private static final List<AlchemyRecipeDefinition> DEFINITIONS = new ArrayList<>();

    public static void clear() {
        DEFINITIONS.clear();
    }

    public static void register(AlchemyRecipeDefinition definition) {
        DEFINITIONS.add(definition);
    }

    public static List<AlchemyRecipeDefinition> getAll() {
        return Collections.unmodifiableList(DEFINITIONS);
    }

    public static AlchemyRecipeDefinition findById(ResourceLocation id) {
        for (AlchemyRecipeDefinition definition : DEFINITIONS) {
            if (definition.id().equals(id)) {
                return definition;
            }
        }
        return null;
    }
}
