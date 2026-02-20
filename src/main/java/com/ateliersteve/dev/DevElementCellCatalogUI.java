package com.ateliersteve.dev;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.ui.ElementCellTileElement;
import com.ateliersteve.ui.ElementCellTilePalette;
import com.ateliersteve.ui.ElementCellTileSpec;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class DevElementCellCatalogUI {
    private static final int COLUMNS_PER_ROW = 4;

    private DevElementCellCatalogUI() {
    }

    public static ModularUI createUI(Player player) {
        var xml = XmlUtils.loadXml(AtelierSteve.id("dev_element_cells.xml"));
        if (xml == null) {
            return ModularUI.of(UI.of(new UIElement()), player);
        }

        var ui = UI.of(xml);
        var title = (Label) ui.select("#dev_title").findFirst().orElseThrow();
        var subtitle = (Label) ui.select("#dev_subtitle").findFirst().orElseThrow();
        var sections = ui.select("#sections").findFirst().orElseThrow();

        title.setText(Component.literal("Dev UI - Element Cell Catalog"));
        subtitle.setText(Component.literal("/atelierdev cells"));

        addSection(sections, "Unfilled Templates", ElementCellTilePalette.unfilledSamples());
        addSection(sections, "Thunder Filled", ElementCellTilePalette.thunderFilledSamples());
        addSection(sections, "Reserved Filled + Link (edit colors in ElementCellTilePalette)", ElementCellTilePalette.reservedFilledSamples());
        addSection(sections, "State", ElementCellTilePalette.stateSamples());

        return ModularUI.of(ui, player);
    }

    private static void addSection(UIElement parent, String sectionTitle, List<ElementCellTileSpec> specs) {
        UIElement section = new UIElement().addClass("cell_section");
        section.addChild(new Label().setText(Component.literal(sectionTitle)).addClass("section_title"));

        UIElement rows = new UIElement().addClass("section_rows");
        for (int i = 0; i < specs.size(); i += COLUMNS_PER_ROW) {
            UIElement row = new UIElement().addClass("cell_row");
            for (int j = i; j < Math.min(i + COLUMNS_PER_ROW, specs.size()); j++) {
                row.addChild(buildCard(specs.get(j)));
            }
            rows.addChild(row);
        }
        section.addChild(rows);
        parent.addChild(section);
    }

    private static UIElement buildCard(ElementCellTileSpec spec) {
        UIElement card = new UIElement().addClass("cell_card");
        card.addChild(new ElementCellTileElement().applySpec(spec));
        card.addChild(new Label().setText(Component.literal(spec.label())).addClass("cell_name"));
        return card;
    }
}
