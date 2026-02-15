package com.ateliersteve.alchemy.trait;

import com.ateliersteve.AtelierSteve;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads trait definitions from JSON data files.
 */
public class TraitDataLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraitDataLoader.class);
    private static final Gson GSON = new Gson();

    /**
     * Load all traits from the data file.
     */
    public static void loadTraits(ResourceManager resourceManager) {
        try {
            ResourceLocation dataLocation = AtelierSteve.id("traits/traits.json");
            Optional<Resource> resource = resourceManager.getResource(dataLocation);

            if (resource.isEmpty()) {
                LOGGER.warn("Traits data file not found: {}", dataLocation);
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {

                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                JsonArray traits = root.getAsJsonArray("traits");

                int count = 0;
                for (var element : traits) {
                    JsonObject traitJson = element.getAsJsonObject();
                    try {
                        TraitDefinition trait = parseTraitDefinition(traitJson);
                        TraitRegistry.register(trait);
                        count++;
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse trait: {}", traitJson, e);
                    }
                }

                LOGGER.info("Loaded {} traits from data file", count);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load traits data", e);
        }
    }

    private static TraitDefinition parseTraitDefinition(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.get("name").getAsString();
        int grade = json.get("grade").getAsInt();
        String description = json.get("description").getAsString();

        // Parse inheritable categories
        Set<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>> categories = new HashSet<>();
        if (json.has("categories")) {
            JsonArray categoriesArray = json.getAsJsonArray("categories");
            for (var catElement : categoriesArray) {
                String catCode = catElement.getAsString();
                var category = ItemCategory.fromCode(catCode);
                if (category != null) {
                    categories.add(category);
                }
            }
        }

        // For now, effects are empty - they can be added later
        List<TraitEffect> effects = new ArrayList<>();

        List<TraitCombinationRecipe> combinations = new ArrayList<>();
        if (json.has("combinations")) {
            JsonArray combinationsArray = json.getAsJsonArray("combinations");
            for (var comboElement : combinationsArray) {
                JsonObject comboJson = comboElement.getAsJsonObject();
                if (!comboJson.has("ingredients")) {
                    continue;
                }
                JsonArray ingredientsArray = comboJson.getAsJsonArray("ingredients");
                List<ResourceLocation> ingredients = new ArrayList<>();
                for (var ingredientElement : ingredientsArray) {
                    String ingredientId = ingredientElement.getAsString();
                    ingredients.add(AtelierSteve.id(ingredientId));
                }
                if (!ingredients.isEmpty()) {
                    combinations.add(new TraitCombinationRecipe(ingredients));
                }
            }
        }

        return new TraitDefinition.Builder()
                .id(AtelierSteve.id(id))
                .nameKey("trait.atelier_steve." + id)
                .grade(grade)
                .descriptionKey("trait.atelier_steve." + id + ".desc")
                .inheritableCategories(categories)
                .effects(effects)
                .combinations(combinations)
                .build();
    }
}
