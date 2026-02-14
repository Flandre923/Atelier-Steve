package com.ateliersteve.alchemy.trait;

import com.ateliersteve.AtelierSteve;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * HP Boost (Small) trait - increases max health by 2 (1 heart).
 */
public class HpBoostTrait implements AlchemyTrait {
    public static final ResourceLocation ID = AtelierSteve.id("hp_boost_small");
    public static final HpBoostTrait INSTANCE = new HpBoostTrait();

    private static final ResourceLocation MODIFIER_ID = AtelierSteve.id("hp_boost_small_modifier");

    private HpBoostTrait() {}

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("trait.atelier_steve.hp_boost_small");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("trait.atelier_steve.hp_boost_small.desc");
    }

    @Override
    public void apply(LivingEntity entity) {
        var maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && !maxHealth.hasModifier(MODIFIER_ID)) {
            maxHealth.addPermanentModifier(new AttributeModifier(
                    MODIFIER_ID,
                    2.0, // +2 HP (1 heart)
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    @Override
    public void remove(LivingEntity entity) {
        var maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(MODIFIER_ID);
        }
    }
}
