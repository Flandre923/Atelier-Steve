package com.ateliersteve.dev;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.ui.IngredientGridElement;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class DevIngredientGridTestUI {
    private DevIngredientGridTestUI() {
    }

    public static ModularUI createUI(Player player) {
        var xml = XmlUtils.loadXml(AtelierSteve.id("dev_ingredient_grid.xml"));
        if (xml == null) {
            return ModularUI.of(UI.of(new UIElement()), player);
        }

        var ui = UI.of(xml);
        var title = (Label) ui.select("#dev_title").findFirst().orElseThrow();
        var holder = ui.select("#grid_holder").findFirst().orElseThrow();

        title.setText(Component.literal("Dev UI - Ingredient Grid"));

        IngredientGridElement grid = new IngredientGridElement().setGridSize(3, 3);
        grid.setCell(1, 1, IngredientGridElement.CellType.LINK);
        grid.setCell(1, 2, IngredientGridElement.CellType.NORMAL);
        grid.setCell(1, 3, IngredientGridElement.CellType.NORMAL);
        grid.setCell(2, 3, IngredientGridElement.CellType.NORMAL);
        grid.setCell(3, 3, IngredientGridElement.CellType.NORMAL);

        holder.addChild(grid);

        return ModularUI.of(ui, player);
    }
}
