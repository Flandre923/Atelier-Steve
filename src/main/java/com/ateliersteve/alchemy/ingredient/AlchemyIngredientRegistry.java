package com.ateliersteve.alchemy.ingredient;

import com.ateliersteve.alchemy.AlchemyItemData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlchemyIngredientRegistry {
    private static final int QUALITY_MAX = 999;
    private static final List<AlchemyIngredientDefinition> DEFINITIONS = new ArrayList<>();

    public interface QualityModifier {
        int adjustQuality(ItemStack stack, AlchemyIngredientDefinition definition, int baseQuality);
    }

    public static final QualityModifier NONE = (stack, definition, baseQuality) -> baseQuality;

    public static void clear() {
        DEFINITIONS.clear();
    }

    public static void register(AlchemyIngredientDefinition definition) {
        DEFINITIONS.add(definition);
    }

    public static List<AlchemyIngredientDefinition> getAll() {
        return Collections.unmodifiableList(DEFINITIONS);
    }

    public static AlchemyIngredientDefinition findMatching(ItemStack stack) {
        AlchemyIngredientDefinition best = null;
        int bestPriority = Integer.MIN_VALUE;
        for (AlchemyIngredientDefinition definition : DEFINITIONS) {
            if (!definition.matches(stack)) {
                continue;
            }
            if (definition.priority() > bestPriority) {
                best = definition;
                bestPriority = definition.priority();
            }
        }
        return best;
    }

    public static AlchemyItemData generateData(ItemStack stack, RandomSource random) {
        return generateData(stack, random, NONE);
    }

    public static AlchemyItemData generateData(ItemStack stack, RandomSource random, QualityModifier qualityModifier) {
        AlchemyIngredientDefinition definition = findMatching(stack);
        if (definition == null) {
            return AlchemyItemData.createRandom(random);
        }
        int baseQuality = random.nextInt(QUALITY_MAX) + 1;
        int adjustedQuality = qualityModifier == null
                ? baseQuality
                : qualityModifier.adjustQuality(stack, definition, baseQuality);
        return AlchemyItemData.createRandom(
                random,
                definition.traitMin(),
                definition.traitMax(),
                definition.getElementPresets(),
                definition.elementMin(),
                definition.elementMax(),
                definition.cole(),
                adjustedQuality
        );
    }
}
