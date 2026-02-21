package com.ateliersteve.alchemy.recipe;

import com.ateliersteve.alchemy.element.ElementComponent;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AlchemyRecipeDataLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlchemyRecipeDataLoader.class);
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "alchemy/recipes";

    public static void load(ResourceManager resourceManager) {
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                DIRECTORY,
                path -> path.getPath().endsWith(".json")
        );

        int count = 0;
        for (var entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                AlchemyRecipeDefinition definition = parseDefinition(fileId, root);
                if (definition != null) {
                    AlchemyRecipeRegistry.register(definition);
                    count++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load alchemy recipe: {}", fileId, e);
            }
        }

        LOGGER.info("Loaded {} alchemy recipe definitions", count);
    }

    private static AlchemyRecipeDefinition parseDefinition(ResourceLocation fileId, JsonObject root) {
        ResourceLocation result = parseId(root, "result", fileId, "result");
        if (result == null) {
            return null;
        }

        JsonObject gridObj = root.has("grid_size") ? root.getAsJsonObject("grid_size") : null;
        int width = readInt(gridObj, "width", 0);
        int height = readInt(gridObj, "height", 0);
        List<AlchemyRecipeDefinition.RecipeCell> cells = new ArrayList<>();
        JsonArray cellsArray = root.has("cells") ? root.getAsJsonArray("cells") : new JsonArray();
        for (JsonElement element : cellsArray) {
            JsonObject cellObj = element.getAsJsonObject();
            int x = readInt(cellObj, "x", 0);
            int y = readInt(cellObj, "y", 0);
            String type = readString(cellObj, "type", "empty");
            String color = readString(cellObj, "color", "");
            cells.add(new AlchemyRecipeDefinition.RecipeCell(x, y, type, color));
        }
        AlchemyRecipeDefinition.RecipeGrid grid = new AlchemyRecipeDefinition.RecipeGrid(width, height, cells);

        List<AlchemyRecipeIngredient> ingredients = parseIngredients(fileId, root);
        AlchemyRecipeDefinition.UnlockCondition unlockCondition = parseUnlockCondition(fileId, root);
        List<AlchemyRecipeDefinition.EffectGroup> effects = parseEffects(fileId, root);

        return new AlchemyRecipeDefinition(fileId, result, grid, ingredients, unlockCondition, effects);
    }

    private static List<AlchemyRecipeIngredient> parseIngredients(ResourceLocation fileId, JsonObject root) {
        List<AlchemyRecipeIngredient> ingredients = new ArrayList<>();
        JsonArray array = root.has("ingredients") ? root.getAsJsonArray("ingredients") : new JsonArray();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            String typeName = readString(obj, "type", "");
            int count = readInt(obj, "count", 1);
            if ("specific".equalsIgnoreCase(typeName)) {
                ResourceLocation itemId = parseId(obj, "name", fileId, "ingredients.name");
                if (itemId != null) {
                    ingredients.add(new AlchemyRecipeIngredient(
                            AlchemyRecipeIngredient.Type.SPECIFIC,
                            Optional.of(itemId),
                            Optional.empty(),
                            count
                    ));
                }
                continue;
            }
            if ("tag".equalsIgnoreCase(typeName)) {
                ResourceLocation tagId = parseId(obj, "tag", fileId, "ingredients.tag");
                if (tagId != null) {
                    ingredients.add(new AlchemyRecipeIngredient(
                            AlchemyRecipeIngredient.Type.TAG,
                            Optional.empty(),
                            Optional.of(TagKey.create(Registries.ITEM, tagId)),
                            count
                    ));
                }
                continue;
            }
            LOGGER.warn("Unknown ingredient type '{}' in {}", typeName, fileId);
        }
        return ingredients;
    }

    private static AlchemyRecipeDefinition.UnlockCondition parseUnlockCondition(ResourceLocation fileId, JsonObject root) {
        if (!root.has("unlock_condition")) {
            return null;
        }
        JsonObject obj = root.getAsJsonObject("unlock_condition");
        int count = readInt(obj, "count", 1);
        if (obj.has("item")) {
            ResourceLocation itemId = parseId(obj, "item", fileId, "unlock_condition.item");
            if (itemId != null) {
                return new AlchemyRecipeDefinition.UnlockCondition(AlchemyRecipeIngredient.Type.SPECIFIC, itemId, count);
            }
        }
        if (obj.has("tag")) {
            ResourceLocation tagId = parseId(obj, "tag", fileId, "unlock_condition.tag");
            if (tagId != null) {
                return new AlchemyRecipeDefinition.UnlockCondition(AlchemyRecipeIngredient.Type.TAG, tagId, count);
            }
        }
        return null;
    }

    private static List<AlchemyRecipeDefinition.EffectGroup> parseEffects(ResourceLocation fileId, JsonObject root) {
        List<AlchemyRecipeDefinition.EffectGroup> groups = new ArrayList<>();
        JsonArray array = root.has("effects") ? root.getAsJsonArray("effects") : new JsonArray();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            String type = readString(obj, "type", "");
            String category = readString(obj, "category", type);
            int lockedSlots = Math.max(0, readInt(obj, "locked_slots", 0));
            JsonArray stepsArray = obj.has("steps") ? obj.getAsJsonArray("steps") : new JsonArray();
            List<AlchemyRecipeDefinition.EffectStep> steps = new ArrayList<>();
            for (JsonElement stepElement : stepsArray) {
                JsonObject stepObj = stepElement.getAsJsonObject();
                int threshold = readInt(stepObj, "threshold", 0);
                String value = readString(stepObj, "value", "");
                List<ElementComponent> bonus = parseBonusElements(fileId, stepObj);
                List<ResourceLocation> grantCategories = parseGrantCategories(fileId, stepObj);
                steps.add(new AlchemyRecipeDefinition.EffectStep(threshold, value, bonus, grantCategories));
            }
            groups.add(new AlchemyRecipeDefinition.EffectGroup(type, category, lockedSlots, steps));
        }
        return groups;
    }

    private static List<ElementComponent> parseBonusElements(ResourceLocation fileId, JsonObject stepObj) {
        if (!stepObj.has("bonus_elements")) {
            return List.of();
        }
        List<ElementComponent> bonus = new ArrayList<>();
        JsonArray array = stepObj.getAsJsonArray("bonus_elements");
        for (JsonElement element : array) {
            ElementComponent.CODEC.parse(JsonOps.INSTANCE, element)
                    .resultOrPartial(message -> LOGGER.error(
                            "Failed to parse bonus element in {}: {}",
                            fileId,
                            message
                    ))
                    .ifPresent(bonus::add);
        }
        return bonus;
    }

    private static List<ResourceLocation> parseGrantCategories(ResourceLocation fileId, JsonObject stepObj) {
        if (!stepObj.has("grant_categories")) {
            return List.of();
        }
        List<ResourceLocation> categories = new ArrayList<>();
        JsonArray array = stepObj.getAsJsonArray("grant_categories");
        for (JsonElement element : array) {
            ResourceLocation categoryId = ResourceLocation.tryParse(element.getAsString());
            if (categoryId == null) {
                LOGGER.warn("Invalid grant category '{}' in {}", element, fileId);
                continue;
            }
            categories.add(categoryId);
        }
        return categories;
    }

    private static ResourceLocation parseId(JsonObject obj, String key, ResourceLocation fileId, String label) {
        if (!obj.has(key)) {
            return null;
        }
        String idString = obj.get(key).getAsString();
        ResourceLocation parsed = ResourceLocation.tryParse(idString);
        if (parsed == null) {
            LOGGER.warn("Invalid {} '{}' in {}", label, idString, fileId);
            return null;
        }
        return parsed;
    }

    private static int readInt(JsonObject obj, String key, int defaultValue) {
        if (obj == null || !obj.has(key)) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String readString(JsonObject obj, String key, String defaultValue) {
        if (obj == null || !obj.has(key)) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
