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
    private static final int CONNECTOR_THICKNESS = 4;
    private static final int CONNECTOR_GAP_REACH = 1;
    private static final int DIAGONAL_STEP = 4;
    private static final int DIAGONAL_SIZE = 3;

    public ElementCellTileElement() {
        addClass("element_cell_tile");
        lss("position", "relative");
        lss("width", String.valueOf(TILE_SIZE));
        lss("height", String.valueOf(TILE_SIZE));
        lss("align-items", "center");
        lss("justify-content", "center");
    }

    public ElementCellTileElement applySpec(ElementCellTileSpec spec) {
        return applySpec(spec,
                null,
                null,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
    }

    public ElementCellTileElement applySpec(
            ElementCellTileSpec spec,
            Integer componentConnectorColor,
            Integer chainConnectorColor,
            boolean componentConnectLeft,
            boolean componentConnectRight,
            boolean componentConnectUp,
            boolean componentConnectDown,
            boolean componentConnectUpLeft,
            boolean componentConnectUpRight,
            boolean componentConnectDownLeft,
            boolean componentConnectDownRight,
            boolean chainConnectLeft,
            boolean chainConnectRight,
            boolean chainConnectUp,
            boolean chainConnectDown
    ) {
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

        if (componentConnectorColor != null) {
            String colorHex = AlchemyEffectPanel.toHexColor(componentConnectorColor);
            if (componentConnectLeft) {
                addChild(createConnector(
                        -CONNECTOR_GAP_REACH,
                        (TILE_SIZE - CONNECTOR_THICKNESS) / 2,
                        TILE_SIZE / 2 + CONNECTOR_GAP_REACH,
                        CONNECTOR_THICKNESS,
                        colorHex
                ));
            }
            if (componentConnectRight) {
                addChild(createConnector(
                        TILE_SIZE / 2,
                        (TILE_SIZE - CONNECTOR_THICKNESS) / 2,
                        TILE_SIZE / 2 + CONNECTOR_GAP_REACH,
                        CONNECTOR_THICKNESS,
                        colorHex
                ));
            }
            if (componentConnectUp) {
                addChild(createConnector(
                        (TILE_SIZE - CONNECTOR_THICKNESS) / 2,
                        -CONNECTOR_GAP_REACH,
                        CONNECTOR_THICKNESS,
                        TILE_SIZE / 2 + CONNECTOR_GAP_REACH,
                        colorHex
                ));
            }
            if (componentConnectDown) {
                addChild(createConnector(
                        (TILE_SIZE - CONNECTOR_THICKNESS) / 2,
                        TILE_SIZE / 2,
                        CONNECTOR_THICKNESS,
                        TILE_SIZE / 2 + CONNECTOR_GAP_REACH,
                        colorHex
                ));
            }
            if (componentConnectUpLeft) {
                addDiagonalConnector(-1, -1, colorHex);
            }
            if (componentConnectUpRight) {
                addDiagonalConnector(1, -1, colorHex);
            }
            if (componentConnectDownLeft) {
                addDiagonalConnector(-1, 1, colorHex);
            }
            if (componentConnectDownRight) {
                addDiagonalConnector(1, 1, colorHex);
            }
        }

        if (chainConnectorColor != null) {
            String colorHex = AlchemyEffectPanel.toHexColor(chainConnectorColor);
            if (chainConnectLeft) {
                addChild(createConnector(
                        -CONNECTOR_GAP_REACH,
                        (TILE_SIZE - CONNECTOR_THICKNESS) / 2,
                        TILE_SIZE / 2 + CONNECTOR_GAP_REACH,
                        CONNECTOR_THICKNESS,
                        colorHex
                ));
            }
            if (chainConnectRight) {
                addChild(createConnector(
                        TILE_SIZE / 2,
                        (TILE_SIZE - CONNECTOR_THICKNESS) / 2,
                        TILE_SIZE / 2 + CONNECTOR_GAP_REACH,
                        CONNECTOR_THICKNESS,
                        colorHex
                ));
            }
            if (chainConnectUp) {
                addChild(createConnector(
                        (TILE_SIZE - CONNECTOR_THICKNESS) / 2,
                        -CONNECTOR_GAP_REACH,
                        CONNECTOR_THICKNESS,
                        TILE_SIZE / 2 + CONNECTOR_GAP_REACH,
                        colorHex
                ));
            }
            if (chainConnectDown) {
                addChild(createConnector(
                        (TILE_SIZE - CONNECTOR_THICKNESS) / 2,
                        TILE_SIZE / 2,
                        CONNECTOR_THICKNESS,
                        TILE_SIZE / 2 + CONNECTOR_GAP_REACH,
                        colorHex
                ));
            }
        }

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

    private UIElement createConnector(int left, int top, int width, int height, String colorHex) {
        UIElement connector = new UIElement();
        connector.lss("position", "absolute");
        connector.lss("left", String.valueOf(left));
        connector.lss("top", String.valueOf(top));
        connector.lss("width", String.valueOf(width));
        connector.lss("height", String.valueOf(height));
        connector.lss("background", "rect(" + colorHex + ", 1)");
        return connector;
    }

    private void addDiagonalConnector(int directionX, int directionY, String colorHex) {
        int base = (TILE_SIZE - DIAGONAL_SIZE) / 2;
        for (int step = 0; step < 3; step++) {
            int left = base + directionX * step * DIAGONAL_STEP;
            int top = base + directionY * step * DIAGONAL_STEP;
            addChild(createConnector(left, top, DIAGONAL_SIZE, DIAGONAL_SIZE, colorHex));
        }

        int edgeLeft = directionX < 0
                ? -CONNECTOR_GAP_REACH
                : TILE_SIZE - DIAGONAL_SIZE + CONNECTOR_GAP_REACH;
        int edgeTop = directionY < 0
                ? -CONNECTOR_GAP_REACH
                : TILE_SIZE - DIAGONAL_SIZE + CONNECTOR_GAP_REACH;
        addChild(createConnector(edgeLeft, edgeTop, DIAGONAL_SIZE, DIAGONAL_SIZE, colorHex));
    }

}
