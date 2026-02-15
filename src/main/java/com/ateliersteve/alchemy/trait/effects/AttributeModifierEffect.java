package com.ateliersteve.alchemy.trait.effects;

import com.ateliersteve.alchemy.trait.TraitEffect;
import com.ateliersteve.alchemy.trait.TraitEffectType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * Effect that modifies an entity attribute (HP, attack, defense, etc.)
 */
public class AttributeModifierEffect implements TraitEffect {
    public static final TraitEffectType<AttributeModifierEffect> TYPE = new Type();

    private final Holder<Attribute> attribute;
    private final double amount;
    private final AttributeModifier.Operation operation;
    private final ResourceLocation modifierId;

    public AttributeModifierEffect(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, ResourceLocation modifierId) {
        this.attribute = attribute;
        this.amount = amount;
        this.operation = operation;
        this.modifierId = modifierId;
    }

    @Override
    public TraitEffectType<?> getType() {
        return TYPE;
    }

    @Override
    public void apply(LivingEntity entity, ItemStack stack) {
        var attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null && !attributeInstance.hasModifier(modifierId)) {
            attributeInstance.addPermanentModifier(new AttributeModifier(
                    modifierId,
                    amount,
                    operation
            ));
        }
    }

    @Override
    public void remove(LivingEntity entity, ItemStack stack) {
        var attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(modifierId);
        }
    }

    @Override
    public String getDescription() {
        String op = switch (operation) {
            case ADD_VALUE -> "+";
            case ADD_MULTIPLIED_BASE -> "+%";
            case ADD_MULTIPLIED_TOTAL -> "*";
        };
        return String.format("%s %s%.1f", attribute.value().getDescriptionId(), op, amount);
    }

    private static class Type implements TraitEffectType<AttributeModifierEffect> {
        @Override
        public String getId() {
            return "attribute_modifier";
        }

        @Override
        public AttributeModifierEffect create(Object... params) {
            if (params.length < 4) {
                throw new IllegalArgumentException("AttributeModifierEffect requires 4 parameters");
            }
            return new AttributeModifierEffect(
                    (Holder<Attribute>) params[0],
                    (Double) params[1],
                    (AttributeModifier.Operation) params[2],
                    (ResourceLocation) params[3]
            );
        }
    }
}
