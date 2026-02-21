package com.ateliersteve.alchemy.recipe;

import com.ateliersteve.alchemy.AlchemyItemData;
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
        return resolveEffects(elementValues, Map.of());
    }

    public List<ResolvedEffect> resolveEffects(
            Map<String, Integer> elementValues,
            Map<String, Integer> chainCounts
    ) {
        List<ResolvedEffect> resolved = new ArrayList<>();
        for (EffectGroup group : effects) {
            int chainCount = chainCounts == null ? 0 : chainCounts.getOrDefault(group.type(), 0);
            int unlockedCap = group.resolveUnlockedCap(chainCount);
            int value = Math.min(elementValues.getOrDefault(group.type(), 0), unlockedCap);
            EffectStep step = group.selectStep(value);
            if (step != null) {
                resolved.add(new ResolvedEffect(group.category(), step.value(), step.bonusElements(), step.grantCategories()));
            }
        }
        return resolved;
    }

    public AlchemyItemData applyResolvedEffects(AlchemyItemData baseData, Map<String, Integer> elementValues) {
        if (baseData == null) {
            return AlchemyItemData.empty();
        }
        List<ElementComponent> mergedElements = new ArrayList<>(baseData.elements());
        List<ResourceLocation> mergedCategories = new ArrayList<>(baseData.categories());
        for (ResolvedEffect effect : resolveEffects(elementValues)) {
            mergedElements.addAll(effect.bonusElements());
            for (ResourceLocation category : effect.grantCategories()) {
                if (!mergedCategories.contains(category)) {
                    mergedCategories.add(category);
                }
            }
        }
        return new AlchemyItemData(baseData.traits(), mergedElements, baseData.cole(), baseData.quality(), mergedCategories);
    }

    public record RecipeGrid(int width, int height, List<RecipeCell> cells) {
    }

    public record RecipeCell(int x, int y, String type, String color) {
    }

    public record UnlockCondition(AlchemyRecipeIngredient.Type type, ResourceLocation id, int count) {
    }

    public record EffectGroup(String type, String category, int lockedSlots, List<EffectStep> steps) {
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

        public int maxThreshold() {
            int max = 0;
            for (EffectStep step : steps) {
                if (step.threshold() > max) {
                    max = step.threshold();
                }
            }
            return max;
        }

        public int resolveUnlockedCap(int chainCount) {
            int max = maxThreshold();
            if (max <= 0) {
                return 0;
            }
            int effectiveLockedSlots = Math.max(0, Math.min(lockedSlots, max));
            int remainingLocked = Math.max(0, effectiveLockedSlots - Math.max(0, chainCount));
            return Math.max(0, max - remainingLocked);
        }
    }

    public record EffectStep(int threshold, String value, List<ElementComponent> bonusElements, List<ResourceLocation> grantCategories) {
    }

    public record ResolvedEffect(String category, String value, List<ElementComponent> bonusElements, List<ResourceLocation> grantCategories) {
    }
}
