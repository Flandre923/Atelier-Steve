package com.ateliersteve.alchemy.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents the shape of an alchemy element on the synthesis grid.
 * Uses a variable-size grid where each cell can be EMPTY, NORMAL, or LINK.
 */
public class ElementShape {
    public static final Codec<ElementShape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("width").forGetter(ElementShape::getWidth),
            Codec.INT.fieldOf("height").forGetter(ElementShape::getHeight),
            Codec.INT.listOf().fieldOf("cells").forGetter(shape -> {
                List<Integer> list = new ArrayList<>();
                for (int y = 0; y < shape.height; y++) {
                    for (int x = 0; x < shape.width; x++) {
                        list.add(shape.cells[y][x].getId());
                    }
                }
                return list;
            })
    ).apply(instance, (width, height, cellList) -> {
        CellType[][] cells = new CellType[height][width];
        for (int i = 0; i < cellList.size(); i++) {
            cells[i / width][i % width] = CellType.fromId(cellList.get(i));
        }
        return new ElementShape(width, height, cells);
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElementShape> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ElementShape decode(RegistryFriendlyByteBuf buf) {
            int width = buf.readByte();
            int height = buf.readByte();
            CellType[][] cells = new CellType[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    cells[y][x] = CellType.fromId(buf.readByte());
                }
            }
            return new ElementShape(width, height, cells);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ElementShape shape) {
            buf.writeByte(shape.width);
            buf.writeByte(shape.height);
            for (int y = 0; y < shape.height; y++) {
                for (int x = 0; x < shape.width; x++) {
                    buf.writeByte(shape.cells[y][x].getId());
                }
            }
        }
    };

    private final int width;
    private final int height;
    private final CellType[][] cells;

    public ElementShape(int width, int height, CellType[][] cells) {
        this.width = width;
        this.height = height;
        this.cells = cells;
    }

    /**
     * Convenience constructor for creating shapes from a flat array of CellTypes.
     */
    public static ElementShape of(int width, int height, CellType... flatCells) {
        CellType[][] cells = new CellType[height][width];
        for (int i = 0; i < flatCells.length; i++) {
            cells[i / width][i % width] = flatCells[i];
        }
        return new ElementShape(width, height, cells);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public CellType getCellAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return CellType.EMPTY;
        return cells[y][x];
    }

    public boolean isFilledAt(int x, int y) {
        return getCellAt(x, y) != CellType.EMPTY;
    }

    public boolean isNormalAt(int x, int y) {
        return getCellAt(x, y) == CellType.NORMAL;
    }

    public boolean isLinkAt(int x, int y) {
        return getCellAt(x, y) == CellType.LINK;
    }

    /**
     * Count how many normal component cells are in this shape.
     */
    public int getNormalCount() {
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (cells[y][x] == CellType.NORMAL) count++;
            }
        }
        return count;
    }

    /**
     * Count how many link component cells are in this shape.
     */
    public int getLinkCount() {
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (cells[y][x] == CellType.LINK) count++;
            }
        }
        return count;
    }

    /**
     * Count how many filled (non-empty) cells are in this shape.
     */
    public int getFilledCount() {
        return getNormalCount() + getLinkCount();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElementShape that = (ElementShape) o;
        return width == that.width && height == that.height && Arrays.deepEquals(cells, that.cells);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(width, height);
        result = 31 * result + Arrays.deepHashCode(cells);
        return result;
    }
}
