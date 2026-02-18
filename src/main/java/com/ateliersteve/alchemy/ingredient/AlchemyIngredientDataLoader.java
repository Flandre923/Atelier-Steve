package com.ateliersteve.alchemy.ingredient;

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

public class AlchemyIngredientDataLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlchemyIngredientDataLoader.class);
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "alchemy/ingredients";

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
                AlchemyIngredientDefinition definition = parseDefinition(fileId, root);
                if (definition != null) {
                    AlchemyIngredientRegistry.register(definition);
                    count++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load alchemy ingredient: {}", fileId, e);
            }
        }

        LOGGER.info("Loaded {} alchemy ingredient definitions", count);
    }

    private static AlchemyIngredientDefinition parseDefinition(ResourceLocation fileId, JsonObject root) {
        JsonObject target = root.has("target") ? root.getAsJsonObject("target") : null;
        if (target == null) {
            LOGGER.warn("Missing target for alchemy ingredient: {}", fileId);
            return null;
        }

        Optional<ResourceLocation> itemId = Optional.empty();
        Optional<TagKey<Item>> tagKey = Optional.empty();

        if (target.has("item")) {
            String itemIdString = target.get("item").getAsString();
            ResourceLocation parsed = ResourceLocation.tryParse(itemIdString);
            if (parsed == null) {
                LOGGER.warn("Invalid item id '{}' in {}", itemIdString, fileId);
                return null;
            }
            itemId = Optional.of(parsed);
        }

        if (target.has("tag")) {
            String tagIdString = target.get("tag").getAsString();
            ResourceLocation parsed = ResourceLocation.tryParse(tagIdString);
            if (parsed == null) {
                LOGGER.warn("Invalid tag id '{}' in {}", tagIdString, fileId);
                return null;
            }
            tagKey = Optional.of(TagKey.create(Registries.ITEM, parsed));
        }

        if (itemId.isEmpty() && tagKey.isEmpty()) {
            LOGGER.warn("No item or tag specified for alchemy ingredient: {}", fileId);
            return null;
        }

        JsonObject traitsObj = root.has("traits") ? root.getAsJsonObject("traits") : null;
        int traitMin = readInt(traitsObj, "min", 1);
        int traitMax = readInt(traitsObj, "max", 3);

        JsonObject elementsObj = root.has("elements") ? root.getAsJsonObject("elements") : null;
        int elementMin = readInt(elementsObj, "min", 1);
        int elementMax = readInt(elementsObj, "max", 3);

        boolean useDefaultElementPresets = true;
        List<ElementComponent> elementPresets = List.of();
        if (elementsObj != null && elementsObj.has("presets")) {
            useDefaultElementPresets = false;
            elementPresets = parseElementPresets(fileId, elementsObj.getAsJsonArray("presets"));
        }

        int cole = readInt(root, "cole", 0);
        int quality = readInt(root, "quality", 0);
        int priority = readInt(root, "priority", 0);

        return new AlchemyIngredientDefinition(
                fileId,
                itemId,
                tagKey,
                traitMin,
                traitMax,
                elementMin,
                elementMax,
                cole,
                quality,
                elementPresets,
                useDefaultElementPresets,
                priority
        );
    }

    private static List<ElementComponent> parseElementPresets(ResourceLocation fileId, JsonArray presetsArray) {
        List<ElementComponent> presets = new ArrayList<>();
        for (JsonElement element : presetsArray) {
            ElementComponent.CODEC.parse(JsonOps.INSTANCE, element)
                    .resultOrPartial(message -> LOGGER.error(
                            "Failed to parse element preset in {}: {}",
                            fileId,
                            message
                    ))
                    .ifPresent(presets::add);
        }
        return presets;
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
}
