package com.ateliersteve.alchemy.element;

/**
 * Represents the type of a cell in an element shape grid.
 * EMPTY - no component at this position
 * NORMAL - a normal component (普通成分) that provides element value
 * LINK - a link component (连结成分) that connects to adjacent elements
 */
public enum CellType {
    EMPTY(0),
    NORMAL(1),
    LINK(2);

    private final int id;

    CellType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static CellType fromId(int id) {
        return switch (id) {
            case 1 -> NORMAL;
            case 2 -> LINK;
            default -> EMPTY;
        };
    }
}
