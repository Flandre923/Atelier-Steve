package com.ateliersteve.alchemy.element;

import java.util.List;

import static com.ateliersteve.alchemy.element.CellType.*;

/**
 * Predefined element component templates for the alchemy system.
 * Each preset defines an element type paired with a specific shape.
 */
public class ElementShapePresets {

    // ===== Shape 1: Fire, 1x1, 1 normal =====
    public static final ElementShape FIRE_SINGLE_NORMAL = ElementShape.of(1, 1, NORMAL);

    // ===== Shape 2: Fire, 1x1, 1 link =====
    public static final ElementShape FIRE_SINGLE_LINK = ElementShape.of(1, 1, LINK);

    // ===== Shape 3: Wind, 2x2, L-shape, 1 normal + 2 link =====
    // N .
    // L L
    public static final ElementShape WIND_L_SHAPE = ElementShape.of(2, 2,
            NORMAL, EMPTY,
            LINK,   LINK
    );

    // ===== Shape 4: Wind, 2x2, square, 3 normal + 1 link (top-left) =====
    // L N
    // N N
    public static final ElementShape WIND_SQUARE = ElementShape.of(2, 2,
            LINK,   NORMAL,
            NORMAL, NORMAL
    );

    // ===== Shape 5: Ice, 3x3, specific L-shape =====
    // N . .
    // N . .
    // L N N
    public static final ElementShape ICE_L_SHAPE = ElementShape.of(3, 3,
            NORMAL, EMPTY,  EMPTY,
            NORMAL, EMPTY,  EMPTY,
            LINK,   NORMAL, NORMAL
    );

    // ===== Link item shape: 1x2, 2 link cells =====
    // L L
    public static final ElementShape DOUBLE_LINK = ElementShape.of(2, 1,
            LINK, LINK
    );

    /**
     * All predefined element component templates (element + shape pairs).
     */
    public record Preset(AlchemyElement element, ElementShape shape) {}

    public static final List<Preset> ALL_PRESETS = List.of(
            new Preset(AlchemyElement.FIRE, FIRE_SINGLE_NORMAL),
            new Preset(AlchemyElement.FIRE, FIRE_SINGLE_LINK),
            new Preset(AlchemyElement.WIND, WIND_L_SHAPE),
            new Preset(AlchemyElement.WIND, WIND_SQUARE),
            new Preset(AlchemyElement.ICE, ICE_L_SHAPE)
    );
}
