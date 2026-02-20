package com.ateliersteve.ui;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.ui.AlchemyEffectPanel;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.resources.ResourceLocation;

public final class ElementCellTileElement extends UIElement {
    private static final ResourceLocation SQUARE_TEXTURE = AtelierSteve.id("textures/gui/dev/square.png");
    private static final int TILE_SIZE = 24;
    private static final int MIN_ICON_SIZE = 10;

    public ElementCellTileElement() {
        addClass("element_cell_tile");
        lss("width", String.valueOf(TILE_SIZE));
        lss("height", String.valueOf(TILE_SIZE));
        lss("align-items", "center");
        lss("justify-content", "center");
    }

    public ElementCellTileElement applySpec(ElementCellTileSpec spec) {
        clearAllChildren();

        if (spec == null) {
            lss("background", "rect(rgba(0, 0, 0, 0), 0)");
            return this;
        }

        if (spec.fillColor() == null) {
            lss("background", "rect(rgba(0, 0, 0, 0), 0)");
        } else {
            lss("background", "rect(" + AlchemyEffectPanel.toHexColor(spec.fillColor()) + ", 0)");
        }

        UIElement square = new UIElement().addClass("element_cell_square");
        square.lss("width", String.valueOf(TILE_SIZE));
        square.lss("height", String.valueOf(TILE_SIZE));
        square.lss("align-items", "center");
        square.lss("justify-content", "center");
        square.style(style -> style.backgroundTexture(SpriteTexture.of(SQUARE_TEXTURE).setColor(spec.squareColor())));

        if (spec.iconTexture() != null) {
            int iconInset = Math.max(0, spec.iconInset());
            int iconSize = Math.max(MIN_ICON_SIZE, TILE_SIZE - iconInset * 2);
            UIElement icon = new UIElement().addClass("element_cell_icon");
            icon.lss("width", String.valueOf(iconSize));
            icon.lss("height", String.valueOf(iconSize));
            if (spec.iconColor() == null) {
                icon.style(style -> style.backgroundTexture(SpriteTexture.of(spec.iconTexture())));
            } else {
                icon.style(style -> style.backgroundTexture(SpriteTexture.of(spec.iconTexture()).setColor(spec.iconColor())));
            }
            square.addChild(icon);
        }

        addChild(square);
        return this;
    }
}
