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
        List<TraitInstance> traits = generateRandomTraits(random, 1, 3);
        List<ElementComponent> elements = generateRandomElements(random, ElementShapePresets.ALL_PRESETS, 1, 3);
        return new AlchemyItemData(traits, elements, 0);
    }

    /**
     * Creates random alchemy data for gathered wheat.
     * - Traits: 0 to 3
     * - Elements: 1 to 2 (from wheat-specific presets)
     * - Cole: fixed to 1
     */
    public static AlchemyItemData createRandomForWheat(RandomSource random) {
        List<TraitInstance> traits = generateRandomTraits(random, 0, 3);
        List<ElementComponent> elements = generateRandomElements(random, ElementShapePresets.WHEAT_PRESETS, 1, 2);
        return new AlchemyItemData(traits, elements, 1);
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

    private static List<ElementComponent> generateRandomElements(
            RandomSource random,
            List<ElementShapePresets.Preset> presetPool,
            int min,
            int max
    ) {
        if (presetPool.isEmpty() || max <= 0) {
            return List.of();
        }

        List<ElementShapePresets.Preset> presets = new ArrayList<>(presetPool);
        Collections.shuffle(presets, new java.util.Random(random.nextLong()));

        int upperBound = Math.min(max, presets.size());
        int lowerBound = Math.min(min, upperBound);
        int count = lowerBound + random.nextInt(upperBound - lowerBound + 1);

        List<ElementComponent> elements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            elements.add(ElementComponent.fromPreset(presets.get(i)));
        }
        return elements;
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
