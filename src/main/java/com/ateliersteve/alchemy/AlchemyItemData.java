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
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Data component that stores alchemy data for an item.
 * Contains traits and element components.
 */
public record AlchemyItemData(List<TraitInstance> traits, List<ElementComponent> elements, int cole) {
    public static final Codec<AlchemyItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TraitInstance.CODEC.listOf().fieldOf("traits").forGetter(AlchemyItemData::traits),
            ElementComponent.CODEC.listOf().fieldOf("elements").forGetter(AlchemyItemData::elements),
            Codec.INT.optionalFieldOf("cole", 0).forGetter(AlchemyItemData::cole)
    ).apply(instance, AlchemyItemData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyItemData> STREAM_CODEC = StreamCodec.composite(
            TraitInstance.STREAM_CODEC.apply(ByteBufCodecs.list()), AlchemyItemData::traits,
            ElementComponent.STREAM_CODEC.apply(ByteBufCodecs.list()), AlchemyItemData::elements,
            ByteBufCodecs.INT, AlchemyItemData::cole,
            AlchemyItemData::new
    );

    /**
     * Creates random alchemy data.
     * Currently:
     * - Randomly picks 1-3 traits from the registered trait pool.
     * - Randomly picks 1-3 element components from the predefined presets.
     */
    public static AlchemyItemData createRandom(RandomSource random) {
        return createRandom(random, 1, 3, ElementShapePresets.ALL_COMPONENTS, 1, 3, 0);
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
            int cole
    ) {
        List<TraitInstance> traits = generateRandomTraits(random, traitMin, traitMax);
        List<ElementComponent> elements = generateRandomElementsFromComponents(random, elementPresetPool, elementMin, elementMax);
        return new AlchemyItemData(traits, elements, cole);
    }

    /**
     * Creates empty alchemy data.
     */
    public static AlchemyItemData empty() {
        return new AlchemyItemData(List.of(), List.of(), 0);
    }

    public boolean isEmpty() {
        return traits.isEmpty() && elements.isEmpty();
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
        return cole == that.cole && Objects.equals(traits, that.traits) && Objects.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traits, elements, cole);
    }
}
