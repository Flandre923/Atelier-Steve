package com.ateliersteve.alchemy.ui;

import com.ateliersteve.alchemy.AlchemyItemData;
import com.ateliersteve.alchemy.element.AlchemyElement;
import com.ateliersteve.alchemy.element.CellType;
import com.ateliersteve.alchemy.element.ElementComponent;
import com.ateliersteve.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

final class AlchemyCombineSessionSnapshot {
    private final LinkedHashMap<String, MaterialEntry> materialsById;
    private final List<String> materialOrder;
    private final List<String> componentOrder;
    private final LinkedHashMap<String, String> componentToMaterialId;
    private final LinkedHashMap<String, Integer> usedComponentCounts;
    private final List<PlacedMaterial> placedMaterials;
    private final String selectedComponentId;
    private final int previewX;
    private final int previewY;

    private AlchemyCombineSessionSnapshot(
            LinkedHashMap<String, MaterialEntry> materialsById,
            List<String> materialOrder,
            List<String> componentOrder,
            LinkedHashMap<String, String> componentToMaterialId,
            LinkedHashMap<String, Integer> usedComponentCounts,
            List<PlacedMaterial> placedMaterials,
            String selectedComponentId,
            int previewX,
            int previewY
    ) {
        this.materialsById = materialsById;
        this.materialOrder = materialOrder;
        this.componentOrder = componentOrder;
        this.componentToMaterialId = componentToMaterialId;
        this.usedComponentCounts = usedComponentCounts;
        this.placedMaterials = placedMaterials;
        this.selectedComponentId = selectedComponentId;
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
                null,
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
        return with(componentId, -1, -1, usedComponentCounts, placedMaterials);
    }

    AlchemyCombineSessionSnapshot hover(int x, int y) {
        if (selectedComponentId == null) {
            return this;
        }
        if (previewX == x && previewY == y) {
            return this;
        }
        return with(selectedComponentId, x, y, usedComponentCounts, placedMaterials);
    }

    AlchemyCombineSessionSnapshot cancelSelection() {
        if (selectedComponentId == null && previewX < 0 && previewY < 0) {
            return this;
        }
        return with(null, -1, -1, usedComponentCounts, placedMaterials);
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
        if (!isPlacementValid(x, y, ref.component().cells(), placedMaterials, gridSize)) {
            return this;
        }

        int nextPlacedIndex = (int) placedMaterials.stream()
                .filter(p -> p.componentId().equals(selectedComponentId))
                .count() + 1;
        String placedId = selectedComponentId + "#" + nextPlacedIndex;

        List<PlacedMaterial> nextPlaced = new ArrayList<>(placedMaterials);
        nextPlaced.add(new PlacedMaterial(
                placedId,
                materialId,
                selectedComponentId,
                ref.material().sourceIndex(),
                ref.material().stack().copy(),
                ref.component().cells(),
                x,
                y
        ));

        LinkedHashMap<String, Integer> nextUsed = new LinkedHashMap<>(usedComponentCounts);
        nextUsed.merge(selectedComponentId, 1, Integer::sum);
        return with(null, -1, -1, nextUsed, List.copyOf(nextPlaced));
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

        LinkedHashMap<String, Integer> nextUsed = new LinkedHashMap<>(usedComponentCounts);
        int remaining = nextUsed.getOrDefault(target.componentId(), 0) - 1;
        if (remaining <= 0) {
            nextUsed.remove(target.componentId());
        } else {
            nextUsed.put(target.componentId(), remaining);
        }

        return with(selectedComponentId, previewX, previewY, nextUsed, List.copyOf(nextPlaced));
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
        return isPlacementValid(previewX, previewY, ref.component().cells(), placedMaterials, gridSize);
    }

    Integer[] toSyncPayload() {
        List<Integer> payload = new ArrayList<>();
        payload.add(1);
        payload.add(componentIndexOf(selectedComponentId));
        payload.add(previewX);
        payload.add(previewY);
        payload.add(placedMaterials.size());
        for (PlacedMaterial placed : placedMaterials) {
            payload.add(componentIndexOf(placed.componentId()));
            payload.add(placed.originX());
            payload.add(placed.originY());
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
        if (payload[0] == null || payload[0] != 1) {
            return base;
        }

        AlchemyCombineSessionSnapshot state = base.cancelSelection();
        int selectedComponentIndex = safe(payload, 1, -1);
        int previewX = safe(payload, 2, -1);
        int previewY = safe(payload, 3, -1);
        int count = Math.max(0, safe(payload, 4, 0));

        int cursor = 5;
        for (int i = 0; i < count; i++) {
            String componentId = state.componentIdAt(safe(payload, cursor++, -1));
            int originX = safe(payload, cursor++, -1);
            int originY = safe(payload, cursor++, -1);
            if (componentId == null) {
                continue;
            }
            String materialId = state.componentToMaterialId.get(componentId);
            if (materialId == null) {
                continue;
            }
            state = state.selectComponent(materialId, componentId).placeSelectedAt(originX, originY, gridSize);
        }

        String selectedComponentId = state.componentIdAt(selectedComponentIndex);
        if (selectedComponentId != null) {
            String materialId = state.componentToMaterialId.get(selectedComponentId);
            if (materialId != null) {
                state = state.selectComponent(materialId, selectedComponentId).hover(previewX, previewY);
            }
        }
        return state;
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

    private static boolean isPlacementValid(
            int originX,
            int originY,
            List<IngredientCell> candidate,
            List<PlacedMaterial> placed,
            int gridSize
    ) {
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }
        LinkedHashSet<Long> occupied = new LinkedHashSet<>();
        for (PlacedMaterial material : placed) {
            for (IngredientCell cell : material.cells()) {
                int x = material.originX() + cell.offsetX();
                int y = material.originY() + cell.offsetY();
                occupied.add(toKey(x, y));
            }
        }
        for (IngredientCell cell : candidate) {
            int x = originX + cell.offsetX();
            int y = originY + cell.offsetY();
            if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
                return false;
            }
            if (occupied.contains(toKey(x, y))) {
                return false;
            }
        }
        return true;
    }

    private static long toKey(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private AlchemyCombineSessionSnapshot with(
            String nextSelectedComponentId,
            int nextPreviewX,
            int nextPreviewY,
            LinkedHashMap<String, Integer> nextUsedComponentCounts,
            List<PlacedMaterial> nextPlacedMaterials
    ) {
        return new AlchemyCombineSessionSnapshot(
                materialsById,
                materialOrder,
                componentOrder,
                componentToMaterialId,
                nextUsedComponentCounts,
                nextPlacedMaterials,
                nextSelectedComponentId,
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
                            new MaterialComponentEntry(componentId, component.element(), List.copyOf(componentCells)));
                }
            }
            return new MaterialEntry(materialId, sourceIndex, stack, components, List.copyOf(allCells));
        }
    }

    record MaterialComponentEntry(String componentId, AlchemyElement element, List<IngredientCell> cells) {
    }

    record MaterialComponentRef(MaterialEntry material, MaterialComponentEntry component) {
    }

    record PlacedMaterial(
            String placedId,
            String materialId,
            String componentId,
            int sourceIndex,
            ItemStack stack,
            List<IngredientCell> cells,
            int originX,
            int originY
    ) {
    }

    static final class Timeline {
        private final Deque<AlchemyCombineSessionSnapshot> history = new ArrayDeque<>();
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
            history.push(current);
            current = next;
            return true;
        }

        boolean undo() {
            if (history.isEmpty()) {
                return false;
            }
            current = history.pop();
            return true;
        }
    }
}
