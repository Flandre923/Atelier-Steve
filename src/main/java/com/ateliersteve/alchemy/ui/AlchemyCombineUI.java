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
import com.ateliersteve.ui.StaticItemElement;
import com.ateliersteve.registry.ModDataComponents;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AlchemyCombineUI {
    private static final Map<UUID, PendingCombine> PENDING_COMBINE_SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingCombine> PENDING_COMBINE_CLIENT = new ConcurrentHashMap<>();
    private static final Map<UUID, AlchemyCombineSessionSnapshot> COMBINE_SESSION_SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, AlchemyCombineSessionSnapshot> COMBINE_SESSION_CLIENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer[]> COMBINE_BOARD_SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer[]> COMBINE_BOARD_CLIENT = new ConcurrentHashMap<>();
    private static final int GRID_SIZE = 5;
    private static final float PREVIEW_ALPHA = 0.55f;
    private static final float INVALID_PREVIEW_ALPHA = 0.6f;

    private AlchemyCombineUI() {
    }

    public static void requestOpen(ServerPlayer player, AlchemyRecipeDefinition recipe, List<ItemStack> selectedStacks) {
        requestOpen(player, recipe, selectedStacks, List.of());
    }

    public static void requestOpen(
            ServerPlayer player,
            AlchemyRecipeDefinition recipe,
            List<ItemStack> selectedStacks,
            List<ReservedMaterialRef> reservedMaterials
    ) {
        if (player == null || recipe == null) {
            return;
        }
        PENDING_COMBINE_SERVER.put(player.getUUID(), new PendingCombine(recipe.id(), copyStacks(selectedStacks), copyReservedMaterials(reservedMaterials)));
    }

    public static void requestOpenClient(Player player, AlchemyRecipeDefinition recipe, List<ItemStack> selectedStacks) {
        if (player == null || recipe == null) {
            return;
        }
        PENDING_COMBINE_CLIENT.put(player.getUUID(), new PendingCombine(recipe.id(), copyStacks(selectedStacks), List.of()));
    }

    public static PendingCombine consumePendingCombine(Player player) {
        if (player == null) {
            return null;
        }
        UUID playerId = player.getUUID();
        boolean isClient = player.level().isClientSide;
        Map<UUID, PendingCombine> primary = isClient ? PENDING_COMBINE_CLIENT : PENDING_COMBINE_SERVER;
        PendingCombine pending = primary.remove(playerId);
        if (pending == null) {
            Map<UUID, PendingCombine> fallback = isClient ? PENDING_COMBINE_SERVER : PENDING_COMBINE_CLIENT;
            pending = fallback.get(playerId);
        }
        return pending;
    }

    public static ModularUI createUI(Player player, BlockPos cauldronPos, PendingCombine pending) {
        var xml = XmlUtils.loadXml(AtelierSteve.id("alchemy_combine.xml"));
        if (xml == null) {
            return ModularUI.of(UI.of(new UIElement()), player);
        }

        AlchemyRecipeDefinition recipe = pending == null ? null : AlchemyRecipeRegistry.findById(pending.recipeId());
        List<ItemStack> selectedStacks = pending == null ? List.of() : pending.selectedStacks();
        List<ItemStack> ingredientStacks = selectedStacks == null ? List.of() : new ArrayList<>(selectedStacks);
        AlchemyCombineSessionSnapshot initialSnapshot = AlchemyCombineSessionSnapshot.fromStacks(ingredientStacks);
        AlchemyCombineSessionSnapshot.Timeline sessionTimeline = new AlchemyCombineSessionSnapshot.Timeline(initialSnapshot);

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

        CombineBoardState[] combineBoard = new CombineBoardState[]{new CombineBoardState(GRID_SIZE, GRID_SIZE)};
        boolean[] suppressBack = new boolean[]{false};
        Runnable[] refreshSelectedListRef = new Runnable[1];
        Runnable[] renderGridRef = new Runnable[1];

        if (player.level().isClientSide) {
            COMBINE_SESSION_CLIENT.put(player.getUUID(), sessionTimeline.current());
        } else {
            COMBINE_SESSION_SERVER.put(player.getUUID(), sessionTimeline.current());
        }

        RPCEvent syncBoardRpc = RPCEventBuilder.simple(Integer[].class, payload -> {
            if (!(player instanceof ServerPlayer serverPlayer) || payload == null) {
                return;
            }
            COMBINE_BOARD_SERVER.put(serverPlayer.getUUID(), payload);
        });
        grid.addRPCEvent(syncBoardRpc);

        RPCEvent syncSessionRpc = RPCEventBuilder.simple(Integer[].class, payload -> {
            if (!(player instanceof ServerPlayer serverPlayer) || payload == null) {
                return;
            }
            AlchemyCombineSessionSnapshot synced = AlchemyCombineSessionSnapshot.fromSyncPayload(initialSnapshot, payload, GRID_SIZE);
            sessionTimeline.sync(synced);
            COMBINE_SESSION_SERVER.put(serverPlayer.getUUID(), synced);
        });
        grid.addRPCEvent(syncSessionRpc);

        RPCEvent openSelectionRpc = RPCEventBuilder.simple(Boolean.class, payload -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            returnReservedMaterials(serverPlayer, pending == null ? List.of() : pending.reservedMaterials());
            serverPlayer.closeContainer();
            serverPlayer.getServer().execute(() -> BlockUIMenuType.openUI(serverPlayer, cauldronPos));
        });
        grid.addRPCEvent(openSelectionRpc);

        Runnable syncBoardState = () -> {
            Integer[] payload = combineBoard[0].toPayload();
            if (player.level().isClientSide) {
                COMBINE_BOARD_CLIENT.put(player.getUUID(), payload);
                grid.sendEvent(syncBoardRpc, (Object) payload);
                grid.sendEvent(syncSessionRpc, (Object) sessionTimeline.current().toSyncPayload());
            } else {
                COMBINE_BOARD_SERVER.put(player.getUUID(), payload);
                COMBINE_SESSION_SERVER.put(player.getUUID(), sessionTimeline.current());
            }
        };

        root.setFocusable(true);
        root.focus();
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> root.focus());
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 1) {
                return;
            }
            if (suppressBack[0]) {
                suppressBack[0] = false;
                return;
            }
            if (sessionTimeline.current().isPreviewing()) {
                if (sessionTimeline.apply(AlchemyCombineSessionSnapshot::cancelSelection)) {
                    suppressBack[0] = true;
                    onSessionStateChanged(player, sessionTimeline.current());
                }
                rerender(renderGridRef, refreshSelectedListRef);
                return;
            }
            if (sessionTimeline.apply(AlchemyCombineSessionSnapshot::removeLastPlaced)) {
                suppressBack[0] = true;
                onSessionStateChanged(player, sessionTimeline.current());
                rerender(renderGridRef, refreshSelectedListRef);
                return;
            }
            grid.sendEvent(openSelectionRpc, Boolean.TRUE);
        });
        root.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 1 || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (suppressBack[0]) {
                suppressBack[0] = false;
                return;
            }
            if (sessionTimeline.current().isPreviewing()) {
                if (sessionTimeline.apply(AlchemyCombineSessionSnapshot::cancelSelection)) {
                    onSessionStateChanged(player, sessionTimeline.current());
                }
                rerender(renderGridRef, refreshSelectedListRef);
                return;
            }
            if (sessionTimeline.apply(AlchemyCombineSessionSnapshot::removeLastPlaced)) {
                onSessionStateChanged(player, sessionTimeline.current());
                rerender(renderGridRef, refreshSelectedListRef);
                return;
            }
        });
        root.addServerEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ESCAPE && player instanceof ServerPlayer serverPlayer) {
                returnReservedMaterials(serverPlayer, pending == null ? List.of() : pending.reservedMaterials());
                serverPlayer.closeContainer();
                serverPlayer.getServer().execute(() -> BlockUIMenuType.openUI(serverPlayer, cauldronPos));
            }
        });
        root.addEventListener(UIEvents.KEY_DOWN, e -> {
            if (!sessionTimeline.current().isPreviewing()) {
                return;
            }
            boolean changed = false;
            if (e.keyCode == GLFW.GLFW_KEY_R) {
                changed = sessionTimeline.apply(AlchemyCombineSessionSnapshot::rotateSelected);
            } else if (e.keyCode == GLFW.GLFW_KEY_F) {
                changed = sessionTimeline.apply(AlchemyCombineSessionSnapshot::flipSelected);
            }
            if (changed) {
                onSessionStateChanged(player, sessionTimeline.current());
                rerender(renderGridRef, refreshSelectedListRef);
            }
        });
        ItemStack resultStack = resolveResultStack(recipe);
        combineTitle.setText(resultStack.isEmpty()
                ? Component.literal(recipe == null ? "No Recipe" : recipe.result().toString())
                : resultStack.getHoverName());


        refreshSelectedListRef[0] = () -> populateSelectedList(
                selectedList,
                sessionTimeline.current(),
                selectedHint,
                sessionTimeline.current().selectedComponentId(),
                (materialId, componentId) -> {
                    if (sessionTimeline.apply(state -> state.selectComponent(materialId, componentId))) {
                        onSessionStateChanged(player, sessionTimeline.current());
                    }
                    rerender(renderGridRef, refreshSelectedListRef);
                }
        );

        buildStatsBar(statsBar, List.of());
        CombineGridView gridView = buildCombineGrid(grid, new CellHandler() {
            @Override
            public void onHover(int x, int y) {
                if (sessionTimeline.apply(state -> state.hover(x, y))) {
                    onSessionStateChanged(player, sessionTimeline.current());
                    if (renderGridRef[0] != null) {
                        renderGridRef[0].run();
                    }
                }
            }

            @Override
            public void onClick(int x, int y, int button) {
                if (button == 0) {
                    if (sessionTimeline.apply(state -> state.placeSelectedAt(x, y, GRID_SIZE))) {
                        onSessionStateChanged(player, sessionTimeline.current());
                    }
                    rerender(renderGridRef, refreshSelectedListRef);
                } else if (button == 1) {
                    boolean changed;
                    if (sessionTimeline.current().isPreviewing()) {
                        suppressBack[0] = true;
                        changed = sessionTimeline.apply(AlchemyCombineSessionSnapshot::cancelSelection);
                    } else {
                        changed = sessionTimeline.apply(AlchemyCombineSessionSnapshot::removeLastPlaced);
                        if (changed) {
                            suppressBack[0] = true;
                        }
                    }
                    if (changed) {
                        onSessionStateChanged(player, sessionTimeline.current());
                        rerender(renderGridRef, refreshSelectedListRef);
                    }
                }
            }
        });

        renderGridRef[0] = () -> {
            AlchemyCombineSessionSnapshot current = sessionTimeline.current();
            combineBoard[0].rebuild(current, GRID_SIZE);
            renderCombineGrid(gridView, combineBoard[0]);
            syncBoardState.run();
        };

        refreshSelectedListRef[0].run();
        renderGridRef[0].run();


        combineHint.setText(Component.translatable("ui.atelier_steve.alchemy_combine.title"));
        int successRate = computeSuccessRate(List.of());
        successLabel.setText(Component.translatable("ui.atelier_steve.alchemy_combine.success_rate"));
        successValue.setText(Component.literal(successRate + "%"));
        successFill.layout(layout -> layout.widthPercent(successRate));

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

        return ModularUI.of(ui, player);
    }

    private static void populateSelectedList(
            UIElement container,
            AlchemyCombineSessionSnapshot snapshot,
            Label hint,
            String selectedComponentId,
            ItemSelectHandler onSelect
    ) {
        container.clearAllChildren();
        if (snapshot == null || snapshot.materials().isEmpty()) {
            hint.setText(Component.translatable("ui.atelier_steve.alchemy_combine.materials_empty"));
            return;
        }

        int availableCount = 0;
        for (AlchemyCombineSessionSnapshot.MaterialEntry material : snapshot.materials()) {
            if (!snapshot.isExhausted(material.materialId())) {
                availableCount++;
            }
        }
        if (availableCount <= 0) {
            hint.setText(Component.translatable("ui.atelier_steve.alchemy_combine.materials_exhausted"));
        } else {
            hint.setText(Component.empty());
        }

        for (AlchemyCombineSessionSnapshot.MaterialEntry material : snapshot.materials()) {
            ItemStack stack = material.stack();
            boolean exhausted = snapshot.isExhausted(material.materialId());
            var row = new UIElement().addClass("selected_row");
            var info = new UIElement().addClass("row_align_center").addClass("selected_prefix");
            var icon = new StaticItemElement().setStack(stack).addClass("selected_icon");
            info.addChildren(icon);
            row.addChild(info);

            if (!exhausted) {
                var components = new UIElement().addClass("selected_components");

                if (material.components().isEmpty()) {
                    components.addChild(new IngredientGridElement().setGridSize(3, 3).addClass("selected_component_grid"));
                } else {
                    for (String componentId : material.components().keySet()) {
                        AlchemyCombineSessionSnapshot.MaterialComponentRef componentRef =
                                snapshot.getIngredientComponent(material.materialId(), componentId);
                        if (componentRef != null) {
                            IngredientGridElement componentGrid = buildElementGrid(componentRef.component());
                            boolean componentExhausted = snapshot.isComponentExhausted(componentId);
                            boolean selected = selectedComponentId != null && selectedComponentId.equals(componentId);
                            if (selected) {
                                componentGrid.addClass("selected");
                                row.addClass("selected");
                            }
                            if (componentExhausted) {
                                componentGrid.addClass("disabled");
                                componentGrid.lss("opacity", "0.45");
                            }
                            if (onSelect != null && !componentExhausted) {
                                String materialId = material.materialId();
                                componentGrid.addEventListener(UIEvents.CLICK, e -> onSelect.handle(materialId, componentId));
                            }
                            components.addChild(componentGrid);
                        }
                    }
                }
                row.addChild(components);
            }
            container.addChild(row);
        }
    }

    private static IngredientGridElement buildElementGrid(AlchemyCombineSessionSnapshot.MaterialComponentEntry component) {
        IngredientGridElement grid = new IngredientGridElement();
        grid.setGridSize(3, 3)
                .setElementColor(0xFF000000 | (component.element().getColor() & 0xFFFFFF));
        grid.addClass("selected_component_grid");
        for (AlchemyCombineSessionSnapshot.IngredientCell cell : component.cells()) {
            if (cell.offsetX() < 0 || cell.offsetX() >= 3 || cell.offsetY() < 0 || cell.offsetY() >= 3) {
                continue;
            }
            CellType cellType = cell.cellType();
                IngredientGridElement.CellType displayType = switch (cellType) {
                    case NORMAL -> IngredientGridElement.CellType.NORMAL;
                    case LINK -> IngredientGridElement.CellType.LINK;
                    case EMPTY -> IngredientGridElement.CellType.EMPTY;
                };
                grid.setCell(cell.offsetX() + 1, cell.offsetY() + 1, displayType);
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

    private static void setItemElementStack(UIElement element, ItemStack stack) {
        element.clearAllChildren();
        element.addChild(new StaticItemElement().setStack(stack)
                .lss("width", "100%")
                .lss("height", "100%"));
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

    private static List<ReservedMaterialRef> copyReservedMaterials(List<ReservedMaterialRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        List<ReservedMaterialRef> copied = new ArrayList<>(refs.size());
        for (ReservedMaterialRef ref : refs) {
            if (ref != null) {
                copied.add(new ReservedMaterialRef(ref.basketPos(), ref.slotIndex(), ref.stack().copy()));
            }
        }
        return copied;
    }

    private static void onSessionStateChanged(Player player, AlchemyCombineSessionSnapshot snapshot) {
        if (player == null || snapshot == null) {
            return;
        }
        if (player.level().isClientSide) {
            COMBINE_SESSION_CLIENT.put(player.getUUID(), snapshot);
        } else {
            COMBINE_SESSION_SERVER.put(player.getUUID(), snapshot);
        }
    }

    private static void rerender(Runnable[] renderGridRef, Runnable[] refreshSelectedListRef) {
        if (refreshSelectedListRef != null && refreshSelectedListRef.length > 0 && refreshSelectedListRef[0] != null) {
            refreshSelectedListRef[0].run();
        }
        if (renderGridRef != null && renderGridRef.length > 0 && renderGridRef[0] != null) {
            renderGridRef[0].run();
        }
    }

    private static void returnReservedMaterials(ServerPlayer player, List<ReservedMaterialRef> reservedMaterials) {
        if (player == null || reservedMaterials == null || reservedMaterials.isEmpty()) {
            return;
        }
        var level = player.level();
        for (ReservedMaterialRef ref : reservedMaterials) {
            if (ref == null || ref.stack() == null || ref.stack().isEmpty()) {
                continue;
            }
            if (!(level.getBlockEntity(ref.basketPos()) instanceof com.ateliersteve.block.GatheringBasketBlockEntity basket)) {
                continue;
            }
            ItemStackHandler inventory = basket.getInventory();
            if (inventory == null) {
                continue;
            }
            int slot = ref.slotIndex();
            if (slot < 0 || slot >= inventory.getSlots()) {
                continue;
            }
            ItemStack current = inventory.getStackInSlot(slot);
            if (current.isEmpty()) {
                inventory.setStackInSlot(slot, ref.stack().copy());
            }
        }
    }

    private interface ItemSelectHandler {
        void handle(String materialId, String componentId);
    }

    private interface CellHandler {
        void onHover(int x, int y);

        void onClick(int x, int y, int button);
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

        private void rebuild(AlchemyCombineSessionSnapshot snapshot, int gridSize) {
            reset();
            if (snapshot == null) {
                return;
            }
            applyPlaced(snapshot.placedMaterials(), gridSize);
            if (snapshot.isPreviewing() && snapshot.selectedComponentId() != null && snapshot.selectedMaterialId() != null) {
                var selected = snapshot.selectedPreviewComponent();
                if (selected != null && snapshot.previewX() >= 0 && snapshot.previewY() >= 0) {
                    applyPreview(selected, snapshot.previewX(), snapshot.previewY(), snapshot.isPreviewPlacementValid(gridSize), gridSize);
                }
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

        private void applyPlaced(List<AlchemyCombineSessionSnapshot.PlacedMaterial> placedIngredients, int gridSize) {
            if (placedIngredients == null || placedIngredients.isEmpty()) {
                return;
            }
            for (AlchemyCombineSessionSnapshot.PlacedMaterial ingredient : placedIngredients) {
                for (AlchemyCombineSessionSnapshot.IngredientCell cell : ingredient.cells()) {
                    int gridX = ingredient.originX() + cell.offsetX();
                    int gridY = ingredient.originY() + cell.offsetY();
                    if (!isInsideGrid(gridX, gridY, gridSize)) {
                        continue;
                    }
                    cells[gridY][gridX] = CombineBoardCellState.placed(cell.element(), cell.cellType() == CellType.LINK);
                }
            }
        }

        private void applyPreview(
                AlchemyCombineSessionSnapshot.MaterialComponentEntry selectedIngredient,
                int previewX,
                int previewY,
                boolean previewValid,
                int gridSize
        ) {
            for (AlchemyCombineSessionSnapshot.IngredientCell cell : selectedIngredient.cells()) {
                int gridX = previewX + cell.offsetX();
                int gridY = previewY + cell.offsetY();
                if (!isInsideGrid(gridX, gridY, gridSize)) {
                    continue;
                }
                cells[gridY][gridX] = previewValid
                        ? CombineBoardCellState.preview(cell.element(), cell.cellType() == CellType.LINK)
                        : CombineBoardCellState.invalidPreview();
            }
        }

        private boolean isInsideGrid(int x, int y, int gridSize) {
            return x >= 0 && x < gridSize && y >= 0 && y < gridSize;
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
            float alpha = link ? 0.8f : PREVIEW_ALPHA;
            return new CombineBoardCellState(ElementCellTilePalette.preview(element, link), alpha, element, link, true, false);
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
        private final List<ReservedMaterialRef> reservedMaterials;

        private PendingCombine(ResourceLocation recipeId, List<ItemStack> selectedStacks, List<ReservedMaterialRef> reservedMaterials) {
            this.recipeId = recipeId;
            this.selectedStacks = selectedStacks == null ? List.of() : selectedStacks;
            this.reservedMaterials = reservedMaterials == null ? List.of() : reservedMaterials;
        }

        public ResourceLocation recipeId() {
            return recipeId;
        }

        public List<ItemStack> selectedStacks() {
            return selectedStacks;
        }

        public List<ReservedMaterialRef> reservedMaterials() {
            return reservedMaterials;
        }
    }

    public record ReservedMaterialRef(BlockPos basketPos, int slotIndex, ItemStack stack) {
    }
}
