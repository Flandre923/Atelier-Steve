package com.ateliersteve.ui;

import net.minecraft.resources.ResourceLocation;

public record ElementCellTileSpec(
        String id,
        String label,
        Integer fillColor,
        int squareColor,
        ResourceLocation iconTexture,
        Integer iconColor,
        int iconInset
) {
}
