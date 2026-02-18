package com.ateliersteve.alchemy.recipe;

import com.ateliersteve.alchemy.element.ElementComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record AlchemyRecipeDefinition(
        ResourceLocation id,
        ResourceLocation result,
        RecipeGrid grid,
        List<AlchemyRecipeIngredient> ingredients,
        UnlockCondition unlockCondition,
        List<EffectGroup> effects
) {
    public List<ResolvedEffect> resolveEffects(Map<String, Integer> elementValues) {
        List<ResolvedEffect> resolved = new ArrayList<>();
        for (EffectGroup group : effects) {
            int value = elementValues.getOrDefault(group.type(), 0);
            EffectStep step = group.selectStep(value);
            if (step != null) {
                resolved.add(new ResolvedEffect(group.category(), step.value(), step.bonusElements()));
            }
        }
        return resolved;
    }

    public record RecipeGrid(int width, int height, List<RecipeCell> cells) {
    }

    public record RecipeCell(int x, int y, String type, String color) {
    }

    public record UnlockCondition(AlchemyRecipeIngredient.Type type, ResourceLocation id, int count) {
    }

    public record EffectGroup(String type, String category, List<EffectStep> steps) {
        public EffectStep selectStep(int value) {
            EffectStep best = null;
            for (EffectStep step : steps) {
                if (value < step.threshold()) {
                    continue;
                }
                if (best == null || step.threshold() > best.threshold()) {
                    best = step;
                }
            }
            return best;
        }
    }

    public record EffectStep(int threshold, String value, List<ElementComponent> bonusElements) {
    }

    public record ResolvedEffect(String category, String value, List<ElementComponent> bonusElements) {
    }
}
