package com.ateliersteve.ui;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.ui.AlchemyEffectPanel;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class IngredientGridElement extends UIElement {
    private static final ResourceLocation STAR_TEXTURE = AtelierSteve.id("textures/gui/dev/star.png");

    public enum CellType {
        EMPTY,
        NORMAL,
        LINK
    }

    private int columns = 0;
    private int rows = 0;
    private int elementColor = 0xFFD44B4B;
    private final Map<CellPos, CellType> cells = new HashMap<>();

    public IngredientGridElement() {
        addClass("ingredient_grid");
    }

    public IngredientGridElement setGridSize(int columns, int rows) {
        this.columns = Math.max(columns, 0);
        this.rows = Math.max(rows, 0);
        rebuild();
        return this;
    }

    public IngredientGridElement setCell(int x, int y, CellType type) {
        if (x <= 0 || y <= 0) {
            return this;
        }
        CellPos key = new CellPos(x, y);
        if (type == null || type == CellType.EMPTY) {
            cells.remove(key);
        } else {
            cells.put(key, type);
        }
        rebuild();
        return this;
    }

    public IngredientGridElement clearCells() {
        cells.clear();
        rebuild();
        return this;
    }

    public IngredientGridElement setElementColor(int argb) {
        this.elementColor = argb;
        rebuild();
        return this;
    }

    private void rebuild() {
        clearAllChildren();
        if (columns <= 0 || rows <= 0) {
            return;
        }

        for (int row = 1; row <= rows; row++) {
            UIElement rowElement = new UIElement().addClass("ingredient_row");
            for (int col = 1; col <= columns; col++) {
                CellType type = cells.getOrDefault(new CellPos(col, row), CellType.EMPTY);
                UIElement cell = new UIElement().addClass("ingredient_cell");
                switch (type) {
                    case LINK -> {
                        cell.addClass("ingredient_cell_link");
                        UIElement star = new UIElement().addClass("ingredient_star");
                        star.style(style -> style.backgroundTexture(SpriteTexture.of(STAR_TEXTURE).setColor(elementColor)));
                        cell.addChild(star);
                    }
                    case NORMAL -> {
                        cell.addClass("ingredient_cell_normal");
                        cell.lss("background", "rect(" + AlchemyEffectPanel.toHexColor(elementColor) + ", 1)");
                    }
                    case EMPTY -> cell.addClass("ingredient_cell_empty");
                }
                rowElement.addChild(cell);
            }
            addChild(rowElement);
        }
    }

    private record CellPos(int x, int y) {
    }
}
