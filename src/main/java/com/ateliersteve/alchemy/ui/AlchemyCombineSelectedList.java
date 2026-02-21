package com.ateliersteve.alchemy.ui;

import com.ateliersteve.alchemy.element.CellType;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.ateliersteve.ui.IngredientGridElement;
import com.ateliersteve.ui.StaticItemElement;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class AlchemyCombineSelectedList {
    private AlchemyCombineSelectedList() {
    }

    static void populate(
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

    interface ItemSelectHandler {
        void handle(String materialId, String componentId);
    }
}
