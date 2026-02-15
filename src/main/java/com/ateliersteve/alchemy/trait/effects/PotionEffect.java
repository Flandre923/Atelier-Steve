package com.ateliersteve.alchemy.trait.effects;

import com.ateliersteve.alchemy.trait.TraitEffect;
import com.ateliersteve.alchemy.trait.TraitEffectType;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Effect that applies a potion effect to the entity.
 */
public class PotionEffect implements TraitEffect {
    public static final TraitEffectType<PotionEffect> TYPE = new Type();

    private final Holder<MobEffect> effect;
    private final int amplifier;
    private final int duration;
    private final boolean ambient;
    private final boolean visible;

    public PotionEffect(Holder<MobEffect> effect, int amplifier, int duration, boolean ambient, boolean visible) {
        this.effect = effect;
        this.amplifier = amplifier;
        this.duration = duration;
        this.ambient = ambient;
        this.visible = visible;
    }

    @Override
    public TraitEffectType<?> getType() {
        return TYPE;
    }

    @Override
    public void apply(LivingEntity entity, ItemStack stack) {
        entity.addEffect(new MobEffectInstance(effect, duration, amplifier, ambient, visible));
    }

    @Override
    public void remove(LivingEntity entity, ItemStack stack) {
        entity.removeEffect(effect);
    }

    @Override
    public void tick(LivingEntity entity, ItemStack stack) {
        // Refresh the effect if it's about to expire
        var activeEffect = entity.getEffect(effect);
        if (activeEffect == null || activeEffect.getDuration() < 20) {
            apply(entity, stack);
        }
    }

    @Override
    public String getDescription() {
        return String.format("%s %d", effect.value().getDescriptionId(), amplifier + 1);
    }

    private static class Type implements TraitEffectType<PotionEffect> {
        @Override
        public String getId() {
            return "potion_effect";
        }

        @Override
        public PotionEffect create(Object... params) {
            if (params.length < 5) {
                throw new IllegalArgumentException("PotionEffect requires 5 parameters");
            }
            return new PotionEffect(
                    (Holder<MobEffect>) params[0],
                    (Integer) params[1],
                    (Integer) params[2],
                    (Boolean) params[3],
                    (Boolean) params[4]
            );
        }
    }
}
