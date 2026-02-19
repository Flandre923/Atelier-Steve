package com.ateliersteve.alchemy.ui;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.AlchemyItemData;
import com.ateliersteve.alchemy.element.CellType;
import com.ateliersteve.alchemy.element.ElementComponent;
import com.ateliersteve.alchemy.element.AlchemyElement;
import com.ateliersteve.alchemy.item.AlchemyItem;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeDefinition;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeIngredient;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeRegistry;
import com.ateliersteve.ui.IngredientGridElement;
import com.ateliersteve.registry.ModDataComponents;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AlchemyCombineUI {
    private static final Map<UUID, PendingCombine> PENDING_COMBINE_SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingCombine> PENDING_COMBINE_CLIENT = new ConcurrentHashMap<>();

    private AlchemyCombineUI() {
    }

    public static void requestOpen(ServerPlayer player, AlchemyRecipeDefinition recipe, List<ItemStack> selectedStacks) {
        if (player == null || recipe == null) {
            return;
        }
        PENDING_COMBINE_SERVER.put(player.getUUID(), new PendingCombine(recipe.id(), copyStacks(selectedStacks)));
    }

    public static void requestOpenClient(Player player, AlchemyRecipeDefinition recipe, List<ItemStack> selectedStacks) {
        if (player == null || recipe == null) {
            return;
        }
        PENDING_COMBINE_CLIENT.put(player.getUUID(), new PendingCombine(recipe.id(), copyStacks(selectedStacks)));
    }

    public static PendingCombine consumePendingCombine(Player player) {
        if (player == null) {
            return null;
        }
        UUID playerId = player.getUUID();
        boolean isClient = player.level().isClientSide;
        Map<UUID, PendingCombine> primary = isClient ? PENDING_COMBINE_CLIENT : PENDING_COMBINE_SERVER;
        PendingCombine pending = primary.remove(playerId);
        if (pending != null) {
            return pending;
        }
        Map<UUID, PendingCombine> fallback = isClient ? PENDING_COMBINE_SERVER : PENDING_COMBINE_CLIENT;
        return fallback.remove(playerId);
    }

    public static ModularUI createUI(Player player, BlockPos cauldronPos, PendingCombine pending) {
        var xml = XmlUtils.loadXml(AtelierSteve.id("alchemy_combine.xml"));
        if (xml == null) {
            return ModularUI.of(UI.of(new UIElement()), player);
        }

        AlchemyRecipeDefinition recipe = pending == null ? null : AlchemyRecipeRegistry.findById(pending.recipeId());
        List<ItemStack> selectedStacks = pending == null ? List.of() : pending.selectedStacks();

        var ui = UI.of(xml);
        var root = ui.select("#alchemy_combine_root").findFirst().orElseThrow();
        var combineTitle = (Label) ui.select("#combine_title").findFirst().orElseThrow();
        var selectedList = ui.select("#selected_list_content").findFirst().orElseThrow();
        var selectedHint = (Label) ui.select("#selected_hint").findFirst().orElseThrow();
        var statsBar = ui.select("#stats_bar").findFirst().orElseThrow();
        var grid = ui.select("#combine_grid").findFirst().orElseThrow();
        var combineHint = (Label) ui.select("#combine_hint").findFirst().orElseThrow();
        var successLabel = (Label) ui.select("#success_label").findFirst().orElseThrow();
        var successFill = ui.select("#success_fill").findFirst().orElseThrow();
        var successValue = (Label) ui.select("#success_value").findFirst().orElseThrow();
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

        root.setFocusable(true);
        root.focus();
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> root.focus());
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 1 && recipe != null) {
                AlchemyMaterialSelectionUI.requestOpenClient(player, recipe);
            }
        });
        root.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 1 && player instanceof ServerPlayer serverPlayer) {
                if (recipe != null) {
                    AlchemyMaterialSelectionUI.requestOpen(serverPlayer, recipe);
                }
                serverPlayer.closeContainer();
                serverPlayer.getServer().execute(() -> BlockUIMenuType.openUI(serverPlayer, cauldronPos));
            }
        });
        root.addServerEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ESCAPE && player instanceof ServerPlayer serverPlayer) {
                if (recipe != null) {
                    AlchemyMaterialSelectionUI.requestOpen(serverPlayer, recipe);
                }
                serverPlayer.closeContainer();
                serverPlayer.getServer().execute(() -> BlockUIMenuType.openUI(serverPlayer, cauldronPos));
            }
        });
        root.addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ESCAPE && recipe != null) {
                AlchemyMaterialSelectionUI.requestOpenClient(player, recipe);
            }
        });

        ItemStack resultStack = resolveResultStack(recipe);
        combineTitle.setText(resultStack.isEmpty()
                ? Component.literal(recipe == null ? "No Recipe" : recipe.result().toString())
                : resultStack.getHoverName());

        populateSelectedList(selectedList, selectedStacks, selectedHint);

        buildStatsBar(statsBar, selectedStacks);
        buildCombineGrid(grid);

        combineHint.setText(Component.literal("\u70bc\u91d1\u8c03\u548c"));
        int successRate = computeSuccessRate(selectedStacks);
        successLabel.setText(Component.literal("\u5927\u6210\u529f\u6982\u7387"));
        successValue.setText(Component.literal(successRate + "%"));
        successFill.layout(layout -> layout.widthPercent(successRate));

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
        AlchemyEffectPanel.buildQualityBar(qualityBar, quality);

        Map<String, Integer> elementValues = computeElementValues(selectedStacks);
        AlchemyEffectPanel.buildEffectAttributes(recipe, elementValues, attributesScroller);

        return ModularUI.of(ui, player);
    }

    private static void populateSelectedList(UIElement container, List<ItemStack> stacks, Label hint) {
        container.clearAllChildren();
        if (stacks == null || stacks.isEmpty()) {
            hint.setText(Component.literal("\u5df2\u9009\u6750\u6599\u4e3a\u7a7a"));
            return;
        }
        hint.setText(Component.empty());

        var handler = createDisplayHandler(stacks);
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            var row = new UIElement().addClass("selected_row");
            var icon = new ItemSlot().bind(handler, i).addClass("selected_icon");
            var components = new UIElement().addClass("selected_components");

            AlchemyItemData data = stack.get(ModDataComponents.ALCHEMY_DATA.get());
            if (data == null || data.elements().isEmpty()) {
                components.addChild(new IngredientGridElement().setGridSize(3, 3).addClass("selected_component_grid"));
            } else {
                for (ElementComponent elementComponent : data.elements()) {
                    components.addChild(buildElementGrid(elementComponent));
                }
            }

            row.addChildren(icon, components);
            container.addChild(row);
        }
    }

    private static IngredientGridElement buildElementGrid(ElementComponent component) {
        var shape = component.shape();
        IngredientGridElement grid = new IngredientGridElement();
        grid.setGridSize(3, 3)
                .setElementColor(0xFF000000 | (component.element().getColor() & 0xFFFFFF));
        grid.addClass("selected_component_grid");
        for (int y = 0; y < Math.min(shape.getHeight(), 3); y++) {
            for (int x = 0; x < Math.min(shape.getWidth(), 3); x++) {
                CellType cellType = shape.getCellAt(x, y);
                IngredientGridElement.CellType displayType = switch (cellType) {
                    case NORMAL -> IngredientGridElement.CellType.NORMAL;
                    case LINK -> IngredientGridElement.CellType.LINK;
                    case EMPTY -> IngredientGridElement.CellType.EMPTY;
                };
                grid.setCell(x + 1, y + 1, displayType);
            }
        }
        return grid;
    }

    private static void buildStatsBar(UIElement statsBar, List<ItemStack> stacks) {
        statsBar.clearAllChildren();
        Map<String, Integer> values = computeElementValues(stacks);
        List<String> order = List.of("fire", "wind", "water", "light");
        for (String element : order) {
            var item = new UIElement().addClass("stat_item");
            var icon = new UIElement().addClass("stat_icon");
            ResourceLocation texture = AlchemyEffectPanel.resolveElementIcon(element);
            if (texture != null) {
                icon.lss("background", "sprite(" + texture + ")");
            } else {
                AlchemyElement elementColor = AlchemyElement.fromName(element);
                icon.lss("background", "rect(" + AlchemyEffectPanel.toHexColor(elementColor.getColor()) + ", 3)");
            }
            var value = new Label()
                    .setText(Component.literal(String.valueOf(values.getOrDefault(element, 0))))
                    .addClass("stat_value");
            item.addChildren(icon, value);
            statsBar.addChild(item);
        }
    }

    private static void buildCombineGrid(UIElement grid) {
        grid.clearAllChildren();
        for (int row = 0; row < 5; row++) {
            var rowElement = new UIElement().addClass("grid_row");
            for (int col = 0; col < 5; col++) {
                rowElement.addChild(new UIElement().addClass("grid_cell"));
            }
            grid.addChild(rowElement);
        }
    }

    private static int computeSuccessRate(List<ItemStack> stacks) {
        int total = computeElementValues(stacks).values().stream().mapToInt(Integer::intValue).sum();
        return Math.min(99, 10 + total);
    }

    private static Map<String, Integer> computeElementValues(List<ItemStack> stacks) {
        Map<String, Integer> values = new HashMap<>();
        if (stacks == null) {
            return values;
        }
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            int stackCount = stack.getCount();
            if (stackCount <= 0) {
                continue;
            }
            AlchemyItemData data = stack.get(ModDataComponents.ALCHEMY_DATA.get());
            if (data == null) {
                continue;
            }
            for (var component : data.elements()) {
                int amount = component.getNormalCount() + component.getLinkCount();
                if (amount <= 0) {
                    continue;
                }
                values.merge(component.element().getSerializedName(), amount * stackCount, Integer::sum);
            }
        }
        return values;
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

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        if (stacks == null) {
            return List.of();
        }
        List<ItemStack> copied = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                copied.add(stack.copy());
            }
        }
        return copied;
    }

    public static final class PendingCombine {
        private final ResourceLocation recipeId;
        private final List<ItemStack> selectedStacks;

        private PendingCombine(ResourceLocation recipeId, List<ItemStack> selectedStacks) {
            this.recipeId = recipeId;
            this.selectedStacks = selectedStacks == null ? List.of() : selectedStacks;
        }

        public ResourceLocation recipeId() {
            return recipeId;
        }

        public List<ItemStack> selectedStacks() {
            return selectedStacks;
        }
    }
}
