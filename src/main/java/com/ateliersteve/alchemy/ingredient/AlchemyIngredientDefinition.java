package com.ateliersteve.alchemy.ingredient;

import com.ateliersteve.alchemy.element.ElementComponent;
import com.ateliersteve.alchemy.element.ElementShapePresets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record AlchemyIngredientDefinition(
        ResourceLocation id,
        Optional<ResourceLocation> itemId,
        Optional<TagKey<Item>> tag,
        int traitMin,
        int traitMax,
        int elementMin,
        int elementMax,
        int cole,
        int quality,
        List<ElementComponent> elementPresets,
        boolean useDefaultElementPresets,
        int priority
) {
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        boolean matched = false;
        if (itemId.isPresent()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!itemId.get().equals(key)) {
                return false;
            }
            matched = true;
        }

        if (tag.isPresent()) {
            if (!stack.is(tag.get())) {
                return false;
            }
            matched = true;
        }

        return matched;
    }

    public List<ElementComponent> getElementPresets() {
        return useDefaultElementPresets ? ElementShapePresets.ALL_COMPONENTS : elementPresets;
    }
}
