package com.ateliersteve.alchemy.trait;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the application and removal of traits on entities.
 * This handles the lifecycle of trait effects.
 */
public class TraitManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraitManager.class);

    /**
     * Apply all traits from an item stack to an entity.
     * @param entity The entity to apply traits to
     * @param stack The item stack containing traits
     */
    public static void applyTraits(LivingEntity entity, ItemStack stack) {
        List<TraitInstance> traits = getTraitsFromStack(stack);
        for (TraitInstance traitInstance : traits) {
            try {
                AlchemyTrait trait = createTrait(traitInstance);
                if (trait != null) {
                    trait.apply(entity, stack);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to apply trait {}", traitInstance.traitId(), e);
            }
        }
    }

    /**
     * Remove all traits from an item stack from an entity.
     * @param entity The entity to remove traits from
     * @param stack The item stack containing traits
     */
    public static void removeTraits(LivingEntity entity, ItemStack stack) {
        List<TraitInstance> traits = getTraitsFromStack(stack);
        for (TraitInstance traitInstance : traits) {
            try {
                AlchemyTrait trait = createTrait(traitInstance);
                if (trait != null) {
                    trait.remove(entity, stack);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to remove trait {}", traitInstance.traitId(), e);
            }
        }
    }

    /**
     * Tick all traits from an item stack.
     * Should be called every tick while the item is equipped/active.
     * @param entity The entity with the item
     * @param stack The item stack containing traits
     */
    public static void tickTraits(LivingEntity entity, ItemStack stack) {
        List<TraitInstance> traits = getTraitsFromStack(stack);
        for (TraitInstance traitInstance : traits) {
            try {
                AlchemyTrait trait = createTrait(traitInstance);
                if (trait != null) {
                    trait.tick(entity, stack);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to tick trait {}", traitInstance.traitId(), e);
            }
        }
    }

    /**
     * Get all traits from an item stack.
     * This reads from the item's AlchemyItemData component.
     */
    private static List<TraitInstance> getTraitsFromStack(ItemStack stack) {
        // TODO: Implement reading traits from AlchemyItemData
        // For now, return empty list
        return new ArrayList<>();
    }

    /**
     * Create an AlchemyTrait instance from a TraitInstance.
     */
    private static AlchemyTrait createTrait(TraitInstance traitInstance) {
        // First check if there's a custom implementation
        AlchemyTrait customTrait = traitInstance.getTrait();
        if (customTrait != null) {
            return customTrait;
        }

        // Otherwise, create a generic trait from the definition
        TraitDefinition definition = TraitRegistry.get(traitInstance.traitId());
        if (definition != null) {
            return GenericTrait.from(definition);
        }

        LOGGER.warn("Unknown trait: {}", traitInstance.traitId());
        return null;
    }

    /**
     * Check if an item can inherit a specific trait based on its categories.
     * @param stack The item stack
     * @param trait The trait to check
     * @return true if the item can inherit this trait
     */
    public static boolean canInheritTrait(ItemStack stack, TraitDefinition trait) {
        // Check if the item matches any of the trait's inheritable categories
        for (var category : trait.getInheritableCategories()) {
            if (stack.is(category)) {
                return true;
            }
        }
        return false;
    }
}
