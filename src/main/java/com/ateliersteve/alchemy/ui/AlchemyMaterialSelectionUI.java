package com.ateliersteve.alchemy.ui;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.AlchemyItemData;
import com.ateliersteve.alchemy.element.AlchemyElement;
import com.ateliersteve.alchemy.item.AlchemyItem;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeDefinition;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeIngredient;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeRegistry;
import com.ateliersteve.block.GatheringBasketBlockEntity;
import com.ateliersteve.registry.ModDataComponents;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.appliedenergistics.yoga.YogaAlign;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public final class AlchemyMaterialSelectionUI {
    private static final int QUALITY_MAX = 999;
    private static final int QUALITY_SEGMENT_WIDTH = 12;
    private static final List<Integer> QUALITY_COLORS = List.of(
            0x6fa3ff,
            0xffe66d,
            0xff9f43,
            0xff6b6b,
            0xa855f7,
            0x1dd1a1,
            0x54a0ff,
            0x5f27cd,
            0x222f3e,
            0x000000
    );
    private static final int SEARCH_RADIUS = 6;
    private static final int GRID_COLUMNS = 4;
    private static final int VISIBLE_ROWS = 5;
    private static final Map<UUID, ResourceLocation> PENDING_RECIPES_SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, ResourceLocation> PENDING_RECIPES_CLIENT = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> TAG_CATEGORY_ICONS = Map.of(
            AtelierSteve.id("category_gunpowder"), AtelierSteve.id("textures/gui/ingredients/category_gunpowder.png"),
            AtelierSteve.id("category_water"), AtelierSteve.id("textures/gui/ingredients/category_water.png")
    );

    private AlchemyMaterialSelectionUI() {
    }

    public static void requestOpen(ServerPlayer player, AlchemyRecipeDefinition recipe) {
        if (player == null || recipe == null) {
            return;
        }
        PENDING_RECIPES_SERVER.put(player.getUUID(), recipe.id());
    }

    public static void requestOpenClient(Player player, AlchemyRecipeDefinition recipe) {
        if (player == null || recipe == null) {
            return;
        }
        PENDING_RECIPES_CLIENT.put(player.getUUID(), recipe.id());
    }

    public static AlchemyRecipeDefinition consumePendingRecipe(Player player) {
        if (player == null) {
            return null;
        }
        Map<UUID, ResourceLocation> pending = player.level().isClientSide
                ? PENDING_RECIPES_CLIENT
                : PENDING_RECIPES_SERVER;
        ResourceLocation id = pending.remove(player.getUUID());
        if (id == null) {
            return null;
        }
        return AlchemyRecipeRegistry.findById(id);
    }

    public static ModularUI createUI(Player player, BlockPos cauldronPos, AlchemyRecipeDefinition recipe) {
        var xml = XmlUtils.loadXml(AtelierSteve.id("alchemy_material_selection.xml"));
        if (xml == null) {
            return ModularUI.of(UI.of(new UIElement()), player);
        }

        var ui = UI.of(xml);
        var root = ui.select("#alchemy_material_select_root").findFirst().orElseThrow();
        var recipeTitle = (Label) ui.select("#recipe_title").findFirst().orElseThrow();
        var ingredientFilter = (Label) ui.select("#ingredient_filter").findFirst().orElseThrow();
        var ingredientTabs = ui.select("#ingredient_tabs").findFirst().orElseThrow();
        var gridScroller = (ScrollerView) ui.select("#ingredient_grid").findFirst().orElseThrow();
        var hintLabel = (Label) ui.select("#basket_hint").findFirst().orElseThrow();
        var previewItemSlot = (ItemSlot) ui.select("#preview_item_slot").findFirst().orElseThrow();
        var levelLabel = (Label) ui.select("#level_label").findFirst().orElseThrow();
        var levelValue = (Label) ui.select("#level_value").findFirst().orElseThrow();
        var usageLabel = (Label) ui.select("#usage_label").findFirst().orElseThrow();
        var usageValue = (Label) ui.select("#usage_value").findFirst().orElseThrow();
        var craftLabel = (Label) ui.select("#craft_label").findFirst().orElseThrow();
        var craftValue = (Label) ui.select("#craft_value").findFirst().orElseThrow();
        var qualityLabel = (Label) ui.select("#quality_label").findFirst().orElseThrow();
        var qualityValue = (Label) ui.select("#quality_value").findFirst().orElseThrow();
        var qualityBar = ui.select("#quality_bar").findFirst().orElseThrow();
        var attributesScroller = ui.select("#attributes_scroller").findFirst().orElseThrow();

        var gridContent = new UIElement().addClass("grid_content");
        gridScroller.addScrollViewChild(gridContent);

        root.setFocusable(true);
        root.focus();
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> root.focus());
        root.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 1 && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
                serverPlayer.getServer().execute(() -> BlockUIMenuType.openUI(serverPlayer, cauldronPos));
            }
        });
        root.addServerEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ESCAPE && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
                serverPlayer.getServer().execute(() -> BlockUIMenuType.openUI(serverPlayer, cauldronPos));
            }
        });

        ItemStack resultStack = resolveResultStack(recipe);
        recipeTitle.setText(resultStack.isEmpty()
                ? Component.literal(recipe == null ? "No Recipe" : recipe.result().toString())
                : resultStack.getHoverName());

        levelLabel.setText(Component.literal("LV"));
        usageLabel.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.usage_count"));
        craftLabel.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.craft_amount"));
        qualityLabel.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.quality"));

        var resultHandler = createDisplayHandler(List.of(resultStack));
        previewItemSlot.bind(resultHandler, 0);

        int effectCount = recipe == null ? 0 : recipe.effects().size();
        int totalIngredients = recipe == null
                ? 0
                : recipe.ingredients().stream().mapToInt(AlchemyRecipeIngredient::count).sum();
        int level = effectCount;
        if (!resultStack.isEmpty() && resultStack.getItem() instanceof AlchemyItem alchemyItem) {
            level = alchemyItem.getLevel();
        }
        levelValue.setText(Component.literal(String.valueOf(level)));
        usageValue.setText(Component.literal("-"));
        craftValue.setText(Component.literal(String.valueOf(totalIngredients)));

        AlchemyItemData alchemyData = resultStack.get(ModDataComponents.ALCHEMY_DATA.get());
        int quality = alchemyData == null ? 0 : alchemyData.quality();
        qualityValue.setText(quality <= 0 ? Component.literal("-") : Component.literal(String.valueOf(quality)));
        buildQualityBar(qualityBar, quality);

        buildEffectAttributes(recipe, Map.of(), attributesScroller);

        List<GatheringBasketBlockEntity> baskets = findNearbyBaskets(player, cauldronPos, SEARCH_RADIUS);
        List<ItemStack> basketStacks = collectBasketStacks(baskets);

        List<AlchemyRecipeIngredient> ingredients = recipe == null ? List.of() : recipe.ingredients();
        List<List<ItemStack>> perIngredient = new ArrayList<>();
        for (AlchemyRecipeIngredient ingredient : ingredients) {
            perIngredient.add(filterStacksForIngredient(basketStacks, ingredient));
        }

        boolean hasBasket = !baskets.isEmpty();
        boolean hasItems = !basketStacks.isEmpty();
        boolean hasMatches = perIngredient.stream().anyMatch(matches -> !matches.isEmpty());
        if (!hasBasket) {
            hintLabel.setText(Component.literal("附近没有采集篮"));
        } else if (!hasItems) {
            hintLabel.setText(Component.literal("采集篮是空的"));
        } else if (!hasMatches) {
            hintLabel.setText(Component.literal("采集篮内没有匹配材料"));
        } else {
            hintLabel.setText(Component.empty());
        }

        Map<Integer, LinkedHashSet<Integer>> selectedSlots = new HashMap<>();
        Runnable updateEffects = () -> {
            Map<String, Integer> values = computeSelectedElementValues(perIngredient, selectedSlots);
            buildEffectAttributes(recipe, values, attributesScroller);
        };

        AtomicInteger selectedIndex = new AtomicInteger(0);
        Runnable[] refreshRef = new Runnable[1];
        refreshRef[0] = () -> {
            int index = selectedIndex.get();
            if (ingredients.isEmpty()) {
                ingredientFilter.setText(Component.literal("No ingredients"));
                ingredientTabs.clearAllChildren();
                populateGrid(gridContent, List.of(), null, null);
                updateEffects.run();
                return;
            }
            if (index < 0 || index >= ingredients.size()) {
                selectedIndex.set(0);
                index = 0;
            }
            int indexFinal = index;
            ingredientFilter.setText(buildIngredientFilterText(ingredients.get(index), perIngredient.get(index)));
            refreshIngredientTabs(ingredientTabs, ingredients, index, selectedIndex, selectedSlots, refreshRef[0]);
            LinkedHashSet<Integer> selectedSet = selectedSlots.get(indexFinal);
            populateGrid(gridContent, perIngredient.get(indexFinal), selectedSet, (slot, stack) -> {
                if (stack.isEmpty()) {
                    return;
                }
                LinkedHashSet<Integer> current = selectedSlots.get(indexFinal);
                if (current != null && current.contains(slot)) {
                    current.remove(slot);
                    if (current.isEmpty()) {
                        selectedSlots.remove(indexFinal);
                    }
                } else {
                    int max = ingredients.get(indexFinal).count();
                    if (current == null) {
                        current = new LinkedHashSet<>();
                        selectedSlots.put(indexFinal, current);
                    }
                    if (current.size() >= max) {
                        Integer first = current.iterator().next();
                        current.remove(first);
                    }
                    current.add(slot);
                }
                refreshRef[0].run();
            });
            updateEffects.run();
        };

        refreshRef[0].run();

        return ModularUI.of(ui, player);
    }

    private static void refreshIngredientTabs(
            UIElement container,
            List<AlchemyRecipeIngredient> ingredients,
            int selectedIndex,
            AtomicInteger selected,
            Map<Integer, LinkedHashSet<Integer>> selectedSlots,
            Runnable refresh
    ) {
        container.clearAllChildren();
        for (int i = 0; i < ingredients.size(); i++) {
            AlchemyRecipeIngredient ingredient = ingredients.get(i);
            UIElement tab = new UIElement().addClass("ingredient_tab");
            if (i == selectedIndex) {
                tab.addClass("active");
            }
            tab.addChildren(
                    buildIngredientIcon(ingredient),
                    new Label().setText(buildIngredientCountText(selectedSlots.get(i), ingredient.count()))
                            .addClass("ingredient_count")
            );
            int index = i;
            tab.addEventListener(UIEvents.CLICK, e -> {
                selected.set(index);
                refresh.run();
            });
            container.addChild(tab);
        }
    }

    private static void populateGrid(
            UIElement gridContent,
            List<ItemStack> stacks,
            LinkedHashSet<Integer> selectedSlots,
            BiConsumer<Integer, ItemStack> onStackClick
    ) {
        gridContent.clearAllChildren();
        int slotCount = Math.max(stacks.size(), GRID_COLUMNS * VISIBLE_ROWS);
        List<ItemStack> gridStacks = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            if (i < stacks.size()) {
                gridStacks.add(stacks.get(i));
            } else {
                gridStacks.add(ItemStack.EMPTY);
            }
        }

        var handler = createDisplayHandler(gridStacks);
        int totalRows = (int) Math.ceil((double) slotCount / GRID_COLUMNS);
        for (int row = 0; row < totalRows; row++) {
            var rowContainer = new UIElement().addClass("grid_row");
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int slot = row * GRID_COLUMNS + col;
                if (slot < slotCount) {
                    ItemStack stack = gridStacks.get(slot);
                    boolean isSelected = !stack.isEmpty() && selectedSlots != null && selectedSlots.contains(slot);
                    BiConsumer<Integer, ItemStack> clickHandler = stack.isEmpty() ? null : onStackClick;
                    rowContainer.addChild(buildGridSlot(handler, slot, stack, isSelected, clickHandler));
                }
            }
            gridContent.addChild(rowContainer);
        }
    }

    private static UIElement buildGridSlot(
            ItemStackHandler handler,
            int slot,
            ItemStack stack,
            boolean selected,
            BiConsumer<Integer, ItemStack> onStackClick
    ) {
        var cell = new UIElement().addClass("grid_slot_cell");
        var slotElement = new ItemSlot().bind(handler, slot).addClass("grid_slot_item");
        var check = new Label().setText(Component.literal("\u2713")).addClass("grid_slot_check");
        if (!selected) {
            check.addClass("hidden");
        }
        cell.addChildren(slotElement, check);
        if (onStackClick != null && !stack.isEmpty()) {
            cell.addEventListener(UIEvents.CLICK, e -> onStackClick.accept(slot, stack));
        }
        return cell;
    }

    private static Component buildIngredientCountText(LinkedHashSet<Integer> selectedSlots, int total) {
        int selectedCount = selectedSlots == null ? 0 : selectedSlots.size();
        return Component.literal(selectedCount + "/" + total);
    }

    private static UIElement buildIngredientIcon(AlchemyRecipeIngredient ingredient) {
        ResourceLocation icon = resolveCategoryIcon(ingredient);
        if (icon != null) {
            return new UIElement()
                    .addClass("ingredient_icon")
                    .lss("background", "sprite(" + icon + ")");
        }
        ItemStack stack = resolveIngredientStack(ingredient);
        if (!stack.isEmpty()) {
            var handler = createDisplayHandler(List.of(stack));
            return new ItemSlot().bind(handler, 0).addClass("ingredient_icon_slot");
        }
        return new Label().setText(Component.literal("?")).addClass("ingredient_icon_fallback");
    }

    private static Component buildIngredientFilterText(AlchemyRecipeIngredient ingredient, List<ItemStack> matches) {
        if (ingredient == null) {
            return Component.literal("-");
        }
        if (ingredient.type() == AlchemyRecipeIngredient.Type.TAG && ingredient.tag().isPresent()) {
            ResourceLocation tagId = ingredient.tag().get().location();
            return Component.literal("#" + tagId);
        }
        if (ingredient.type() == AlchemyRecipeIngredient.Type.SPECIFIC && ingredient.itemId().isPresent()) {
            Item item = BuiltInRegistries.ITEM.get(ingredient.itemId().get());
            if (item != null) {
                return new ItemStack(item).getHoverName();
            }
        }
        if (!matches.isEmpty()) {
            return matches.get(0).getHoverName();
        }
        return Component.literal("Unknown");
    }

    private static List<GatheringBasketBlockEntity> findNearbyBaskets(Player player, BlockPos center, int radius) {
        if (player == null || center == null) {
            return List.of();
        }
        Level level = player.level();
        if (level == null) {
            return List.of();
        }
        List<GatheringBasketBlockEntity> baskets = new ArrayList<>();
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(pos) instanceof GatheringBasketBlockEntity basket) {
                baskets.add(basket);
            }
        }
        return baskets;
    }

    private static List<ItemStack> collectBasketStacks(List<GatheringBasketBlockEntity> baskets) {
        if (baskets == null || baskets.isEmpty()) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (GatheringBasketBlockEntity basket : baskets) {
            ItemStackHandler handler = basket.getInventory();
            if (handler == null) {
                continue;
            }
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
        }
        return stacks;
    }

    private static List<ItemStack> filterStacksForIngredient(List<ItemStack> stacks, AlchemyRecipeIngredient ingredient) {
        if (stacks == null || stacks.isEmpty() || ingredient == null) {
            return List.of();
        }
        List<ItemStack> matches = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (matchesIngredient(stack, ingredient)) {
                matches.add(stack.copy());
            }
        }
        return matches;
    }

    private static boolean matchesIngredient(ItemStack stack, AlchemyRecipeIngredient ingredient) {
        if (ingredient.type() == AlchemyRecipeIngredient.Type.SPECIFIC && ingredient.itemId().isPresent()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return ingredient.itemId().get().equals(itemId);
        }
        if (ingredient.type() == AlchemyRecipeIngredient.Type.TAG && ingredient.tag().isPresent()) {
            TagKey<Item> tagKey = ingredient.tag().get();
            return stack.is(tagKey);
        }
        return false;
    }

    private static ItemStackHandler createDisplayHandler(List<ItemStack> stacks) {
        var handler = new ItemStackHandler(stacks.size()) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }
        };

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            handler.setStackInSlot(i, stack == null ? ItemStack.EMPTY : stack.copy());
        }

        return handler;
    }

    private static ItemStack resolveResultStack(AlchemyRecipeDefinition recipe) {
        if (recipe == null || recipe.result() == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(recipe.result());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private static ItemStack resolveIngredientStack(AlchemyRecipeIngredient ingredient) {
        if (ingredient == null) {
            return ItemStack.EMPTY;
        }
        if (ingredient.type() == AlchemyRecipeIngredient.Type.SPECIFIC) {
            return ingredient.itemId()
                    .map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id), ingredient.count()))
                    .orElse(ItemStack.EMPTY);
        }
        if (ingredient.type() == AlchemyRecipeIngredient.Type.TAG && ingredient.tag().isPresent()) {
            TagKey<Item> tagKey = ingredient.tag().get();
            var tag = BuiltInRegistries.ITEM.getTag(tagKey);
            if (tag.isPresent()) {
                for (Holder<Item> holder : tag.get()) {
                    return new ItemStack(holder.value(), ingredient.count());
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static ResourceLocation resolveCategoryIcon(AlchemyRecipeIngredient ingredient) {
        if (ingredient == null || ingredient.type() != AlchemyRecipeIngredient.Type.TAG) {
            return null;
        }
        if (ingredient.tag().isEmpty()) {
            return null;
        }
        ResourceLocation tagId = ingredient.tag().get().location();
        return TAG_CATEGORY_ICONS.get(tagId);
    }

    private static void buildQualityBar(UIElement bar, int quality) {
        bar.clearAllChildren();
        int clamped = Math.max(0, Math.min(quality, QUALITY_MAX));
        for (int i = 0; i < QUALITY_COLORS.size(); i++) {
            int segmentStart = i * 100;
            int segmentEnd = (i + 1) * 100;
            int fillWidth = 0;
            if (clamped >= segmentEnd) {
                fillWidth = QUALITY_SEGMENT_WIDTH;
            } else if (clamped > segmentStart) {
                float ratio = (clamped - segmentStart) / 100f;
                fillWidth = Math.max(1, Math.round(ratio * QUALITY_SEGMENT_WIDTH));
            }

            var segment = new UIElement().addClass("quality_segment");
            if (fillWidth > 0) {
                int fillWidthFinal = fillWidth;
                var fill = new UIElement()
                        .addClass("quality_segment_fill")
                        .layout(layout -> layout.width(fillWidthFinal));
                fill.lss("background", "rect(" + toHexColor(QUALITY_COLORS.get(i)) + ", 1)");
                segment.addChild(fill);
            }
            bar.addChild(segment);
        }
    }

    private static void buildEffectAttributes(
            AlchemyRecipeDefinition recipe,
            Map<String, Integer> elementValues,
            UIElement attributesScroller
    ) {
        attributesScroller.clearAllChildren();
        var attributesList = new UIElement()
                .addClass("attributes_list")
                .layout(layout -> layout.widthPercent(100));
        attributesScroller.addChild(attributesList);

        if (recipe == null || recipe.effects().isEmpty()) {
            attributesList.addChild(new Label()
                    .setText(Component.literal("\u65e0"))
                    .addClass("effects_empty"));
            return;
        }

        boolean added = false;
        for (AlchemyRecipeDefinition.EffectGroup group : recipe.effects()) {
            int max = 0;
            Set<Integer> positions = new TreeSet<>();
            for (AlchemyRecipeDefinition.EffectStep step : group.steps()) {
                int threshold = step.threshold();
                if (threshold <= 0) {
                    continue;
                }
                positions.add(threshold);
                if (threshold > max) {
                    max = threshold;
                }
            }
            if (max <= 0) {
                continue;
            }

            int value = Math.min(elementValues.getOrDefault(group.type(), 0), max);
            AlchemyElement element = AlchemyElement.fromName(group.type());
            String color = toHexColor(element.getColor());
            AlchemyRecipeDefinition.EffectStep step = group.selectStep(value);

            var row = new UIElement()
                    .addClass("attr_item")
                    .layout(layout -> layout.widthPercent(100));
            var icon = new UIElement()
                    .addClass("attr_icon")
                    .lss("background", "rect(" + color + ", 7)");
            var content = new UIElement().addClass("attr_content");
            var name = new Label()
                    .setText(step == null ? Component.literal("\u65e0") : Component.literal(step.value()))
                    .addClass("attr_name");
            var bar = new UIElement()
                    .addClass("attr_bar")
                    .layout(layout -> layout.setAlignItems(YogaAlign.FLEX_END));

            for (int n = 1; n <= max; n++) {
                var segment = new UIElement()
                        .addClass("bar_segment")
                        .layout(layout -> layout.setAlignSelf(YogaAlign.FLEX_END));
                if (positions.contains(n)) {
                    segment.addClass("bar_segment_key");
                }
                if (n <= value) {
                    segment.lss("background", "rect(" + color + ", 1)");
                }
                bar.addChild(segment);
            }

            content.addChildren(name, bar);
            row.addChildren(icon, content);
            attributesList.addChild(row);
            added = true;
        }

        if (!added) {
            attributesList.addChild(new Label()
                    .setText(Component.literal("\u65e0"))
                    .addClass("effects_empty"));
        }
    }

    private static Map<String, Integer> computeSelectedElementValues(
            List<List<ItemStack>> perIngredient,
            Map<Integer, LinkedHashSet<Integer>> selectedSlots
    ) {
        Map<String, Integer> values = new HashMap<>();
        if (perIngredient == null || selectedSlots == null) {
            return values;
        }
        for (var entry : selectedSlots.entrySet()) {
            int index = entry.getKey();
            if (index < 0 || index >= perIngredient.size()) {
                continue;
            }
            List<ItemStack> stacks = perIngredient.get(index);
            for (Integer slot : entry.getValue()) {
                if (slot == null || slot < 0 || slot >= stacks.size()) {
                    continue;
                }
                ItemStack stack = stacks.get(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                AlchemyItemData data = stack.get(ModDataComponents.ALCHEMY_DATA.get());
                if (data == null) {
                    continue;
                }
                for (var component : data.elements()) {
                    values.merge(component.element().getSerializedName(), component.getNormalCount(), Integer::sum);
                }
            }
        }
        return values;
    }

    private static String toHexColor(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
}
