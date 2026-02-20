package com.ateliersteve.alchemy.ui;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.AlchemyItemData;
import com.ateliersteve.alchemy.item.AlchemyItem;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeDefinition;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeIngredient;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeRegistry;
import com.ateliersteve.alchemy.ui.AlchemyCombineUI;
import com.ateliersteve.alchemy.ui.AlchemyEffectPanel;
import com.ateliersteve.block.GatheringBasketBlockEntity;
import com.ateliersteve.registry.ModDataComponents;
import com.ateliersteve.ui.StaticItemElement;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEvent;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public final class AlchemyMaterialSelectionUI {
    private static final int SEARCH_RADIUS = 6;
    private static final int GRID_COLUMNS = 4;
    private static final int VISIBLE_ROWS = 5;
    private static final int GRID_SLOT_CAP = 200;
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
        UUID playerId = player.getUUID();
        ResourceLocation id = pending.remove(playerId);
        if (id == null) {
            Map<UUID, ResourceLocation> fallback = player.level().isClientSide
                    ? PENDING_RECIPES_SERVER
                    : PENDING_RECIPES_CLIENT;
            id = fallback.get(playerId);
        }
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
        var previewItemSlot = ui.select("#preview_item_slot").findFirst().orElseThrow();
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
        var combineButton = ui.select("#combine_button").findFirst().orElseThrow();
        var combineButtonLabel = (Label) ui.select("#combine_button_label").findFirst().orElseThrow();

        var gridContent = new UIElement().addClass("grid_content");
        gridScroller.addScrollViewChild(gridContent);

        root.setFocusable(true);
        root.focus();
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> root.focus());

        ItemStack resultStack = resolveResultStack(recipe);
        recipeTitle.setText(resultStack.isEmpty()
                ? Component.literal(recipe == null ? "No Recipe" : recipe.result().toString())
                : resultStack.getHoverName());

        levelLabel.setText(Component.literal("LV"));
        usageLabel.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.usage_count"));
        craftLabel.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.craft_amount"));
        qualityLabel.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.quality"));

        setItemElementStack(previewItemSlot, resultStack);

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
        AlchemyEffectPanel.buildQualityBar(qualityBar, quality);

        AlchemyEffectPanel.buildEffectAttributes(recipe, Map.of(), attributesScroller);

        List<GatheringBasketBlockEntity> baskets = findNearbyBaskets(player, cauldronPos, SEARCH_RADIUS);
        List<BasketStackRef> basketStacks = collectBasketStacks(baskets);

        List<AlchemyRecipeIngredient> ingredients = recipe == null ? List.of() : recipe.ingredients();
        List<List<BasketStackRef>> perIngredient = new ArrayList<>();
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
            AlchemyEffectPanel.buildEffectAttributes(recipe, values, attributesScroller);
        };

        combineButtonLabel.setText(Component.literal("\u8fdb\u5165\u8c03\u548c"));
        RPCEvent combineRpc = RPCEventBuilder.simple(ResourceLocation.class, BlockPos.class, Integer[].class,
                (recipeId, pos, pairs) -> {
                    if (!(player instanceof ServerPlayer serverPlayer)) {
                        return;
                    }
                    AlchemyRecipeDefinition targetRecipe = AlchemyRecipeRegistry.findById(recipeId);
                    if (targetRecipe == null) {
                        return;
                    }
                    List<BasketStackRef> selectedRefs = new ArrayList<>();
                    if (pairs != null) {
                        for (int i = 0; i + 1 < pairs.length; i += 2) {
                            Integer ingredientIndex = pairs[i];
                            Integer slotIndex = pairs[i + 1];
                            if (ingredientIndex == null || slotIndex == null) {
                                continue;
                            }
                            if (ingredientIndex < 0 || ingredientIndex >= perIngredient.size()) {
                                continue;
                            }
                             List<BasketStackRef> stacks = perIngredient.get(ingredientIndex);
                             if (slotIndex < 0 || slotIndex >= stacks.size()) {
                                 continue;
                             }
                             BasketStackRef selectedRef = stacks.get(slotIndex);
                             ItemStack stack = selectedRef.stack();
                             if (!stack.isEmpty()) {
                                 selectedRefs.add(selectedRef);
                             }
                          }
                      }
                    if (selectedRefs.isEmpty()) {
                        return;
                    }

                    List<ItemStack> selected = new ArrayList<>(selectedRefs.size());
                    for (BasketStackRef ref : selectedRefs) {
                        if (ref != null && ref.stack() != null && !ref.stack().isEmpty()) {
                            selected.add(ref.stack().copy());
                        }
                    }
                    if (selected.isEmpty()) {
                        return;
                    }
                    serverPlayer.closeContainer();
                    serverPlayer.getServer().execute(() -> {
                        List<AlchemyCombineUI.ReservedMaterialRef> reserved = reserveFromBaskets(serverPlayer, selectedRefs);
                        AlchemyCombineUI.requestOpen(serverPlayer, targetRecipe, selected, reserved);
                        BlockUIMenuType.openUI(serverPlayer, pos);
                    });
                });
        combineButton.addRPCEvent(combineRpc);
        Runnable updateCombineButton = () -> {
            if (isSelectionComplete(ingredients, selectedSlots)) {
                combineButton.removeClass("disabled");
            } else {
                combineButton.addClass("disabled");
            }
        };

        combineButton.addEventListener(UIEvents.CLICK, e -> {
            if (!isSelectionComplete(ingredients, selectedSlots)) {
                return;
            }
            List<ItemStack> selectedStacks = collectSelectedStacks(perIngredient, selectedSlots);
            if (selectedStacks.isEmpty()) {
                return;
            }
            AlchemyCombineUI.requestOpenClient(player, recipe, selectedStacks);
            Integer[] payload = buildSelectionPairs(selectedSlots);
            combineButton.sendEvent(combineRpc, recipe.id(), cauldronPos, payload);
        });

        AtomicInteger selectedIndex = new AtomicInteger(0);
        Runnable[] refreshRef = new Runnable[1];
        refreshRef[0] = () -> {
            int index = selectedIndex.get();
            if (ingredients.isEmpty()) {
                ingredientFilter.setText(Component.literal("No ingredients"));
                ingredientTabs.clearAllChildren();
            populateGrid(gridContent, List.of(), null, null);
                updateEffects.run();
                updateCombineButton.run();
                return;
            }
            if (index < 0 || index >= ingredients.size()) {
                selectedIndex.set(0);
                index = 0;
            }
            int indexFinal = index;
            ingredientFilter.setText(buildIngredientFilterText(ingredients.get(index), toStacks(perIngredient.get(index))));
            refreshIngredientTabs(ingredientTabs, ingredients, index, selectedIndex, selectedSlots, refreshRef[0]);
            LinkedHashSet<Integer> selectedSet = selectedSlots.get(indexFinal);
            populateGrid(gridContent, toStacks(perIngredient.get(indexFinal)), selectedSet, (slot, stack) -> {
                if (stack.isEmpty()) {
                    return;
                }
                toggleSelectedSlot(ingredients.get(indexFinal), selectedSlots, indexFinal, slot);
                refreshRef[0].run();
            });
            updateEffects.run();
            updateCombineButton.run();
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
            UIElement icon = buildIngredientIcon(ingredient);
            Label countLabel = new Label();
            countLabel.setText(buildIngredientCountText(selectedSlots.get(i), ingredient.count()));
            countLabel.addClass("ingredient_count");
            tab.addChildren(icon, countLabel);
            int index = i;
            Runnable selectTabClient = () -> {
                selected.set(index);
                refresh.run();
            };
            tab.addEventListener(UIEvents.CLICK, e -> selectTabClient.run());
            icon.addEventListener(UIEvents.CLICK, e -> selectTabClient.run());
            countLabel.addEventListener(UIEvents.CLICK, e -> selectTabClient.run());
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
        int slotCount = GRID_SLOT_CAP;
        List<ItemStack> gridStacks = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            if (i < stacks.size()) {
                gridStacks.add(stacks.get(i));
            } else {
                gridStacks.add(ItemStack.EMPTY);
            }
        }

        int totalRows = (int) Math.ceil((double) slotCount / GRID_COLUMNS);
        for (int row = 0; row < totalRows; row++) {
            var rowContainer = new UIElement().addClass("grid_row");
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int slot = row * GRID_COLUMNS + col;
                if (slot < slotCount) {
                    ItemStack stack = gridStacks.get(slot);
                    boolean isSelected = !stack.isEmpty() && selectedSlots != null && selectedSlots.contains(slot);
                    BiConsumer<Integer, ItemStack> clickHandler = stack.isEmpty() ? null : onStackClick;
                    rowContainer.addChild(buildGridSlot(slot, stack, isSelected, clickHandler));
                }
            }
            gridContent.addChild(rowContainer);
        }
    }

    private static UIElement buildGridSlot(
            int slot,
            ItemStack stack,
            boolean selected,
            BiConsumer<Integer, ItemStack> onStackClick
    ) {
        var cell = new UIElement().addClass("grid_slot_cell");
        if (selected) {
            cell.addClass("selected");
        }
        var slotElement = new StaticItemElement().setStack(stack).addClass("grid_slot_item");
        var check = new Label().setText(Component.literal("\u2713")).addClass("grid_slot_check");
        if (!selected) {
            check.addClass("hidden");
        }
        cell.addChildren(slotElement, check);
        if (onStackClick != null && !stack.isEmpty()) {
            slotElement.addEventListener(UIEvents.CLICK, e -> onStackClick.accept(slot, stack));
        }
        return cell;
    }

    private static void toggleSelectedSlot(
            AlchemyRecipeIngredient ingredient,
            Map<Integer, LinkedHashSet<Integer>> selectedSlots,
            int index,
            int slot
    ) {
        if (ingredient == null || selectedSlots == null) {
            return;
        }
        LinkedHashSet<Integer> current = selectedSlots.get(index);
        if (current != null && current.contains(slot)) {
            current.remove(slot);
            if (current.isEmpty()) {
                selectedSlots.remove(index);
            }
        } else {
            int max = ingredient.count();
            if (current == null) {
                current = new LinkedHashSet<>();
                selectedSlots.put(index, current);
            }
            if (current.size() >= max) {
                Integer first = current.iterator().next();
                current.remove(first);
            }
            current.add(slot);
        }
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
            return new StaticItemElement().setStack(stack).addClass("ingredient_icon_slot");
        }
        return new Label().setText(Component.literal("?")).addClass("ingredient_icon_fallback");
    }

    private static void setItemElementStack(UIElement element, ItemStack stack) {
        element.clearAllChildren();
        element.addChild(new StaticItemElement().setStack(stack)
                .lss("width", "100%")
                .lss("height", "100%"));
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

    private static List<BasketStackRef> collectBasketStacks(List<GatheringBasketBlockEntity> baskets) {
        if (baskets == null || baskets.isEmpty()) {
            return List.of();
        }
        List<BasketStackRef> stacks = new ArrayList<>();
        for (GatheringBasketBlockEntity basket : baskets) {
            ItemStackHandler handler = basket.getInventory();
            if (handler == null) {
                continue;
            }
            BlockPos basketPos = basket.getBlockPos();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    stacks.add(new BasketStackRef(basketPos, i, stack.copy()));
                }
            }
        }
        return stacks;
    }

    private static List<BasketStackRef> filterStacksForIngredient(List<BasketStackRef> stacks, AlchemyRecipeIngredient ingredient) {
        if (stacks == null || stacks.isEmpty() || ingredient == null) {
            return List.of();
        }
        List<BasketStackRef> matches = new ArrayList<>();
        for (BasketStackRef stackRef : stacks) {
            ItemStack stack = stackRef.stack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (matchesIngredient(stack, ingredient)) {
                matches.add(stackRef);
            }
        }
        return matches;
    }

    private static List<ItemStack> toStacks(List<BasketStackRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>(refs.size());
        for (BasketStackRef ref : refs) {
            stacks.add(ref == null || ref.stack() == null ? ItemStack.EMPTY : ref.stack().copy());
        }
        return stacks;
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

    private static boolean isSelectionComplete(
            List<AlchemyRecipeIngredient> ingredients,
            Map<Integer, LinkedHashSet<Integer>> selectedSlots
    ) {
        if (ingredients == null || ingredients.isEmpty()) {
            return false;
        }
        for (int i = 0; i < ingredients.size(); i++) {
            int needed = ingredients.get(i).count();
            int selected = selectedSlots.getOrDefault(i, new LinkedHashSet<>()).size();
            if (selected < needed) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> collectSelectedStacks(
            List<List<BasketStackRef>> perIngredient,
            Map<Integer, LinkedHashSet<Integer>> selectedSlots
    ) {
        List<ItemStack> selected = new ArrayList<>();
        if (perIngredient == null || selectedSlots == null) {
            return selected;
        }
        for (var entry : selectedSlots.entrySet()) {
            int index = entry.getKey();
            if (index < 0 || index >= perIngredient.size()) {
                continue;
            }
            List<BasketStackRef> stacks = perIngredient.get(index);
            for (Integer slot : entry.getValue()) {
                if (slot == null || slot < 0 || slot >= stacks.size()) {
                    continue;
                }
                ItemStack stack = stacks.get(slot).stack();
                if (!stack.isEmpty()) {
                    selected.add(stack.copy());
                }
            }
        }
        return selected;
    }

    private static Integer[] buildSelectionPairs(Map<Integer, LinkedHashSet<Integer>> selectedSlots) {
        if (selectedSlots == null || selectedSlots.isEmpty()) {
            return new Integer[0];
        }
        List<Integer> pairs = new ArrayList<>();
        for (var entry : selectedSlots.entrySet()) {
            Integer ingredientIndex = entry.getKey();
            if (ingredientIndex == null) {
                continue;
            }
            for (Integer slotIndex : entry.getValue()) {
                if (slotIndex == null) {
                    continue;
                }
                pairs.add(ingredientIndex);
                pairs.add(slotIndex);
            }
        }
        return pairs.toArray(Integer[]::new);
    }

    private static Map<String, Integer> computeSelectedElementValues(
            List<List<BasketStackRef>> perIngredient,
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
            List<BasketStackRef> stacks = perIngredient.get(index);
            for (Integer slot : entry.getValue()) {
                if (slot == null || slot < 0 || slot >= stacks.size()) {
                    continue;
                }
                ItemStack stack = stacks.get(slot).stack();
                if (stack.isEmpty()) {
                    continue;
                }
                AlchemyItemData data = stack.get(ModDataComponents.ALCHEMY_DATA.get());
                if (data == null) {
                    continue;
                }
                for (var component : data.elements()) {
                    int amount = component.getNormalCount() + component.getLinkCount();
                    values.merge(component.element().getSerializedName(), amount, Integer::sum);
                }
            }
        }
        return values;
    }

    private static List<AlchemyCombineUI.ReservedMaterialRef> reserveFromBaskets(
            ServerPlayer player,
            List<BasketStackRef> selectedRefs
    ) {
        if (player == null || selectedRefs == null || selectedRefs.isEmpty()) {
            return List.of();
        }
        List<AlchemyCombineUI.ReservedMaterialRef> reserved = new ArrayList<>();
        java.util.Set<String> visitedSlots = new java.util.HashSet<>();
        for (BasketStackRef ref : selectedRefs) {
            if (ref == null || ref.stack() == null || ref.stack().isEmpty()) {
                continue;
            }
            String key = ref.basketPos().asLong() + ":" + ref.slotIndex();
            if (!visitedSlots.add(key)) {
                continue;
            }
            if (!(player.level().getBlockEntity(ref.basketPos()) instanceof GatheringBasketBlockEntity basket)) {
                continue;
            }
            ItemStackHandler inventory = basket.getInventory();
            if (inventory == null) {
                continue;
            }
            int slotIndex = ref.slotIndex();
            if (slotIndex < 0 || slotIndex >= inventory.getSlots()) {
                continue;
            }
            ItemStack current = inventory.getStackInSlot(slotIndex);
            if (current.isEmpty()) {
                continue;
            }
            inventory.setStackInSlot(slotIndex, ItemStack.EMPTY);
            reserved.add(new AlchemyCombineUI.ReservedMaterialRef(ref.basketPos(), slotIndex, current.copy()));
        }
        return reserved;
    }

    private record BasketStackRef(BlockPos basketPos, int slotIndex, ItemStack stack) {
    }

}
