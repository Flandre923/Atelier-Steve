package com.ateliersteve.alchemy.trait;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TraitCombinationEngine {
    private TraitCombinationEngine() {
    }

    public static List<TraitInstance> resolveTraitCombinations(List<TraitInstance> inputTraits) {
        if (inputTraits.isEmpty()) {
            return List.of();
        }

        Map<ResourceLocation, Integer> counts = new HashMap<>();
        for (TraitInstance trait : inputTraits) {
            counts.merge(trait.traitId(), 1, Integer::sum);
        }

        List<TraitDefinition> combinableTraits = TraitRegistry.getAll().stream()
                .filter(definition -> !definition.getCombinations().isEmpty())
                .sorted((a, b) -> {
                    int byGrade = Integer.compare(b.getGrade(), a.getGrade());
                    if (byGrade != 0) {
                        return byGrade;
                    }
                    return a.getId().toString().compareTo(b.getId().toString());
                })
                .toList();

        boolean changed;
        int safetyGuard = 0;
        do {
            changed = false;
            for (TraitDefinition outputTrait : combinableTraits) {
                for (TraitCombinationRecipe recipe : outputTrait.getCombinations()) {
                    while (canApplyRecipe(counts, recipe)) {
                        applyRecipe(counts, recipe, outputTrait.getId());
                        changed = true;
                    }
                }
            }
            safetyGuard++;
        } while (changed && safetyGuard < 256);

        List<TraitInstance> resolved = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((a, b) -> a.toString().compareTo(b.toString())))
                .forEach(entry -> {
                    for (int i = 0; i < entry.getValue(); i++) {
                        resolved.add(new TraitInstance(entry.getKey()));
                    }
                });
        return resolved;
    }

    private static boolean canApplyRecipe(Map<ResourceLocation, Integer> counts, TraitCombinationRecipe recipe) {
        Map<ResourceLocation, Integer> required = new HashMap<>();
        for (ResourceLocation ingredient : recipe.getIngredients()) {
            required.merge(ingredient, 1, Integer::sum);
        }
        for (Map.Entry<ResourceLocation, Integer> requirement : required.entrySet()) {
            if (counts.getOrDefault(requirement.getKey(), 0) < requirement.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static void applyRecipe(
            Map<ResourceLocation, Integer> counts,
            TraitCombinationRecipe recipe,
            ResourceLocation output
    ) {
        for (ResourceLocation ingredient : recipe.getIngredients()) {
            int current = counts.getOrDefault(ingredient, 0);
            if (current <= 1) {
                counts.remove(ingredient);
            } else {
                counts.put(ingredient, current - 1);
            }
        }
        counts.merge(output, 1, Integer::sum);
    }
}
