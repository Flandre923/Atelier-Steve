package com.ateliersteve.alchemy.item;

import com.ateliersteve.alchemy.AlchemyItemData;
import com.ateliersteve.alchemy.element.AlchemyElement;
import com.ateliersteve.alchemy.element.CellType;
import com.ateliersteve.alchemy.element.ElementComponent;
import com.ateliersteve.alchemy.element.ElementShape;

import java.util.List;

public final class AlchemyItemDefinitions {
    private AlchemyItemDefinitions() {
    }

    public static final ElementComponent NEUTRALIZER_RED_FIRE_COMPONENT = new ElementComponent(
            AlchemyElement.FIRE,
            ElementShape.of(3, 3,
                    CellType.LINK, CellType.EMPTY, CellType.EMPTY,
                    CellType.NORMAL, CellType.NORMAL, CellType.EMPTY,
                    CellType.EMPTY, CellType.EMPTY, CellType.EMPTY
            )
    );

    public static final AlchemyItemData NEUTRALIZER_RED_BASE_DATA = new AlchemyItemData(
            List.of(),
            List.of(NEUTRALIZER_RED_FIRE_COMPONENT),
            3,
            0
    );

    public static final List<AlchemyItemEffect> NEUTRALIZER_RED_EFFECTS = List.of(
            AlchemyItemEffect.simple("Fire Level Up +1"),
            AlchemyItemEffect.simple("Grant Tag"),
            AlchemyItemEffect.simple("Heat Storage"),
            new AlchemyItemEffect("Neutralize Power", List.of(NEUTRALIZER_RED_FIRE_COMPONENT))
    );
}
