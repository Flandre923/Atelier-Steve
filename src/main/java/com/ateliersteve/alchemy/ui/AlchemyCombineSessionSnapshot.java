package com.ateliersteve.alchemy.ui;

import com.ateliersteve.alchemy.AlchemyItemData;
import com.ateliersteve.alchemy.element.AlchemyElement;
import com.ateliersteve.alchemy.element.CellType;
import com.ateliersteve.alchemy.element.ElementComponent;
import com.ateliersteve.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class AlchemyCombineSessionSnapshot {
    private final LinkedHashMap<String, MaterialEntry> materialsById;
    private final List<String> materialOrder;
    private final List<String> componentOrder;
    private final LinkedHashMap<String, String> componentToMaterialId;
    private final LinkedHashMap<String, Integer> usedComponentCounts;
    private final List<PlacedMaterial> placedMaterials;
    private final List<BoardState> boardHistory;
    private final String selectedComponentId;
    private final int selectedRotation;
    private final boolean selectedFlipped;
    private final int previewX;
    private final int previewY;

    private AlchemyCombineSessionSnapshot(
            LinkedHashMap<String, MaterialEntry> materialsById,
            List<String> materialOrder,
            List<String> componentOrder,
            LinkedHashMap<String, String> componentToMaterialId,
            LinkedHashMap<String, Integer> usedComponentCounts,
            List<PlacedMaterial> placedMaterials,
            List<BoardState> boardHistory,
            String selectedComponentId,
            int selectedRotation,
            boolean selectedFlipped,
            int previewX,
            int previewY
    ) {
        this.materialsById = materialsById;
        this.materialOrder = materialOrder;
        this.componentOrder = componentOrder;
        this.componentToMaterialId = componentToMaterialId;
        this.usedComponentCounts = usedComponentCounts;
        this.placedMaterials = placedMaterials;
        this.boardHistory = boardHistory;
        this.selectedComponentId = selectedComponentId;
        this.selectedRotation = normalizeRotation(selectedRotation);
        this.selectedFlipped = selectedFlipped;
        this.previewX = previewX;
        this.previewY = previewY;
    }

    static AlchemyCombineSessionSnapshot fromStacks(List<ItemStack> stacks) {
        LinkedHashMap<String, MaterialEntry> materials = new LinkedHashMap<>();
        List<String> materialIds = new ArrayList<>();
        List<String> componentIds = new ArrayList<>();
        LinkedHashMap<String, String> componentToMaterial = new LinkedHashMap<>();

        if (stacks != null) {
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                String materialId = String.valueOf(i + 1);
                MaterialEntry entry = MaterialEntry.of(materialId, i, stack.copy());
                if (entry.components().isEmpty()) {
                    continue;
                }
                materials.put(materialId, entry);
                materialIds.add(materialId);
                for (String componentId : entry.components().keySet()) {
                    componentIds.add(componentId);
                    componentToMaterial.put(componentId, materialId);
                }
            }
        }

        return new AlchemyCombineSessionSnapshot(
                materials,
                List.copyOf(materialIds),
                List.copyOf(componentIds),
                componentToMaterial,
                new LinkedHashMap<>(),
                List.of(),
                List.of(),
                null,
                0,
                false,
                -1,
                -1
        );
    }

    List<MaterialEntry> materials() {
        List<MaterialEntry> list = new ArrayList<>(materialOrder.size());
        for (String materialId : materialOrder) {
            MaterialEntry entry = materialsById.get(materialId);
            if (entry != null) {
                list.add(entry);
            }
        }
        return list;
    }

    MaterialEntry material(String materialId) {
        return materialsById.get(materialId);
    }

    MaterialComponentRef getIngredientComponent(String materialId, String componentId) {
        MaterialEntry material = materialsById.get(materialId);
        if (material == null) {
            return null;
        }
        MaterialComponentEntry component = material.components().get(componentId);
        if (component == null) {
            return null;
        }
        return new MaterialComponentRef(material, component);
    }

    String selectedComponentId() {
        return selectedComponentId;
    }

    String selectedMaterialId() {
        return selectedComponentId == null ? null : componentToMaterialId.get(selectedComponentId);
    }

    int selectedRotation() {
        return selectedRotation;
    }

    boolean selectedFlipped() {
        return selectedFlipped;
    }

    int previewX() {
        return previewX;
    }

    int previewY() {
        return previewY;
    }

    List<PlacedMaterial> placedMaterials() {
        return placedMaterials;
    }

    int remainingCount(String materialId) {
        MaterialEntry material = materialsById.get(materialId);
        if (material == null || material.components().isEmpty()) {
            return 0;
        }
        int remaining = 0;
        for (String componentId : material.components().keySet()) {
            remaining += remainingCountForComponent(componentId, material.stack().getCount());
        }
        return remaining;
    }

    boolean isExhausted(String materialId) {
        return remainingCount(materialId) <= 0;
    }

    boolean isComponentExhausted(String componentId) {
        String materialId = componentToMaterialId.get(componentId);
        if (materialId == null) {
            return true;
        }
        MaterialEntry material = materialsById.get(materialId);
        if (material == null || !material.components().containsKey(componentId)) {
            return true;
        }
        return remainingCountForComponent(componentId, material.stack().getCount()) <= 0;
    }

    AlchemyCombineSessionSnapshot selectComponent(String materialId, String componentId) {
        if (materialId == null || componentId == null) {
            return this;
        }
        MaterialComponentRef ref = getIngredientComponent(materialId, componentId);
        if (ref == null || ref.component().cells().isEmpty() || isComponentExhausted(componentId)) {
            return this;
        }
        if (componentId.equals(selectedComponentId) && previewX < 0 && previewY < 0) {
            return this;
        }
        return with(componentId, 0, false, -1, -1, usedComponentCounts, placedMaterials, boardHistory);
    }

    AlchemyCombineSessionSnapshot hover(int x, int y) {
        if (selectedComponentId == null) {
            return this;
        }
        if (previewX == x && previewY == y) {
            return this;
        }
        return with(selectedComponentId, selectedRotation, selectedFlipped, x, y, usedComponentCounts, placedMaterials, boardHistory);
    }

    AlchemyCombineSessionSnapshot cancelSelection() {
        if (selectedComponentId == null && previewX < 0 && previewY < 0) {
            return this;
        }
        return with(null, 0, false, -1, -1, usedComponentCounts, placedMaterials, boardHistory);
    }

    AlchemyCombineSessionSnapshot rotateSelected() {
        if (selectedComponentId == null) {
            return this;
        }
        int nextRotation = selectedFlipped
                ? normalizeRotation(selectedRotation - 1)
                : normalizeRotation(selectedRotation + 1);
        return with(selectedComponentId, nextRotation, selectedFlipped, previewX, previewY, usedComponentCounts, placedMaterials, boardHistory);
    }

    AlchemyCombineSessionSnapshot flipSelected() {
        if (selectedComponentId == null) {
            return this;
        }
        return with(selectedComponentId, selectedRotation, !selectedFlipped, previewX, previewY, usedComponentCounts, placedMaterials, boardHistory);
    }

    AlchemyCombineSessionSnapshot setSelectedTransform(int rotation, boolean flipped) {
        if (selectedComponentId == null) {
            return this;
        }
        int normalized = normalizeRotation(rotation);
        if (selectedRotation == normalized && selectedFlipped == flipped) {
            return this;
        }
        return with(selectedComponentId, normalized, flipped, previewX, previewY, usedComponentCounts, placedMaterials, boardHistory);
    }

    AlchemyCombineSessionSnapshot placeSelectedAt(int x, int y, int gridSize) {
        if (selectedComponentId == null) {
            return this;
        }
        String materialId = componentToMaterialId.get(selectedComponentId);
        if (materialId == null) {
            return this;
        }
        MaterialComponentRef ref = getIngredientComponent(materialId, selectedComponentId);
        if (ref == null || ref.component().cells().isEmpty() || isComponentExhausted(selectedComponentId)) {
            return this;
        }
        MaterialComponentEntry transformed = transformComponent(ref.component(), selectedRotation, selectedFlipped);
        List<PlacedMaterial> boardAfterSink = resolvePlacementBoard(x, y, transformed.cells(), placedMaterials, gridSize);
        if (boardAfterSink == null) {
            return this;
        }

        int nextPlacedIndex = (int) boardAfterSink.stream()
                .filter(p -> p.componentId().equals(selectedComponentId))
                .count() + 1;
        String placedId = selectedComponentId + "#" + nextPlacedIndex;

        List<PlacedMaterial> nextPlaced = new ArrayList<>(boardAfterSink);
        nextPlaced.add(new PlacedMaterial(
                placedId,
                materialId,
                selectedComponentId,
                selectedRotation,
                selectedFlipped,
                ref.material().sourceIndex(),
                ref.material().stack().copy(),
                transformed.cells(),
                x,
                y
        ));

        List<BoardState> nextHistory = new ArrayList<>(boardHistory);
        nextHistory.add(new BoardState(List.copyOf(placedMaterials), new LinkedHashMap<>(usedComponentCounts)));

        LinkedHashMap<String, Integer> nextUsed = new LinkedHashMap<>(usedComponentCounts);
        nextUsed.merge(selectedComponentId, 1, Integer::sum);
        return with(null, 0, false, -1, -1, nextUsed, List.copyOf(nextPlaced), List.copyOf(nextHistory));
    }

    AlchemyCombineSessionSnapshot removePlacedAt(int x, int y) {
        if (placedMaterials.isEmpty()) {
            return this;
        }
        PlacedMaterial target = findPlacedAt(x, y);
        if (target == null) {
            return this;
        }

        List<PlacedMaterial> nextPlaced = new ArrayList<>(placedMaterials);
        nextPlaced.remove(target);

        List<BoardState> nextHistory = new ArrayList<>(boardHistory);
        nextHistory.add(new BoardState(List.copyOf(placedMaterials), new LinkedHashMap<>(usedComponentCounts)));

        LinkedHashMap<String, Integer> nextUsed = new LinkedHashMap<>(usedComponentCounts);
        int remaining = nextUsed.getOrDefault(target.componentId(), 0) - 1;
        if (remaining <= 0) {
            nextUsed.remove(target.componentId());
        } else {
            nextUsed.put(target.componentId(), remaining);
        }

        return with(
                selectedComponentId,
                selectedRotation,
                selectedFlipped,
                previewX,
                previewY,
                nextUsed,
                List.copyOf(nextPlaced),
                List.copyOf(nextHistory)
        );
    }

    AlchemyCombineSessionSnapshot removeLastPlaced() {
        if (boardHistory.isEmpty()) {
            return this;
        }
        List<BoardState> nextHistory = new ArrayList<>(boardHistory);
        BoardState previous = nextHistory.remove(nextHistory.size() - 1);
        List<PlacedMaterial> previousBoard = previous.placedMaterials();
        LinkedHashMap<String, Integer> nextUsed = new LinkedHashMap<>(previous.usedComponentCounts());

        return with(
                selectedComponentId,
                selectedRotation,
                selectedFlipped,
                previewX,
                previewY,
                nextUsed,
                List.copyOf(previousBoard),
                List.copyOf(nextHistory)
        );
    }

    boolean isPreviewing() {
        return selectedComponentId != null;
    }

    boolean isPreviewPlacementValid(int gridSize) {
        if (selectedComponentId == null || previewX < 0 || previewY < 0) {
            return false;
        }
        String materialId = componentToMaterialId.get(selectedComponentId);
        if (materialId == null) {
            return false;
        }
        MaterialComponentRef ref = getIngredientComponent(materialId, selectedComponentId);
        if (ref == null) {
            return false;
        }
        MaterialComponentEntry transformed = transformComponent(ref.component(), selectedRotation, selectedFlipped);
        return resolvePlacementBoard(previewX, previewY, transformed.cells(), placedMaterials, gridSize) != null;
    }

    MaterialComponentEntry selectedPreviewComponent() {
        if (selectedComponentId == null) {
            return null;
        }
        String materialId = componentToMaterialId.get(selectedComponentId);
        if (materialId == null) {
            return null;
        }
        MaterialComponentRef ref = getIngredientComponent(materialId, selectedComponentId);
        if (ref == null) {
            return null;
        }
        return transformComponent(ref.component(), selectedRotation, selectedFlipped);
    }

    Integer[] toSyncPayload() {
        List<Integer> payload = new ArrayList<>();
        payload.add(3);
        payload.add(componentIndexOf(selectedComponentId));
        payload.add(previewX);
        payload.add(previewY);
        payload.add(selectedRotation);
        payload.add(selectedFlipped ? 1 : 0);
        payload.add(placedMaterials.size());
        for (PlacedMaterial placed : placedMaterials) {
            payload.add(componentIndexOf(placed.componentId()));
            payload.add(placed.originX());
            payload.add(placed.originY());
            payload.add(placed.rotation());
            payload.add(placed.flipped() ? 1 : 0);
        }
        payload.add(usedComponentCounts.size());
        for (Map.Entry<String, Integer> entry : usedComponentCounts.entrySet()) {
            payload.add(componentIndexOf(entry.getKey()));
            payload.add(entry.getValue());
        }
        return payload.toArray(Integer[]::new);
    }

    static AlchemyCombineSessionSnapshot fromSyncPayload(
            AlchemyCombineSessionSnapshot base,
            Integer[] payload,
            int gridSize
    ) {
        if (base == null || payload == null || payload.length < 5) {
            return base;
        }
        int version = payload[0] == null ? 1 : payload[0];
        if (version != 1 && version != 2 && version != 3) {
            return base;
        }

        AlchemyCombineSessionSnapshot state = base.cancelSelection();
        int selectedComponentIndex = safe(payload, 1, -1);
        int previewX = safe(payload, 2, -1);
        int previewY = safe(payload, 3, -1);
        int selectedRotation = version >= 2 ? normalizeRotation(safe(payload, 4, 0)) : 0;
        boolean selectedFlipped = version >= 2 && safe(payload, 5, 0) != 0;
        int count = Math.max(0, safe(payload, version >= 2 ? 6 : 4, 0));

        int cursor = version >= 2 ? 7 : 5;
        for (int i = 0; i < count; i++) {
            String componentId = state.componentIdAt(safe(payload, cursor++, -1));
            int originX = safe(payload, cursor++, -1);
            int originY = safe(payload, cursor++, -1);
            int rotation = version >= 2 ? normalizeRotation(safe(payload, cursor++, 0)) : 0;
            boolean flipped = version >= 2 && safe(payload, cursor++, 0) != 0;
            if (componentId == null) {
                continue;
            }
            String materialId = state.componentToMaterialId.get(componentId);
            if (materialId == null) {
                continue;
            }
            state = state.selectComponent(materialId, componentId)
                    .setSelectedTransform(rotation, flipped)
                    .placeSelectedAt(originX, originY, gridSize);
        }

        if (version >= 3) {
            int usedCount = Math.max(0, safe(payload, cursor++, 0));
            LinkedHashMap<String, Integer> used = new LinkedHashMap<>();
            for (int i = 0; i < usedCount; i++) {
                String componentId = state.componentIdAt(safe(payload, cursor++, -1));
                int value = Math.max(0, safe(payload, cursor++, 0));
                if (componentId != null && value > 0) {
                    used.put(componentId, value);
                }
            }
            state = state.with(
                    state.selectedComponentId,
                    state.selectedRotation,
                    state.selectedFlipped,
                    state.previewX,
                    state.previewY,
                    used,
                    state.placedMaterials,
                    state.boardHistory
            );
        }

        String selectedComponentId = state.componentIdAt(selectedComponentIndex);
        if (selectedComponentId != null) {
            String materialId = state.componentToMaterialId.get(selectedComponentId);
            if (materialId != null) {
                state = state.selectComponent(materialId, selectedComponentId)
                        .setSelectedTransform(selectedRotation, selectedFlipped)
                        .hover(previewX, previewY);
            }
        }
        return state;
    }

    private static int normalizeRotation(int rotation) {
        int normalized = rotation % 4;
        return normalized < 0 ? normalized + 4 : normalized;
    }

    private static MaterialComponentEntry transformComponent(MaterialComponentEntry component, int rotation, boolean flipped) {
        if (component == null) {
            return null;
        }
        int normalizedRotation = normalizeRotation(rotation);
        if (normalizedRotation == 0 && !flipped) {
            return component;
        }

        int rotatedWidth = (normalizedRotation % 2 == 0) ? component.width() : component.height();
        int rotatedHeight = (normalizedRotation % 2 == 0) ? component.height() : component.width();
        List<IngredientCell> transformedCells = new ArrayList<>(component.cells().size());
        for (IngredientCell cell : component.cells()) {
            int rx;
            int ry;
            switch (normalizedRotation) {
                case 1 -> {
                    rx = component.height() - 1 - cell.offsetY();
                    ry = cell.offsetX();
                }
                case 2 -> {
                    rx = component.width() - 1 - cell.offsetX();
                    ry = component.height() - 1 - cell.offsetY();
                }
                case 3 -> {
                    rx = cell.offsetY();
                    ry = component.width() - 1 - cell.offsetX();
                }
                default -> {
                    rx = cell.offsetX();
                    ry = cell.offsetY();
                }
            }

            int tx = flipped ? (rotatedWidth - 1 - rx) : rx;
            transformedCells.add(new IngredientCell(tx, ry, cell.element(), cell.cellType()));
        }

        if (transformedCells.isEmpty()) {
            return new MaterialComponentEntry(
                    component.componentId(),
                    component.element(),
                    rotatedWidth,
                    rotatedHeight,
                    List.of()
            );
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (IngredientCell cell : transformedCells) {
            minX = Math.min(minX, cell.offsetX());
            minY = Math.min(minY, cell.offsetY());
            maxX = Math.max(maxX, cell.offsetX());
            maxY = Math.max(maxY, cell.offsetY());
        }

        List<IngredientCell> normalizedCells = new ArrayList<>(transformedCells.size());
        for (IngredientCell cell : transformedCells) {
            normalizedCells.add(new IngredientCell(
                    cell.offsetX() - minX,
                    cell.offsetY() - minY,
                    cell.element(),
                    cell.cellType()
            ));
        }

        return new MaterialComponentEntry(
                component.componentId(),
                component.element(),
                Math.max(1, maxX - minX + 1),
                Math.max(1, maxY - minY + 1),
                List.copyOf(normalizedCells)
        );
    }

    private int remainingCountForComponent(String componentId, int materialStackCount) {
        int capacity = Math.max(1, materialStackCount);
        return Math.max(0, capacity - usedComponentCounts.getOrDefault(componentId, 0));
    }

    private static int safe(Integer[] payload, int index, int fallback) {
        if (index < 0 || index >= payload.length || payload[index] == null) {
            return fallback;
        }
        return payload[index];
    }

    private int componentIndexOf(String componentId) {
        if (componentId == null) {
            return -1;
        }
        for (int i = 0; i < componentOrder.size(); i++) {
            if (componentOrder.get(i).equals(componentId)) {
                return i;
            }
        }
        return -1;
    }

    private String componentIdAt(int index) {
        if (index < 0 || index >= componentOrder.size()) {
            return null;
        }
        return componentOrder.get(index);
    }

    private PlacedMaterial findPlacedAt(int x, int y) {
        for (int i = placedMaterials.size() - 1; i >= 0; i--) {
            PlacedMaterial placed = placedMaterials.get(i);
            for (IngredientCell cell : placed.cells()) {
                int gx = placed.originX() + cell.offsetX();
                int gy = placed.originY() + cell.offsetY();
                if (gx == x && gy == y) {
                    return placed;
                }
            }
        }
        return null;
    }

    private static List<PlacedMaterial> resolvePlacementBoard(
            int originX,
            int originY,
            List<IngredientCell> candidate,
            List<PlacedMaterial> placed,
            int gridSize
    ) {
        if (candidate == null || candidate.isEmpty()) {
            return null;
        }
        for (IngredientCell cell : candidate) {
            int x = originX + cell.offsetX();
            int y = originY + cell.offsetY();
            if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
                return null;
            }
        }

        List<PlacedMaterial> board = placed == null ? List.of() : placed;
        Map<Long, LinkedHashSet<Long>> associationByPosition = buildAssociations(board);
        long targetKey = toKey(originX, originY);
        LinkedHashSet<Long> relatedPositions = associationByPosition.get(targetKey);
        if (relatedPositions != null && !relatedPositions.isEmpty()) {
            board = removeAssociatedPlacedMaterials(board, relatedPositions);
        }

        LinkedHashSet<Long> occupied = new LinkedHashSet<>();
        for (PlacedMaterial material : board) {
            for (IngredientCell cell : material.cells()) {
                int x = material.originX() + cell.offsetX();
                int y = material.originY() + cell.offsetY();
                occupied.add(toKey(x, y));
            }
        }
        for (IngredientCell cell : candidate) {
            int x = originX + cell.offsetX();
            int y = originY + cell.offsetY();
            if (occupied.contains(toKey(x, y))) {
                return null;
            }
        }
        return board;
    }

    private static Map<Long, LinkedHashSet<Long>> buildAssociations(List<PlacedMaterial> placed) {
        LinkedHashMap<Long, LinkedHashSet<Long>> associations = new LinkedHashMap<>();
        if (placed == null || placed.isEmpty()) {
            return associations;
        }
        for (PlacedMaterial material : placed) {
            LinkedHashSet<Long> related = new LinkedHashSet<>();
            for (IngredientCell cell : material.cells()) {
                related.add(toKey(material.originX() + cell.offsetX(), material.originY() + cell.offsetY()));
            }
            for (Long key : related) {
                associations.put(key, related);
            }
        }
        return associations;
    }

    private static List<PlacedMaterial> removeAssociatedPlacedMaterials(List<PlacedMaterial> placed, LinkedHashSet<Long> relatedPositions) {
        if (placed == null || placed.isEmpty() || relatedPositions == null || relatedPositions.isEmpty()) {
            return placed;
        }
        List<PlacedMaterial> next = new ArrayList<>(placed.size());
        for (PlacedMaterial material : placed) {
            boolean intersects = false;
            for (IngredientCell cell : material.cells()) {
                long key = toKey(material.originX() + cell.offsetX(), material.originY() + cell.offsetY());
                if (relatedPositions.contains(key)) {
                    intersects = true;
                    break;
                }
            }
            if (!intersects) {
                next.add(material);
            }
        }
        return List.copyOf(next);
    }

    private static long toKey(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private AlchemyCombineSessionSnapshot with(
            String nextSelectedComponentId,
            int nextSelectedRotation,
            boolean nextSelectedFlipped,
            int nextPreviewX,
            int nextPreviewY,
            LinkedHashMap<String, Integer> nextUsedComponentCounts,
            List<PlacedMaterial> nextPlacedMaterials,
            List<BoardState> nextBoardHistory
    ) {
        return new AlchemyCombineSessionSnapshot(
                materialsById,
                materialOrder,
                componentOrder,
                componentToMaterialId,
                nextUsedComponentCounts,
                nextPlacedMaterials,
                nextBoardHistory,
                nextSelectedComponentId,
                nextSelectedRotation,
                nextSelectedFlipped,
                nextPreviewX,
                nextPreviewY
        );
    }

    record IngredientCell(int offsetX, int offsetY, AlchemyElement element, CellType cellType) {
    }

    record MaterialEntry(
            String materialId,
            int sourceIndex,
            ItemStack stack,
            LinkedHashMap<String, MaterialComponentEntry> components,
            List<IngredientCell> cells
    ) {
        static MaterialEntry of(String materialId, int sourceIndex, ItemStack stack) {
            LinkedHashMap<String, MaterialComponentEntry> components = new LinkedHashMap<>();
            List<IngredientCell> allCells = new ArrayList<>();

            AlchemyItemData data = stack.get(ModDataComponents.ALCHEMY_DATA.get());
            if (data != null && !data.elements().isEmpty()) {
                int componentIndex = 1;
                for (ElementComponent component : data.elements()) {
                    if (component == null || component.shape() == null) {
                        continue;
                    }
                    String componentId = materialId + "." + componentIndex++;
                    List<IngredientCell> componentCells = new ArrayList<>();
                    for (int y = 0; y < component.shape().getHeight(); y++) {
                        for (int x = 0; x < component.shape().getWidth(); x++) {
                            CellType cellType = component.shape().getCellAt(x, y);
                            if (cellType == CellType.EMPTY) {
                                continue;
                            }
                            IngredientCell cell = new IngredientCell(x, y, component.element(), cellType);
                            componentCells.add(cell);
                            allCells.add(cell);
                        }
                    }
                    components.put(componentId,
                            new MaterialComponentEntry(
                                    componentId,
                                    component.element(),
                                    component.shape().getWidth(),
                                    component.shape().getHeight(),
                                    List.copyOf(componentCells)
                            ));
                }
            }
            return new MaterialEntry(materialId, sourceIndex, stack, components, List.copyOf(allCells));
        }
    }

    record MaterialComponentEntry(String componentId, AlchemyElement element, int width, int height, List<IngredientCell> cells) {
    }

    record MaterialComponentRef(MaterialEntry material, MaterialComponentEntry component) {
    }

    record PlacedMaterial(
            String placedId,
            String materialId,
            String componentId,
            int rotation,
            boolean flipped,
            int sourceIndex,
            ItemStack stack,
            List<IngredientCell> cells,
            int originX,
            int originY
    ) {
    }

    record BoardState(List<PlacedMaterial> placedMaterials, LinkedHashMap<String, Integer> usedComponentCounts) {
    }

    static final class Timeline {
        private AlchemyCombineSessionSnapshot current;

        Timeline(AlchemyCombineSessionSnapshot initial) {
            this.current = initial;
        }

        AlchemyCombineSessionSnapshot current() {
            return current;
        }

        boolean apply(java.util.function.UnaryOperator<AlchemyCombineSessionSnapshot> operator) {
            if (operator == null) {
                return false;
            }
            AlchemyCombineSessionSnapshot next = operator.apply(current);
            if (next == null || next == current) {
                return false;
            }
            current = next;
            return true;
        }

        boolean sync(AlchemyCombineSessionSnapshot snapshot) {
            if (snapshot == null || snapshot == current) {
                return false;
            }
            current = snapshot;
            return true;
        }
    }
}
