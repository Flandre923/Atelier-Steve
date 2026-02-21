package com.ateliersteve.dev;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.ui.AlchemyCombineWriteTests;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import net.neoforged.neoforge.common.NeoForge;

public final class DevUiBootstrap {
    private DevUiBootstrap() {
    }

    public static void register() {
        AlchemyCombineWriteTests.run();
        PlayerUIMenuType.register(AtelierSteve.id("dev_showcase"), player -> DevShowcaseUI::createUI);
        PlayerUIMenuType.register(AtelierSteve.id("dev_grid_test"), player -> DevIngredientGridTestUI::createUI);
        PlayerUIMenuType.register(AtelierSteve.id("dev_element_cells"), player -> DevElementCellCatalogUI::createUI);
        NeoForge.EVENT_BUS.addListener(DevUiCommand::register);
    }
}
