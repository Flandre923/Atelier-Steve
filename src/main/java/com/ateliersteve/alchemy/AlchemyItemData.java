package com.ateliersteve.alchemy;

import com.ateliersteve.alchemy.element.ElementComponent;
import com.ateliersteve.alchemy.element.ElementShapePresets;
import com.ateliersteve.alchemy.trait.TraitCombinationEngine;
import com.ateliersteve.alchemy.trait.TraitDefinition;
import com.ateliersteve.alchemy.trait.TraitInstance;
import com.ateliersteve.alchemy.trait.TraitRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Data component that stores alchemy data for an item.
 * Contains traits and element components.
 */
public record AlchemyItemData(List<TraitInstance> traits, List<ElementComponent> elements, int cole, int quality, List<ResourceLocation> categories) {
    public static final Codec<AlchemyItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TraitInstance.CODEC.listOf().fieldOf("traits").forGetter(AlchemyItemData::traits),
            ElementComponent.CODEC.listOf().fieldOf("elements").forGetter(AlchemyItemData::elements),
            Codec.INT.optionalFieldOf("cole", 0).forGetter(AlchemyItemData::cole),
            Codec.INT.optionalFieldOf("quality", 0).forGetter(AlchemyItemData::quality),
            ResourceLocation.CODEC.listOf().optionalFieldOf("categories", List.of()).forGetter(AlchemyItemData::categories)
    ).apply(instance, AlchemyItemData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyItemData> STREAM_CODEC = StreamCodec.composite(
            TraitInstance.STREAM_CODEC.apply(ByteBufCodecs.list()), AlchemyItemData::traits,
            ElementComponent.STREAM_CODEC.apply(ByteBufCodecs.list()), AlchemyItemData::elements,
            ByteBufCodecs.INT, AlchemyItemData::cole,
            ByteBufCodecs.INT, AlchemyItemData::quality,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), AlchemyItemData::categories,
            AlchemyItemData::new
    );

    public AlchemyItemData {
        traits = traits == null ? List.of() : List.copyOf(traits);
        elements = elements == null ? List.of() : List.copyOf(elements);
        categories = categories == null ? List.of() : List.copyOf(categories);
    }

    /**
     * Creates random alchemy data.
     * Currently:
     * - Randomly picks 1-3 traits from the registered trait pool.
     * - Randomly picks 1-3 element components from the predefined presets.
     */
    public static AlchemyItemData createRandom(RandomSource random) {
        return createRandom(random, 1, 3, ElementShapePresets.ALL_COMPONENTS, 1, 3, 0, 0);
    }

    /**
     * Creates random alchemy data using explicit ranges and preset pool.
     */
    public static AlchemyItemData createRandom(
            RandomSource random,
            int traitMin,
            int traitMax,
            List<ElementComponent> elementPresetPool,
            int elementMin,
            int elementMax,
            int cole,
            int quality
    ) {
        List<TraitInstance> traits = generateRandomTraits(random, traitMin, traitMax);
        List<ElementComponent> elements = generateRandomElementsFromComponents(random, elementPresetPool, elementMin, elementMax);
        return new AlchemyItemData(traits, elements, cole, quality, List.of());
    }

    /**
     * Creates empty alchemy data.
     */
    public static AlchemyItemData empty() {
        return new AlchemyItemData(List.of(), List.of(), 0, 0, List.of());
    }

    public boolean isEmpty() {
        return traits.isEmpty() && elements.isEmpty() && cole == 0 && quality == 0 && categories.isEmpty();
    }

    public boolean hasCategory(ResourceLocation categoryId) {
        return categoryId != null && categories.contains(categoryId);
    }

    public AlchemyItemData withAdditionalCategories(Collection<ResourceLocation> extraCategories) {
        if (extraCategories == null || extraCategories.isEmpty()) {
            return this;
        }
        List<ResourceLocation> merged = new ArrayList<>(categories);
        for (ResourceLocation extraCategory : extraCategories) {
            if (extraCategory != null && !merged.contains(extraCategory)) {
                merged.add(extraCategory);
            }
        }
        return new AlchemyItemData(traits, elements, cole, quality, merged);
    }

    private static List<TraitInstance> generateRandomTraits(RandomSource random, int min, int max) {
        List<TraitDefinition> availableTraits = new ArrayList<>(TraitRegistry.getAll());
        if (availableTraits.isEmpty() || max <= 0) {
            return List.of();
        }

        Collections.shuffle(availableTraits, new java.util.Random(random.nextLong()));
        int upperBound = Math.min(max, availableTraits.size());
        int lowerBound = Math.min(min, upperBound);
        int traitCount = lowerBound + random.nextInt(upperBound - lowerBound + 1);

        List<TraitInstance> picked = new ArrayList<>();
        for (int i = 0; i < traitCount; i++) {
            picked.add(new TraitInstance(availableTraits.get(i).getId()));
        }
        return new ArrayList<>(TraitCombinationEngine.resolveTraitCombinations(picked));
    }

    private static List<ElementComponent> generateRandomElementsFromComponents(
            RandomSource random,
            List<ElementComponent> presetPool,
            int min,
            int max
    ) {
        if (presetPool == null || presetPool.isEmpty() || max <= 0) {
            return List.of();
        }

        List<ElementComponent> presets = new ArrayList<>(presetPool);
        Collections.shuffle(presets, new java.util.Random(random.nextLong()));

        int upperBound = Math.min(max, presets.size());
        int lowerBound = Math.min(min, upperBound);
        int count = lowerBound + random.nextInt(upperBound - lowerBound + 1);

        return new ArrayList<>(presets.subList(0, count));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlchemyItemData that = (AlchemyItemData) o;
        return cole == that.cole
                && quality == that.quality
                && Objects.equals(traits, that.traits)
                && Objects.equals(elements, that.elements)
                && Objects.equals(categories, that.categories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traits, elements, cole, quality, categories);
    }
}
