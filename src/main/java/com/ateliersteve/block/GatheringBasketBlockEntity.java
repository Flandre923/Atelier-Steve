package com.ateliersteve.block;

import com.ateliersteve.alchemy.AlchemyItemData;
import com.ateliersteve.alchemy.ingredient.AlchemyIngredientRegistry;
import com.ateliersteve.registry.ModBlockEntities;
import com.ateliersteve.registry.ModDataComponents;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;

import java.util.List;

public class GatheringBasketBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {
    // Configurable slot count
    public static final int SLOT_COUNT = 4000;
    // Number of columns in the grid
    private static final int COLUMNS = 9;
    // Visible rows in the scrollable area
    private static final int VISIBLE_ROWS = 6;
    // Slot size in pixels
    private static final int SLOT_SIZE = 18;
    // Scrollbar width in pixels
    private static final int SCROLLBAR_WIDTH = 10;
    private static final int SLOT_SCROLLBAR_GAP = 4;


    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public void setSize(int size) {
            // Prevent deserialization from shrinking below SLOT_COUNT
            super.setSize(Math.max(size, SLOT_COUNT));
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
            // Add alchemy data to the item when it's placed in the basket
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty() && !stack.has(ModDataComponents.ALCHEMY_DATA.get())) {
                var level = GatheringBasketBlockEntity.this.getLevel();
                var random = level != null ? level.getRandom() : net.minecraft.util.RandomSource.create();
                AlchemyItemData generated = AlchemyIngredientRegistry.generateData(stack, random);
                stack.set(ModDataComponents.ALCHEMY_DATA.get(), generated);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public GatheringBasketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GATHERING_BASKET.get(), pos, state);
    }

    @Override
    public FieldManagedStorage getSyncStorage() {
        return syncStorage;
    }

    public ModularUI createUI(Player player) {
        var root = new UIElement().layout(layout -> layout
                .setPadding(YogaEdge.ALL, 8)
                .setGap(YogaGutter.ALL, 4)
                .setAlignItems(YogaAlign.CENTER)
        ).addClass("panel_bg");

        // Title
        root.addChild(new Label()
                .setText(Component.translatable("block.atelier_steve.gathering_basket"))
                .layout(l -> l.setMargin(YogaEdge.BOTTOM, 4)));

        // ScrollerView with built-in scrollbar
        var scrollerView = new ScrollerView();
        scrollerView.layout(layout -> layout
                .setWidth(COLUMNS * SLOT_SIZE + SCROLLBAR_WIDTH + SLOT_SCROLLBAR_GAP)
                .setHeight(VISIBLE_ROWS * SLOT_SIZE)
        );

        // Create all slot rows and add to ScrollerView
        int totalRows = (int) Math.ceil((double) SLOT_COUNT / COLUMNS);
        for (int row = 0; row < totalRows; row++) {
            var rowContainer = new UIElement().layout(layout -> layout
                    .setFlexDirection(YogaFlexDirection.ROW)
                    .setGap(YogaGutter.ALL, 0)
                    .marginRight(SLOT_SCROLLBAR_GAP)
            );
            for (int col = 0; col < COLUMNS; col++) {
                int slotIndex = row * COLUMNS + col;
                if (slotIndex < SLOT_COUNT) {
                    rowContainer.addChild(new ItemSlot().bind(inventory, slotIndex));
                }
            }
            scrollerView.addScrollViewChild(rowContainer);
        }

        root.addChild(scrollerView);

        // Separator
        root.addChild(new UIElement().layout(l -> l.setHeight(8)));

        // Player inventory
        root.addChildren(new InventorySlots());

        return ModularUI.of(
                UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))),
                player
        );
    }
}
