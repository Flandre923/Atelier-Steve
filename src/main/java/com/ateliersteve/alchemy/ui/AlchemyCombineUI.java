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
import com.ateliersteve.ui.ElementCellTileElement;
import com.ateliersteve.ui.ElementCellTilePalette;
import com.ateliersteve.ui.ElementCellTileSpec;
import com.ateliersteve.ui.IngredientGridElement;
import com.ateliersteve.registry.ModDataComponents;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEvent;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AlchemyCombineUI {
    private static final Map<UUID, PendingCombine> PENDING_COMBINE_SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingCombine> PENDING_COMBINE_CLIENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer[]> COMBINE_BOARD_SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer[]> COMBINE_BOARD_CLIENT = new ConcurrentHashMap<>();
    private static final int GRID_SIZE = 5;
    private static final float PREVIEW_ALPHA = 0.55f;
    private static final float INVALID_PREVIEW_ALPHA = 0.6f;

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
        List<ItemStack> ingredientStacks = selectedStacks == null ? List.of() : new ArrayList<>(selectedStacks);

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

        Map<Integer, Integer> usedCounts = new HashMap<>();
        List<PlacedIngredient> placedIngredients = new ArrayList<>();
        CombineBoardState[] combineBoard = new CombineBoardState[]{new CombineBoardState(GRID_SIZE, GRID_SIZE)};
        SelectedIngredient[] selectedIngredient = new SelectedIngredient[1];
        CombineState[] combineState = new CombineState[]{CombineState.IDLE};
        int[] previewOrigin = new int[]{-1, -1};
        boolean[] suppressBack = new boolean[]{false};
        Runnable[] refreshSelectedListRef = new Runnable[1];
        Runnable[] renderGridRef = new Runnable[1];

        RPCEvent syncBoardRpc = RPCEventBuilder.simple(Integer[].class, payload -> {
            if (!(player instanceof ServerPlayer serverPlayer) || payload == null) {
                return;
            }
            COMBINE_BOARD_SERVER.put(serverPlayer.getUUID(), payload);
        });
        grid.addRPCEvent(syncBoardRpc);

        Runnable syncBoardState = () -> {
            Integer[] payload = combineBoard[0].toPayload();
            if (player.level().isClientSide) {
                COMBINE_BOARD_CLIENT.put(player.getUUID(), payload);
                grid.sendEvent(syncBoardRpc, (Object) payload);
            } else {
                COMBINE_BOARD_SERVER.put(player.getUUID(), payload);
            }
        };

        root.setFocusable(true);
        root.focus();
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> root.focus());
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 1) {
                return;
            }
            if (combineState[0] == CombineState.PREVIEWING) {
                suppressBack[0] = true;
                handleRightClick(-1, -1, selectedIngredient, placedIngredients, usedCounts, combineState, previewOrigin, renderGridRef, refreshSelectedListRef);
                return;
            }
            if (suppressBack[0]) {
                suppressBack[0] = false;
                return;
            }
            if (recipe != null) {
                AlchemyMaterialSelectionUI.requestOpenClient(player, recipe);
            }
        });
        root.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 1 || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (combineState[0] == CombineState.PREVIEWING || suppressBack[0]) {
                suppressBack[0] = false;
                return;
            }
            if (recipe != null) {
                AlchemyMaterialSelectionUI.requestOpen(serverPlayer, recipe);
            }
            serverPlayer.closeContainer();
            serverPlayer.getServer().execute(() -> BlockUIMenuType.openUI(serverPlayer, cauldronPos));
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


        refreshSelectedListRef[0] = () -> populateSelectedList(
                selectedList,
                ingredientStacks,
                usedCounts,
                selectedHint,
                selectedIngredient[0] == null ? null : selectedIngredient[0].sourceIndex(),
                (index, stack) -> {
                    if (index < 0 || index >= ingredientStacks.size()) {
                        return;
                    }
                    ItemStack currentStack = ingredientStacks.get(index);
                    if (currentStack == null || currentStack.isEmpty()) {
                        return;
                    }
                    int used = usedCounts.getOrDefault(index, 0);
                    if (used >= currentStack.getCount()) {
                        return;
                    }
                    SelectedIngredient candidate = buildSelectedIngredient(index, currentStack);
                    if (candidate == null || candidate.cells().isEmpty()) {
                        return;
                    }
                    selectedIngredient[0] = candidate;
                    combineState[0] = CombineState.PREVIEWING;
                    previewOrigin[0] = -1;
                    previewOrigin[1] = -1;
                    if (renderGridRef[0] != null) {
                        renderGridRef[0].run();
                    }
                    refreshSelectedListRef[0].run();
                }
        );

        buildStatsBar(statsBar, List.of());
        CombineGridView gridView = buildCombineGrid(grid, new CellHandler() {
            @Override
            public void onHover(int x, int y) {
                if (combineState[0] != CombineState.PREVIEWING || selectedIngredient[0] == null) {
                    return;
                }
                if (previewOrigin[0] == x && previewOrigin[1] == y) {
                    return;
                }
                previewOrigin[0] = x;
                previewOrigin[1] = y;
                if (renderGridRef[0] != null) {
                    renderGridRef[0].run();
                }
            }

            @Override
            public void onClick(int x, int y, int button) {
                if (button == 0) {
                    handleLeftClick(x, y, selectedIngredient, placedIngredients, usedCounts, combineState, previewOrigin, renderGridRef, refreshSelectedListRef);
                } else if (button == 1) {
                    suppressBack[0] = true;
                    handleRightClick(x, y, selectedIngredient, placedIngredients, usedCounts, combineState, previewOrigin, renderGridRef, refreshSelectedListRef);
                }
            }
        });

        renderGridRef[0] = () -> {
            SelectedIngredient current = selectedIngredient[0];
            boolean previewing = combineState[0] == CombineState.PREVIEWING && current != null && previewOrigin[0] >= 0;
            boolean valid = previewing && isPlacementValid(previewOrigin[0], previewOrigin[1], current.cells(), placedIngredients);
            combineBoard[0].rebuild(placedIngredients, current, previewOrigin[0], previewOrigin[1], previewing, valid);
            renderCombineGrid(gridView, combineBoard[0]);
            syncBoardState.run();
        };

        refreshSelectedListRef[0].run();
        renderGridRef[0].run();


        combineHint.setText(Component.literal("\u70bc\u91d1\u8c03\u548c"));
        int successRate = computeSuccessRate(List.of());
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

        AlchemyEffectPanel.buildEffectAttributes(recipe, Map.of(), attributesScroller);

        return ModularUI.of(ui, player);
    }

    private static void populateSelectedList(
            UIElement container,
            List<ItemStack> stacks,
            Map<Integer, Integer> usedCounts,
            Label hint,
            Integer selectedIndex,
            ItemSelectHandler onSelect
    ) {
        container.clearAllChildren();
        if (stacks == null || stacks.isEmpty()) {
            hint.setText(Component.literal("\u5df2\u9009\u6750\u6599\u4e3a\u7a7a"));
            return;
        }

        int availableCount = 0;
        for (int i = 0; i < stacks.size(); i++) {
            if (usedCounts != null && usedCounts.getOrDefault(i, 0) >= stacks.get(i).getCount()) {
                continue;
            }
            availableCount++;
        }
        if (availableCount <= 0) {
            hint.setText(Component.literal("\u5df2\u9009\u6750\u6599\u5df2\u7528\u5b8c"));
            return;
        }
        hint.setText(Component.empty());

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            int used = usedCounts == null ? 0 : usedCounts.getOrDefault(i, 0);
            int remaining = Math.max(0, stack.getCount() - used);
            boolean exhausted = usedCounts != null && usedCounts.getOrDefault(i, 0) >= stack.getCount();
            var row = new UIElement().addClass("selected_row");
            if (selectedIndex != null && selectedIndex == i) {
                row.addClass("selected");
            }
            if (exhausted) {
                row.addClass("disabled");
                row.lss("opacity", "0.45");
            }
            var name = new Label()
                    .setText(Component.literal(stack.getHoverName().getString() + " x" + remaining))
                    .addClass("selected_name");
            var components = new UIElement().addClass("selected_components");

            AlchemyItemData data = stack.get(ModDataComponents.ALCHEMY_DATA.get());
            if (data == null || data.elements().isEmpty()) {
                components.addChild(new IngredientGridElement().setGridSize(3, 3).addClass("selected_component_grid"));
            } else {
                for (ElementComponent elementComponent : data.elements()) {
                    components.addChild(buildElementGrid(elementComponent));
                }
            }

            row.addChildren(name, components);
            container.addChild(row);

            if (onSelect != null && !exhausted) {
                int index = i;
                row.addEventListener(UIEvents.CLICK, e -> onSelect.handle(index, stack));
                name.addEventListener(UIEvents.CLICK, e -> onSelect.handle(index, stack));
                components.addEventListener(UIEvents.CLICK, e -> onSelect.handle(index, stack));
            }
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
        List<String> order = List.of("fire", "ice", "thunder", "wind", "light");
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

    private static CombineGridView buildCombineGrid(UIElement grid, CellHandler handler) {
        grid.clearAllChildren();
        var content = new UIElement().addClass("combine_grid_content");
        ElementCellTileElement[][] cells = new ElementCellTileElement[GRID_SIZE][GRID_SIZE];
        for (int row = 0; row < GRID_SIZE; row++) {
            var rowElement = new UIElement().addClass("combine_grid_row");
            for (int col = 0; col < GRID_SIZE; col++) {
                ElementCellTileElement cell = new ElementCellTileElement();
                cell.addClass("combine_grid_cell");
                int x = col;
                int y = row;
                if (handler != null) {
                    cell.addEventListener(UIEvents.MOUSE_MOVE, e -> handler.onHover(x, y));
                    cell.addEventListener(UIEvents.MOUSE_DOWN, e -> handler.onClick(x, y, e.button));
                }
                rowElement.addChild(cell);
                cells[row][col] = cell;
            }
            content.addChild(rowElement);
        }
        grid.addChild(content);
        return new CombineGridView(cells);
    }

    private static void handleLeftClick(
            int x,
            int y,
            SelectedIngredient[] selectedIngredient,
            List<PlacedIngredient> placedIngredients,
            Map<Integer, Integer> usedCounts,
            CombineState[] combineState,
            int[] previewOrigin,
            Runnable[] renderGridRef,
            Runnable[] refreshSelectedListRef
    ) {
        if (combineState[0] != CombineState.PREVIEWING || selectedIngredient[0] == null) {
            return;
        }
        SelectedIngredient selected = selectedIngredient[0];
        if (!isPlacementValid(x, y, selected.cells(), placedIngredients)) {
            return;
        }
        placedIngredients.add(new PlacedIngredient(selected.sourceIndex(), selected.stack().copy(), selected.cells(), x, y, 0));
        usedCounts.merge(selected.sourceIndex(), 1, Integer::sum);
        selectedIngredient[0] = null;
        combineState[0] = CombineState.PLACED;
        previewOrigin[0] = -1;
        previewOrigin[1] = -1;
        if (refreshSelectedListRef[0] != null) {
            refreshSelectedListRef[0].run();
        }
        if (renderGridRef[0] != null) {
            renderGridRef[0].run();
        }
    }

    private static void handleRightClick(
            int x,
            int y,
            SelectedIngredient[] selectedIngredient,
            List<PlacedIngredient> placedIngredients,
            Map<Integer, Integer> usedCounts,
            CombineState[] combineState,
            int[] previewOrigin,
            Runnable[] renderGridRef,
            Runnable[] refreshSelectedListRef
    ) {
        if (combineState[0] == CombineState.PREVIEWING) {
            selectedIngredient[0] = null;
            combineState[0] = CombineState.IDLE;
            previewOrigin[0] = -1;
            previewOrigin[1] = -1;
            if (refreshSelectedListRef[0] != null) {
                refreshSelectedListRef[0].run();
            }
            if (renderGridRef[0] != null) {
                renderGridRef[0].run();
            }
            return;
        }
        PlacedIngredient target = findPlacedIngredientAt(x, y, placedIngredients);
        if (target == null) {
            return;
        }
        placedIngredients.remove(target);
        int remaining = usedCounts.getOrDefault(target.sourceIndex(), 0) - 1;
        if (remaining <= 0) {
            usedCounts.remove(target.sourceIndex());
        } else {
            usedCounts.put(target.sourceIndex(), remaining);
        }
        combineState[0] = CombineState.IDLE;
        if (refreshSelectedListRef[0] != null) {
            refreshSelectedListRef[0].run();
        }
        if (renderGridRef[0] != null) {
            renderGridRef[0].run();
        }
    }

    private static SelectedIngredient buildSelectedIngredient(int index, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        List<IngredientCell> cells = buildIngredientCells(stack);
        if (cells.isEmpty()) {
            return null;
        }
        return new SelectedIngredient(index, stack.copy(), cells);
    }

    private static List<IngredientCell> buildIngredientCells(ItemStack stack) {
        List<IngredientCell> cells = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return cells;
        }
        AlchemyItemData data = stack.get(ModDataComponents.ALCHEMY_DATA.get());
        if (data == null || data.elements().isEmpty()) {
            return cells;
        }
        for (ElementComponent component : data.elements()) {
            if (component == null || component.shape() == null) {
                continue;
            }
            AlchemyElement element = component.element();
            var shape = component.shape();
            for (int y = 0; y < shape.getHeight(); y++) {
                for (int x = 0; x < shape.getWidth(); x++) {
                    CellType cellType = shape.getCellAt(x, y);
                    if (cellType == CellType.EMPTY) {
                        continue;
                    }
                    cells.add(new IngredientCell(x, y, element, cellType));
                }
            }
        }
        return cells;
    }

    private static boolean isPlacementValid(int originX, int originY, List<IngredientCell> cells, List<PlacedIngredient> placed) {
        if (cells == null || cells.isEmpty()) {
            return false;
        }
        for (IngredientCell cell : cells) {
            int gridX = originX + cell.offsetX();
            int gridY = originY + cell.offsetY();
            if (!isInsideGrid(gridX, gridY)) {
                return false;
            }
            if (isCellOccupied(gridX, gridY, placed)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCellOccupied(int x, int y, List<PlacedIngredient> placed) {
        if (placed == null || placed.isEmpty()) {
            return false;
        }
        for (PlacedIngredient ingredient : placed) {
            for (IngredientCell cell : ingredient.cells()) {
                int gridX = ingredient.originX() + cell.offsetX();
                int gridY = ingredient.originY() + cell.offsetY();
                if (gridX == x && gridY == y) {
                    return true;
                }
            }
        }
        return false;
    }

    private static PlacedIngredient findPlacedIngredientAt(int x, int y, List<PlacedIngredient> placed) {
        if (placed == null || placed.isEmpty()) {
            return null;
        }
        for (PlacedIngredient ingredient : placed) {
            for (IngredientCell cell : ingredient.cells()) {
                int gridX = ingredient.originX() + cell.offsetX();
                int gridY = ingredient.originY() + cell.offsetY();
                if (gridX == x && gridY == y) {
                    return ingredient;
                }
            }
        }
        return null;
    }

    private static boolean isInsideGrid(int x, int y) {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
    }

    private static void renderCombineGrid(
            CombineGridView gridView,
            CombineBoardState boardState
    ) {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                ElementCellTileElement cell = gridView.cellAt(x, y);
                CombineBoardCellState state = boardState.cellAt(x, y);
                cell.applySpec(state.spec());
                cell.lss("opacity", String.valueOf(state.opacity()));
            }
        }
    }

    private static int computeSuccessRate(List<ItemStack> stacks) {
        int total = computeElementValues(stacks).values().stream().mapToInt(Integer::intValue).sum();
        return Math.min(99, total);
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

    private interface ItemSelectHandler {
        void handle(int index, ItemStack stack);
    }

    private interface CellHandler {
        void onHover(int x, int y);

        void onClick(int x, int y, int button);
    }

    private enum CombineState {
        IDLE,
        PREVIEWING,
        PLACED
    }

    private record IngredientCell(int offsetX, int offsetY, AlchemyElement element, CellType cellType) {
    }

    private record SelectedIngredient(int sourceIndex, ItemStack stack, List<IngredientCell> cells) {
    }

    private record PlacedIngredient(
            int sourceIndex,
            ItemStack stack,
            List<IngredientCell> cells,
            int originX,
            int originY,
            int rotation
    ) {
    }

    private static final class CombineGridView {
        private final ElementCellTileElement[][] cells;

        private CombineGridView(ElementCellTileElement[][] cells) {
            this.cells = cells;
        }

        private ElementCellTileElement cellAt(int x, int y) {
            return cells[y][x];
        }
    }

    private static final class CombineBoardState {
        private final int width;
        private final int height;
        private final CombineBoardCellState[][] cells;

        private CombineBoardState(int width, int height) {
            this.width = width;
            this.height = height;
            this.cells = new CombineBoardCellState[height][width];
            reset();
        }

        private void rebuild(
                List<PlacedIngredient> placedIngredients,
                SelectedIngredient selectedIngredient,
                int previewX,
                int previewY,
                boolean previewing,
                boolean previewValid
        ) {
            reset();
            applyPlaced(placedIngredients);
            if (previewing && selectedIngredient != null) {
                applyPreview(selectedIngredient, previewX, previewY, previewValid);
            }
        }

        private CombineBoardCellState cellAt(int x, int y) {
            return cells[y][x];
        }

        private Integer[] toPayload() {
            Integer[] payload = new Integer[width * height];
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    payload[index++] = cells[y][x].encode();
                }
            }
            return payload;
        }

        private void reset() {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    cells[y][x] = CombineBoardCellState.empty();
                }
            }
        }

        private void applyPlaced(List<PlacedIngredient> placedIngredients) {
            if (placedIngredients == null || placedIngredients.isEmpty()) {
                return;
            }
            for (PlacedIngredient ingredient : placedIngredients) {
                for (IngredientCell cell : ingredient.cells()) {
                    int gridX = ingredient.originX() + cell.offsetX();
                    int gridY = ingredient.originY() + cell.offsetY();
                    if (!isInsideGrid(gridX, gridY)) {
                        continue;
                    }
                    cells[gridY][gridX] = CombineBoardCellState.placed(cell.element(), cell.cellType() == CellType.LINK);
                }
            }
        }

        private void applyPreview(SelectedIngredient selectedIngredient, int previewX, int previewY, boolean previewValid) {
            for (IngredientCell cell : selectedIngredient.cells()) {
                int gridX = previewX + cell.offsetX();
                int gridY = previewY + cell.offsetY();
                if (!isInsideGrid(gridX, gridY)) {
                    continue;
                }
                cells[gridY][gridX] = previewValid
                        ? CombineBoardCellState.preview(cell.element(), cell.cellType() == CellType.LINK)
                        : CombineBoardCellState.invalidPreview();
            }
        }
    }

    private record CombineBoardCellState(
            ElementCellTileSpec spec,
            float opacity,
            AlchemyElement element,
            boolean link,
            boolean preview,
            boolean invalid
    ) {
        private static CombineBoardCellState empty() {
            return new CombineBoardCellState(ElementCellTilePalette.empty(), 1.0f, null, false, false, false);
        }

        private static CombineBoardCellState placed(AlchemyElement element, boolean link) {
            return new CombineBoardCellState(ElementCellTilePalette.filled(element, link), 1.0f, element, link, false, false);
        }

        private static CombineBoardCellState preview(AlchemyElement element, boolean link) {
            return new CombineBoardCellState(ElementCellTilePalette.filled(element, link), PREVIEW_ALPHA, element, link, true, false);
        }

        private static CombineBoardCellState invalidPreview() {
            return new CombineBoardCellState(ElementCellTilePalette.disabled(), INVALID_PREVIEW_ALPHA, null, false, true, true);
        }

        private int encode() {
            if (invalid) {
                return -1;
            }
            if (element == null) {
                return 0;
            }
            int code = element.ordinal() + 1;
            if (link) {
                code |= 1 << 4;
            }
            if (preview) {
                code |= 1 << 5;
            }
            return code;
        }
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
