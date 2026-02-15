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
public record AlchemyItemData(List<TraitInstance> traits, List<ElementComponent> elements) {
    public static final Codec<AlchemyItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TraitInstance.CODEC.listOf().fieldOf("traits").forGetter(AlchemyItemData::traits),
            ElementComponent.CODEC.listOf().fieldOf("elements").forGetter(AlchemyItemData::elements)
    ).apply(instance, AlchemyItemData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyItemData> STREAM_CODEC = StreamCodec.composite(
            TraitInstance.STREAM_CODEC.apply(ByteBufCodecs.list()), AlchemyItemData::traits,
            ElementComponent.STREAM_CODEC.apply(ByteBufCodecs.list()), AlchemyItemData::elements,
            AlchemyItemData::new
    );

    /**
     * Creates random alchemy data.
     * Currently:
     * - Randomly picks 1-3 traits from the registered trait pool.
     * - Randomly picks 1-3 element components from the predefined presets.
     */
    public static AlchemyItemData createRandom(RandomSource random) {
        List<TraitInstance> traits = new ArrayList<>();

        List<TraitDefinition> availableTraits = new ArrayList<>(TraitRegistry.getAll());
        if (!availableTraits.isEmpty()) {
            Collections.shuffle(availableTraits, new java.util.Random(random.nextLong()));
            int traitCount = 1 + random.nextInt(3); // 1 to 3
            traitCount = Math.min(traitCount, availableTraits.size());
            for (int i = 0; i < traitCount; i++) {
                traits.add(new TraitInstance(availableTraits.get(i).getId()));
            }
            traits = new ArrayList<>(TraitCombinationEngine.resolveTraitCombinations(traits));
        }

        List<ElementShapePresets.Preset> presets = new ArrayList<>(ElementShapePresets.ALL_PRESETS);
        Collections.shuffle(presets, new java.util.Random(random.nextLong()));

        int count = 1 + random.nextInt(3); // 1 to 3
        count = Math.min(count, presets.size());

        List<ElementComponent> elements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            elements.add(ElementComponent.fromPreset(presets.get(i)));
        }

        return new AlchemyItemData(traits, elements);
    }

    /**
     * Creates empty alchemy data.
     */
    public static AlchemyItemData empty() {
        return new AlchemyItemData(List.of(), List.of());
    }

    public boolean isEmpty() {
        return traits.isEmpty() && elements.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlchemyItemData that = (AlchemyItemData) o;
        return Objects.equals(traits, that.traits) && Objects.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traits, elements);
    }
}
