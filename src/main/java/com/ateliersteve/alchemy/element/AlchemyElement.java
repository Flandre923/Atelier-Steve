package com.ateliersteve.alchemy.element;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum AlchemyElement implements StringRepresentable {
    FIRE("fire", 0xFF5500),
    LIGHT("light", 0xC9B3FF),
    ICE("ice", 0x00FFFF),
    THUNDER("thunder", 0xFFFF00),
    WIND("wind", 0x00FF00);

    private final String name;
    private final int color;

    AlchemyElement(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public Component getDisplayName() {
        return Component.translatable("element.atelier_steve." + name);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static AlchemyElement fromName(String name) {
        if ("water".equals(name)) {
            return LIGHT;
        }
        for (AlchemyElement element : values()) {
            if (element.name.equals(name)) {
                return element;
            }
        }
        return FIRE;
    }
}
