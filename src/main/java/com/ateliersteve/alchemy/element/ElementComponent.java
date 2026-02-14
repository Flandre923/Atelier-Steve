package com.ateliersteve.alchemy.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

/**
 * Represents an element component attached to an item.
 * Contains the element type and shape (which defines normal/link cell layout).
 */
public record ElementComponent(AlchemyElement element, ElementShape shape) {
    public static final Codec<ElementComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("element").forGetter(c -> c.element.getSerializedName()),
            ElementShape.CODEC.fieldOf("shape").forGetter(ElementComponent::shape)
    ).apply(instance, (elementName, shape) ->
            new ElementComponent(AlchemyElement.fromName(elementName), shape)));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElementComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, c -> c.element.getSerializedName(),
            ElementShape.STREAM_CODEC, ElementComponent::shape,
            (elementName, shape) -> new ElementComponent(AlchemyElement.fromName(elementName), shape)
    );

    /**
     * Creates an ElementComponent from a preset.
     */
    public static ElementComponent fromPreset(ElementShapePresets.Preset preset) {
        return new ElementComponent(preset.element(), preset.shape());
    }

    /**
     * @return The number of normal component cells in this shape.
     */
    public int getNormalCount() {
        return shape.getNormalCount();
    }

    /**
     * @return The number of link component cells in this shape.
     */
    public int getLinkCount() {
        return shape.getLinkCount();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElementComponent that = (ElementComponent) o;
        return element == that.element && Objects.equals(shape, that.shape);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, shape);
    }
}
