package com.ateliersteve.alchemy.element;

import java.util.List;

import static com.ateliersteve.alchemy.element.CellType.*;

/**
 * Predefined element component templates for the alchemy system.
 * Each preset defines an element type paired with a specific shape.
 */
public class ElementShapePresets {

    // ===== Shape 1: Fire, 3x3, 1 normal (top-left) =====
    public static final ElementShape FIRE_SINGLE_NORMAL = ElementShape.of(3, 3,
            NORMAL, EMPTY, EMPTY,
            EMPTY,  EMPTY, EMPTY,
            EMPTY,  EMPTY, EMPTY
    );

    // ===== Shape 2: Fire, 3x3, 1 link (top-left) =====
    public static final ElementShape FIRE_SINGLE_LINK = ElementShape.of(3, 3,
            LINK,  EMPTY, EMPTY,
            EMPTY, EMPTY, EMPTY,
            EMPTY, EMPTY, EMPTY
    );

    // ===== Shape 3: Wind, 3x3, L-shape (top-left), 1 normal + 2 link =====
    // N .
    // L L
    public static final ElementShape WIND_L_SHAPE = ElementShape.of(3, 3,
            NORMAL, EMPTY, EMPTY,
            LINK,   LINK,  EMPTY,
            EMPTY,  EMPTY, EMPTY
    );

    // ===== Shape 4: Wind, 3x3, square (top-left), 3 normal + 1 link =====
    // L N
    // N N
    public static final ElementShape WIND_SQUARE = ElementShape.of(3, 3,
            LINK,   NORMAL, EMPTY,
            NORMAL, NORMAL, EMPTY,
            EMPTY,  EMPTY,  EMPTY
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

    // ===== Link item shape: 3x3, 2 link cells (top row) =====
    // L L
    public static final ElementShape DOUBLE_LINK = ElementShape.of(3, 3,
            LINK, LINK,  EMPTY,
            EMPTY, EMPTY, EMPTY,
            EMPTY, EMPTY, EMPTY
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

    public static final List<ElementComponent> ALL_COMPONENTS = ALL_PRESETS.stream()
            .map(ElementComponent::fromPreset)
            .toList();
}
