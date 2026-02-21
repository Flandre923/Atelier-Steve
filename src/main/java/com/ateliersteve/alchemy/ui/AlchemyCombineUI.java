package com.ateliersteve.alchemy.ui;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeDefinition;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeRegistry;
import com.ateliersteve.ui.StaticItemElement;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AlchemyCombineUI {
    private static final AlchemyCombineSessionStorage STORAGE = new AlchemyCombineSessionStorage();
    private static final int GRID_SIZE = 5;

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
        STORAGE.requestServer(player.getUUID(), recipe.id(), selectedStacks, reservedMaterials);
    }

    public static void requestOpenClient(Player player, AlchemyRecipeDefinition recipe, List<ItemStack> selectedStacks) {
        if (player == null || recipe == null) {
            return;
        }
        STORAGE.requestClient(player.getUUID(), recipe.id(), selectedStacks);
    }

    public static PendingCombine consumePendingCombine(Player player) {
        if (player == null) {
            return null;
        }
        return STORAGE.consumePending(player.getUUID(), player.level().isClientSide);
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

        AlchemyCombineBoard.State[] combineBoard = new AlchemyCombineBoard.State[]{new AlchemyCombineBoard.State(GRID_SIZE, GRID_SIZE)};
        boolean[] suppressBack = new boolean[]{false};
        Runnable[] refreshSelectedListRef = new Runnable[1];
        Runnable[] renderGridRef = new Runnable[1];
        Runnable[] refreshComputedRef = new Runnable[1];

        onSessionStateChanged(player, sessionTimeline.current());

        RPCEvent syncBoardRpc = RPCEventBuilder.simple(Integer[].class, payload -> {
            if (!(player instanceof ServerPlayer serverPlayer) || payload == null) {
                return;
            }
            STORAGE.storeServerBoard(serverPlayer.getUUID(), payload);
        });
        grid.addRPCEvent(syncBoardRpc);

        RPCEvent syncSessionRpc = RPCEventBuilder.simple(Integer[].class, payload -> {
            if (!(player instanceof ServerPlayer serverPlayer) || payload == null) {
                return;
            }
            AlchemyCombineSessionSnapshot synced = AlchemyCombineSessionSnapshot.fromSyncPayload(initialSnapshot, payload, GRID_SIZE);
            sessionTimeline.sync(synced);
            STORAGE.storeServerSession(serverPlayer.getUUID(), synced);
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
            Integer[] payload = combineBoard[0].writePayload();
            if (player.level().isClientSide) {
                STORAGE.storeBoard(player.getUUID(), true, payload);
                grid.sendEvent(syncBoardRpc, (Object) payload);
                grid.sendEvent(syncSessionRpc, (Object) sessionTimeline.current().toSyncPayload());
            } else {
                STORAGE.storeBoard(player.getUUID(), false, payload);
                STORAGE.storeSession(player.getUUID(), false, sessionTimeline.current());
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
            if (e.button != 1 || !(player instanceof ServerPlayer)) {
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

        ItemStack resultStack = AlchemyCombineStats.resolveResultStack(recipe);
        combineTitle.setText(resultStack.isEmpty()
                ? Component.literal(recipe == null ? "No Recipe" : recipe.result().toString())
                : resultStack.getHoverName());

        refreshSelectedListRef[0] = () -> AlchemyCombineSelectedList.populate(
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

        AlchemyCombineBoard.View gridView = AlchemyCombineBoard.buildGrid(grid, new AlchemyCombineBoard.CellHandler() {
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
        }, GRID_SIZE);

        renderGridRef[0] = () -> {
            AlchemyCombineSessionSnapshot current = sessionTimeline.current();
            combineBoard[0].rebuild(current, GRID_SIZE);
            AlchemyCombineBoard.render(gridView, combineBoard[0]);
            syncBoardState.run();
            if (refreshComputedRef[0] != null) {
                refreshComputedRef[0].run();
            }
        };

        refreshSelectedListRef[0].run();
        renderGridRef[0].run();

        combineHint.setText(Component.translatable("ui.atelier_steve.alchemy_combine.title"));
        levelLabel.setText(Component.literal("LV"));
        usageLabel.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.usage_count"));
        craftLabel.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.craft_amount"));
        qualityLabel.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.quality"));

        setItemElementStack(previewItemSlot, resultStack);

        int level = AlchemyCombineStats.resolveLevel(recipe, resultStack);
        levelValue.setText(Component.literal(String.valueOf(level)));
        usageValue.setText(Component.literal("-"));
        craftValue.setText(Component.literal("1"));

        int quality = AlchemyCombineStats.resolveInitialQuality(resultStack);
        qualityValue.setText(quality <= 0 ? Component.literal("-") : Component.literal(String.valueOf(quality)));
        AlchemyEffectPanel.buildQualityBar(qualityBar, quality);

        refreshComputedRef[0] = () -> {
            Map<String, Integer> values = AlchemyCombineStats.computeCombinedElementValues(sessionTimeline.current(), recipe);
            AlchemyCombineStats.buildStatsBar(statsBar, values);
            int successRate = AlchemyCombineStats.computeSuccessRate(values);
            successLabel.setText(Component.translatable("ui.atelier_steve.alchemy_combine.success_rate"));
            successValue.setText(Component.literal(successRate + "%"));
            successFill.layout(layout -> layout.widthPercent(successRate));
            int quantity = AlchemyCombineStats.computePlaceholderQuantity(values);
            craftValue.setText(Component.literal(String.valueOf(quantity)));
            int computedQuality = AlchemyCombineStats.computeCombinedQuality(sessionTimeline.current());
            qualityValue.setText(computedQuality <= 0
                    ? Component.literal("-")
                    : Component.literal(String.valueOf(computedQuality)));
            AlchemyEffectPanel.buildQualityBar(qualityBar, computedQuality);
            AlchemyEffectPanel.buildEffectAttributes(recipe, values, attributesScroller);
        };
        refreshComputedRef[0].run();

        return ModularUI.of(ui, player);
    }

    private static void setItemElementStack(UIElement element, ItemStack stack) {
        element.clearAllChildren();
        element.addChild(new StaticItemElement().setStack(stack)
                .lss("width", "100%")
                .lss("height", "100%"));
    }

    private static void onSessionStateChanged(Player player, AlchemyCombineSessionSnapshot snapshot) {
        if (player == null || snapshot == null) {
            return;
        }
        STORAGE.storeSession(player.getUUID(), player.level().isClientSide, snapshot);
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

    public static final class PendingCombine {
        private final ResourceLocation recipeId;
        private final List<ItemStack> selectedStacks;
        private final List<ReservedMaterialRef> reservedMaterials;

        PendingCombine(ResourceLocation recipeId, List<ItemStack> selectedStacks, List<ReservedMaterialRef> reservedMaterials) {
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
