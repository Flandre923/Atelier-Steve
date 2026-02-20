package com.ateliersteve.alchemy.category;

import com.ateliersteve.AtelierSteve;
import net.minecraft.resources.ResourceLocation;

public final class AlchemyCategoryRegistry {
    private static final ResourceLocation DEFAULT_ICON = AtelierSteve.id("textures/gui/ingredients/category_water.png");

    private AlchemyCategoryRegistry() {
    }

    public static AlchemyCategoryDefinition resolve(ResourceLocation categoryId) {
        if (categoryId == null) {
            return null;
        }
        String suffix = normalizeSuffix(categoryId);
        ResourceLocation icon = AtelierSteve.id("textures/gui/ingredients/" + categoryId.getPath() + ".png");
        String translationKey = "ui.atelier_steve.alchemy_recipe.category." + suffix;
        return new AlchemyCategoryDefinition(categoryId, icon, translationKey);
    }

    public static ResourceLocation resolveIcon(ResourceLocation categoryId) {
        AlchemyCategoryDefinition definition = resolve(categoryId);
        return definition == null ? null : fallbackIcon(definition.icon());
    }

    public static String resolveTranslationKey(ResourceLocation categoryId) {
        AlchemyCategoryDefinition definition = resolve(categoryId);
        return definition == null ? "" : definition.translationKey();
    }

    private static String normalizeSuffix(ResourceLocation categoryId) {
        String path = categoryId.getPath();
        return path.startsWith("category_") ? path.substring("category_".length()) : path;
    }

    private static ResourceLocation fallbackIcon(ResourceLocation candidate) {
        if (candidate == null) {
            return DEFAULT_ICON;
        }
        String path = candidate.getPath();
        if ("textures/gui/ingredients/category_gunpowder.png".equals(path)
                || "textures/gui/ingredients/category_water.png".equals(path)) {
            return candidate;
        }
        return DEFAULT_ICON;
    }
}
