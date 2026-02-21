package com.ateliersteve.alchemy.ui;

import com.ateliersteve.alchemy.AlchemyItemData;
import com.ateliersteve.alchemy.element.CellType;
import com.ateliersteve.alchemy.element.ElementComponent;
import com.ateliersteve.alchemy.item.AlchemyItem;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeDefinition;
import com.ateliersteve.registry.ModDataComponents;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class AlchemyCombineStats {
    private AlchemyCombineStats() {
    }

    static void buildStatsBar(UIElement statsBar, Map<String, Integer> values) {
        statsBar.clearAllChildren();
        List<String> order = List.of("fire", "ice", "thunder", "wind", "light");
        for (String element : order) {
            var item = new UIElement().addClass("stat_item");
            var icon = new UIElement().addClass("stat_icon");
            ResourceLocation texture = AlchemyEffectPanel.resolveElementIcon(element);
            if (texture != null) {
                icon.lss("background", "sprite(" + texture + ")");
            } else {
                var elementColor = com.ateliersteve.alchemy.element.AlchemyElement.fromName(element);
                icon.lss("background", "rect(" + AlchemyEffectPanel.toHexColor(elementColor.getColor()) + ", 3)");
            }
            var value = new Label()
                    .setText(Component.literal(String.valueOf(values.getOrDefault(element, 0))))
                    .addClass("stat_value");
            item.addChildren(icon, value);
            statsBar.addChild(item);
        }
    }

    static int computeSuccessRate(Map<String, Integer> values) {
        int total = values.values().stream().mapToInt(Integer::intValue).sum();
        return Math.min(99, total);
    }

    static int computePlaceholderQuantity(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return 1;
        }
        int total = values.values().stream().mapToInt(Integer::intValue).sum();
        int quantity = 1 + (total / 12);
        return Math.max(1, Math.min(10, quantity));
    }

    static int computeCombinedQuality(AlchemyCombineSessionSnapshot snapshot) {
        if (snapshot == null || snapshot.placedMaterials().isEmpty()) {
            return 0;
        }
        int totalQuality = 0;
        int totalCells = 0;
        for (AlchemyCombineSessionSnapshot.PlacedMaterial placed : snapshot.placedMaterials()) {
            if (placed == null || placed.cells() == null || placed.cells().isEmpty()) {
                continue;
            }
            int cellCount = placed.cells().size();
            AlchemyItemData data = placed.stack().get(ModDataComponents.ALCHEMY_DATA.get());
            int quality = data == null ? 0 : data.quality();
            totalQuality += quality * cellCount;
            totalCells += cellCount;
        }
        if (totalCells <= 0) {
            return 0;
        }
        int average = Math.round((float) totalQuality / totalCells);
        return Math.max(0, Math.min(999, average));
    }

    static Map<String, Integer> computeCombinedElementValues(
            AlchemyCombineSessionSnapshot snapshot,
            AlchemyRecipeDefinition recipe
    ) {
        Map<String, Integer> insertedValues = computeInsertedElementValues(snapshot);
        Map<String, Integer> chainCounts = computeChainCounts(snapshot);
        if (recipe == null || insertedValues.isEmpty()) {
            return insertedValues;
        }

        Map<String, Integer> values = new HashMap<>(insertedValues);
        int maxIterations = Math.max(1, recipe.effects().size() + 1);
        for (int i = 0; i < maxIterations; i++) {
            List<AlchemyRecipeDefinition.ResolvedEffect> resolvedEffects = recipe.resolveEffects(values, chainCounts);
            Map<String, Integer> next = new HashMap<>(insertedValues);
            applyEnhancedElementBonuses(next, insertedValues, resolvedEffects);
            if (next.equals(values)) {
                break;
            }
            values = next;
        }
        return values;
    }

    static Map<String, Integer> computeChainCounts(AlchemyCombineSessionSnapshot snapshot) {
        Map<String, Integer> counts = new HashMap<>();
        if (snapshot == null || snapshot.placedMaterials().isEmpty()) {
            return counts;
        }

        Map<Long, AlchemyCombineSessionSnapshot.IngredientCell> linkCells = new HashMap<>();
        for (AlchemyCombineSessionSnapshot.PlacedMaterial placed : snapshot.placedMaterials()) {
            if (placed == null || placed.cells() == null || placed.cells().isEmpty()) {
                continue;
            }
            for (AlchemyCombineSessionSnapshot.IngredientCell cell : placed.cells()) {
                if (cell == null || cell.cellType() != CellType.LINK) {
                    continue;
                }
                int x = placed.originX() + cell.offsetX();
                int y = placed.originY() + cell.offsetY();
                linkCells.put(toKey(x, y), cell);
            }
        }

        int[][] directions = new int[][]{
                {1, 0},
                {0, 1},
                {1, 1},
                {-1, 1}
        };
        for (Map.Entry<Long, AlchemyCombineSessionSnapshot.IngredientCell> entry : linkCells.entrySet()) {
            long key = entry.getKey();
            int x = (int) (key >> 32);
            int y = (int) key;
            var cell = entry.getValue();
            if (cell == null || cell.element() == null) {
                continue;
            }
            for (int[] direction : directions) {
                long neighborKey = toKey(x + direction[0], y + direction[1]);
                var neighbor = linkCells.get(neighborKey);
                if (neighbor == null || neighbor.element() != cell.element()) {
                    continue;
                }
                counts.merge(cell.element().getSerializedName(), 1, Integer::sum);
            }
        }
        return counts;
    }

    static ItemStack resolveResultStack(AlchemyRecipeDefinition recipe) {
        if (recipe == null || recipe.result() == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(recipe.result());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    static int resolveLevel(AlchemyRecipeDefinition recipe, ItemStack resultStack) {
        int effectCount = recipe == null ? 0 : recipe.effects().size();
        int level = effectCount;
        if (!resultStack.isEmpty() && resultStack.getItem() instanceof AlchemyItem alchemyItem) {
            level = alchemyItem.getLevel();
        }
        return level;
    }

    static int resolveInitialQuality(ItemStack resultStack) {
        AlchemyItemData alchemyData = resultStack.get(ModDataComponents.ALCHEMY_DATA.get());
        return alchemyData == null ? 0 : alchemyData.quality();
    }

    private static Map<String, Integer> computeInsertedElementValues(AlchemyCombineSessionSnapshot snapshot) {
        Map<String, Integer> values = new HashMap<>();
        if (snapshot == null || snapshot.placedMaterials().isEmpty()) {
            return values;
        }
        for (AlchemyCombineSessionSnapshot.PlacedMaterial placed : snapshot.placedMaterials()) {
            for (AlchemyCombineSessionSnapshot.IngredientCell cell : placed.cells()) {
                values.merge(cell.element().getSerializedName(), 1, Integer::sum);
            }
        }
        return values;
    }

    private static void applyEnhancedElementBonuses(
            Map<String, Integer> targetValues,
            Map<String, Integer> insertedValues,
            List<AlchemyRecipeDefinition.ResolvedEffect> resolvedEffects
    ) {
        if (targetValues == null || insertedValues == null || insertedValues.isEmpty() || resolvedEffects == null || resolvedEffects.isEmpty()) {
            return;
        }

        Set<String> insertedElements = insertedValues.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (insertedElements.isEmpty()) {
            return;
        }

        for (AlchemyRecipeDefinition.ResolvedEffect resolvedEffect : resolvedEffects) {
            for (ElementComponent bonus : resolvedEffect.bonusElements()) {
                int enhancedCount = bonus.getNormalCount() + bonus.getLinkCount();
                if (enhancedCount <= 0) {
                    continue;
                }
                String enhancedElement = bonus.element().getSerializedName();
                for (String insertedElement : insertedElements) {
                    targetValues.merge(insertedElement, enhancedCount, Integer::sum);
                }
                if (insertedElements.contains(enhancedElement)) {
                    targetValues.merge(enhancedElement, enhancedCount, Integer::sum);
                }
            }
        }
    }

    private static long toKey(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }
}
