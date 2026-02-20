package com.ateliersteve.alchemy.category;

import com.ateliersteve.alchemy.AlchemyItemData;
import com.ateliersteve.registry.ModDataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AlchemyCategoryMatcher {
    private AlchemyCategoryMatcher() {
    }

    public static boolean hasCategory(ItemStack stack, TagKey<Item> categoryTag) {
        if (stack == null || stack.isEmpty() || categoryTag == null) {
            return false;
        }
        if (stack.is(categoryTag)) {
            return true;
        }
        AlchemyItemData data = stack.get(ModDataComponents.ALCHEMY_DATA.get());
        return data != null && data.hasCategory(categoryTag.location());
    }
}
