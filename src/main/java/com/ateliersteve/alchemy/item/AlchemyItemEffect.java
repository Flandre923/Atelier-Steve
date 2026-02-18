package com.ateliersteve.alchemy.item;

import com.ateliersteve.alchemy.element.ElementComponent;

import java.util.List;

public record AlchemyItemEffect(String name, List<ElementComponent> bonusElements) {
    public static AlchemyItemEffect simple(String name) {
        return new AlchemyItemEffect(name, List.of());
    }
}
