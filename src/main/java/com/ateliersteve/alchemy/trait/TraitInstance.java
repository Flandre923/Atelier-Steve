package com.ateliersteve.alchemy.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A serializable reference to an alchemy trait.
 */
public record TraitInstance(ResourceLocation traitId) {
    private static final Map<ResourceLocation, AlchemyTrait> TRAIT_REGISTRY = new HashMap<>();

    static {
        // Register built-in traits
        registerTrait(HpBoostTrait.INSTANCE);
    }

    public static void registerTrait(AlchemyTrait trait) {
        TRAIT_REGISTRY.put(trait.getId(), trait);
    }

    public static AlchemyTrait getTrait(ResourceLocation id) {
        return TRAIT_REGISTRY.get(id);
    }

    public static final Codec<TraitInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("traitId").forGetter(TraitInstance::traitId)
    ).apply(instance, TraitInstance::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TraitInstance> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TraitInstance::traitId,
            TraitInstance::new
    );

    /**
     * Creates a default HP boost small trait instance.
     */
    public static TraitInstance createHpBoostSmall() {
        return new TraitInstance(HpBoostTrait.ID);
    }

    /**
     * Get the actual trait implementation.
     */
    public AlchemyTrait getTrait() {
        return getTrait(traitId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraitInstance that = (TraitInstance) o;
        return Objects.equals(traitId, that.traitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traitId);
    }
}
