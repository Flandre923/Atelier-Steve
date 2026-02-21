package com.ateliersteve.alchemy.ui;

import com.ateliersteve.alchemy.element.AlchemyElement;
import com.ateliersteve.alchemy.element.CellType;
import com.ateliersteve.ui.ElementCellTileElement;
import com.ateliersteve.ui.ElementCellTilePalette;
import com.ateliersteve.ui.ElementCellTileSpec;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import java.util.List;

final class AlchemyCombineBoard {
    private static final float PREVIEW_ALPHA = 0.55f;
    private static final float INVALID_PREVIEW_ALPHA = 0.6f;

    private AlchemyCombineBoard() {
    }

    static View buildGrid(UIElement grid, CellHandler handler, int gridSize) {
        grid.clearAllChildren();
        var content = new UIElement().addClass("combine_grid_content");
        ElementCellTileElement[][] cells = new ElementCellTileElement[gridSize][gridSize];
        for (int row = 0; row < gridSize; row++) {
            var rowElement = new UIElement().addClass("combine_grid_row");
            for (int col = 0; col < gridSize; col++) {
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
        return new View(cells);
    }

    static void render(View gridView, State boardState) {
        for (int y = 0; y < boardState.height(); y++) {
            for (int x = 0; x < boardState.width(); x++) {
                ElementCellTileElement cell = gridView.cellAt(x, y);
                CellState state = boardState.cellAt(x, y);
                boolean connectLeft = isConnected(boardState, x, y, x - 1, y);
                boolean connectRight = isConnected(boardState, x, y, x + 1, y);
                boolean connectUp = isConnected(boardState, x, y, x, y - 1);
                boolean connectDown = isConnected(boardState, x, y, x, y + 1);
                boolean connectUpLeft = isConnected(boardState, x, y, x - 1, y - 1);
                boolean connectUpRight = isConnected(boardState, x, y, x + 1, y - 1);
                boolean connectDownLeft = isConnected(boardState, x, y, x - 1, y + 1);
                boolean connectDownRight = isConnected(boardState, x, y, x + 1, y + 1);
                Integer connectorColor = resolveConnectorColor(state);
                cell.applySpec(
                        state.spec(),
                        connectorColor,
                        connectLeft,
                        connectRight,
                        connectUp,
                        connectDown,
                        connectUpLeft,
                        connectUpRight,
                        connectDownLeft,
                        connectDownRight
                );
                cell.lss("opacity", String.valueOf(state.opacity()));
            }
        }
    }

    private static boolean isConnected(State boardState, int x, int y, int otherX, int otherY) {
        if (boardState == null || otherX < 0 || otherX >= boardState.width() || otherY < 0 || otherY >= boardState.height()) {
            return false;
        }
        CellState current = boardState.cellAt(x, y);
        CellState other = boardState.cellAt(otherX, otherY);
        if (current == null || other == null || current.groupId() == null) {
            return false;
        }
        return current.groupId().equals(other.groupId());
    }

    private static Integer resolveConnectorColor(CellState state) {
        if (state == null || state.spec() == null || state.groupId() == null) {
            return null;
        }
        if (state.element() != null) {
            ElementCellTileSpec baseSpec = ElementCellTilePalette.filled(state.element(), false);
            if (baseSpec != null && baseSpec.iconColor() != null) {
                return baseSpec.iconColor();
            }
            return 0xFF000000 | (state.element().getColor() & 0xFFFFFF);
        }
        return state.spec().squareColor();
    }

    interface CellHandler {
        void onHover(int x, int y);

        void onClick(int x, int y, int button);
    }

    static final class View {
        private final ElementCellTileElement[][] cells;

        private View(ElementCellTileElement[][] cells) {
            this.cells = cells;
        }

        private ElementCellTileElement cellAt(int x, int y) {
            return cells[y][x];
        }
    }

    static final class State {
        private final int width;
        private final int height;
        private final CellState[][] cells;

        State(int width, int height) {
            this.width = width;
            this.height = height;
            this.cells = new CellState[height][width];
            reset();
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }

        void rebuild(AlchemyCombineSessionSnapshot snapshot, int gridSize) {
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

        CellState cellAt(int x, int y) {
            return cells[y][x];
        }

        Integer[] writePayload() {
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
                    cells[y][x] = CellState.empty();
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
                    cells[gridY][gridX] = CellState.placed(ingredient.placedId(), cell.element(), cell.cellType() == CellType.LINK);
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
                        ? CellState.preview(cell.element(), cell.cellType() == CellType.LINK)
                        : CellState.invalidPreview();
            }
        }

        private boolean isInsideGrid(int x, int y, int gridSize) {
            return x >= 0 && x < gridSize && y >= 0 && y < gridSize;
        }
    }

    private record CellState(
            ElementCellTileSpec spec,
            float opacity,
            AlchemyElement element,
            boolean link,
            boolean preview,
            boolean invalid,
            String groupId
    ) {
        private static CellState empty() {
            return new CellState(ElementCellTilePalette.empty(), 1.0f, null, false, false, false, null);
        }

        private static CellState placed(String groupId, AlchemyElement element, boolean link) {
            return new CellState(ElementCellTilePalette.filled(element, link), 1.0f, element, link, false, false, groupId);
        }

        private static CellState preview(AlchemyElement element, boolean link) {
            float alpha = link ? 0.8f : PREVIEW_ALPHA;
            return new CellState(ElementCellTilePalette.preview(element, link), alpha, element, link, true, false, "__preview__");
        }

        private static CellState invalidPreview() {
            return new CellState(ElementCellTilePalette.disabled(), INVALID_PREVIEW_ALPHA, null, false, true, true, null);
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
}
