package com.ateliersteve.ui;

import com.ateliersteve.AtelierSteve;

import java.util.List;

public final class ElementCellTilePalette {
    private static final int DEFAULT_ICON_INSET = 4;

    private static final int UNFILLED_SQUARE_COLOR = 0xFF43362C;
    private static final int UNFILLED_FLAME_COLOR = 0xFF736459;

    private static final int FILLED_THUNDER_BG_COLOR = 0xFF4B3C11;
    private static final int FILLED_THUNDER_EDGE_COLOR = 0xFF9D9443;
    private static final int FILLED_THUNDER_ICON_COLOR = 0xFF998B44;

    private static final int FILLED_THUNDER_LINK_BG_COLOR = 0xFF4B3C11;
    private static final int FILLED_THUNDER_LINK_EDGE_COLOR = 0xFF9D9443;
    private static final int FILLED_THUNDER_LINK_ICON_COLOR = 0xFFFFFDBB;

    private static final int FILLED_GENERIC_BG_COLOR = 0xFF4B3C11;
    private static final int FILLED_GENERIC_LINK_EDGE_COLOR = 0xFFFFFDF6;

    public static final int FILLED_FIRE_BG_COLOR = 0xFF72270D;
    public static final int FILLED_FIRE_ICON_COLOR = 0xFFC5803D;
    public static final int FILLED_FIRE_LINK_ICON_COLOR = 0xFFFFF9CA;
    
    public static final int FILLED_ICE_BG_COLOR = 0xFF255675;
    public static final int FILLED_ICE_ICON_COLOR = 0xFF4B93A4;
    public static final int FILLED_ICE_LINK_ICON_COLOR = 0xFFE0FBFF;
    
    public static final int FILLED_LIGHT_BG_COLOR = 0xFF473B42;
    public static final int FILLED_LIGHT_ICON_COLOR = 0xFF9B8B98;
    public static final int FILLED_LIGHT_LINK_ICON_COLOR = 0xFFE8BCEB;

    public static final int FILLED_WIND_BG_COLOR = 0xFF23501D;
    public static final int FILLED_WIND_ICON_COLOR = 0xFF346628;
    public static final int FILLED_WIND_LINK_ICON_COLOR = 0xFFF5FEED;

    private static final int DISABLED_BG_COLOR = 0xFF000000;
    private static final int DISABLED_EDGE_COLOR = 0xFF843436;

    private ElementCellTilePalette() {
    }

    public static List<ElementCellTileSpec> unfilledSamples() {
        return List.of(
                new ElementCellTileSpec("empty", "Square", UNFILLED_SQUARE_COLOR, UNFILLED_SQUARE_COLOR, null, null, DEFAULT_ICON_INSET),
                new ElementCellTileSpec("flame", "Flame", UNFILLED_SQUARE_COLOR, UNFILLED_SQUARE_COLOR, AtelierSteve.id("textures/gui/dev/flame.png"), UNFILLED_FLAME_COLOR, DEFAULT_ICON_INSET),
                new ElementCellTileSpec("snow", "Snow", UNFILLED_SQUARE_COLOR, UNFILLED_SQUARE_COLOR, AtelierSteve.id("textures/gui/dev/snow.png"), UNFILLED_FLAME_COLOR, DEFAULT_ICON_INSET),
                new ElementCellTileSpec("star", "Star", UNFILLED_SQUARE_COLOR, UNFILLED_SQUARE_COLOR, AtelierSteve.id("textures/gui/dev/star.png"), UNFILLED_FLAME_COLOR, DEFAULT_ICON_INSET),
                new ElementCellTileSpec("thunder", "Thunder", UNFILLED_SQUARE_COLOR, UNFILLED_SQUARE_COLOR, AtelierSteve.id("textures/gui/dev/thunder.png"), UNFILLED_FLAME_COLOR, DEFAULT_ICON_INSET),
                new ElementCellTileSpec("wind", "Wind", UNFILLED_SQUARE_COLOR, UNFILLED_SQUARE_COLOR, AtelierSteve.id("textures/gui/dev/wind.png"), UNFILLED_FLAME_COLOR, DEFAULT_ICON_INSET)
        );
    }

    public static List<ElementCellTileSpec> thunderFilledSamples() {
        return List.of(
                new ElementCellTileSpec(
                        "filled_thunder",
                        "Thunder Filled",
                        FILLED_THUNDER_BG_COLOR,
                        FILLED_THUNDER_EDGE_COLOR,
                        AtelierSteve.id("textures/gui/dev/thunder.png"),
                        FILLED_THUNDER_ICON_COLOR,
                        DEFAULT_ICON_INSET
                ),
                new ElementCellTileSpec(
                        "filled_thunder_link",
                        "Thunder Link",
                        FILLED_THUNDER_BG_COLOR,
                        FILLED_THUNDER_EDGE_COLOR,
                        AtelierSteve.id("textures/gui/dev/thunder.png"),
                        FILLED_THUNDER_LINK_ICON_COLOR,
                        DEFAULT_ICON_INSET
                )
        );
    }

    public static List<ElementCellTileSpec> reservedFilledSamples() {
        return List.of(
                new ElementCellTileSpec(
                        "filled_fire",
                        "Fire Filled",
                        FILLED_FIRE_ICON_COLOR,
                        FILLED_FIRE_BG_COLOR,
                        AtelierSteve.id("textures/gui/dev/flame.png"),
                        FILLED_FIRE_ICON_COLOR,
                        DEFAULT_ICON_INSET
                ),
                new ElementCellTileSpec(
                        "filled_fire_link",
                        "Fire Link",
                        FILLED_FIRE_LINK_ICON_COLOR,
                        FILLED_FIRE_BG_COLOR,
                        AtelierSteve.id("textures/gui/dev/flame.png"),
                        FILLED_FIRE_LINK_ICON_COLOR,
                        DEFAULT_ICON_INSET
                ),
                new ElementCellTileSpec(
                        "filled_ice",
                        "Ice Filled",
                        FILLED_ICE_ICON_COLOR,
                        FILLED_ICE_BG_COLOR,
                        AtelierSteve.id("textures/gui/dev/snow.png"),
                        FILLED_ICE_ICON_COLOR,
                        DEFAULT_ICON_INSET
                ),
                new ElementCellTileSpec(
                        "filled_ice_link",
                        "Ice Link",
                        FILLED_ICE_LINK_ICON_COLOR,
                        FILLED_ICE_BG_COLOR,
                        AtelierSteve.id("textures/gui/dev/snow.png"),
                        FILLED_ICE_LINK_ICON_COLOR,
                        DEFAULT_ICON_INSET
                ),
                new ElementCellTileSpec(
                        "filled_light",
                        "Light Filled",
                        FILLED_LIGHT_ICON_COLOR,
                        FILLED_LIGHT_BG_COLOR,
                        AtelierSteve.id("textures/gui/dev/star.png"),
                        FILLED_LIGHT_ICON_COLOR,
                        DEFAULT_ICON_INSET
                ),
                new ElementCellTileSpec(
                        "filled_light_link",
                        "Light Link",
                        FILLED_LIGHT_LINK_ICON_COLOR,
                        FILLED_LIGHT_BG_COLOR,
                        AtelierSteve.id("textures/gui/dev/star.png"),
                        FILLED_LIGHT_LINK_ICON_COLOR,
                        DEFAULT_ICON_INSET
                ),
                new ElementCellTileSpec(
                        "filled_wind",
                        "Wind Filled",
                        FILLED_WIND_ICON_COLOR,
                        FILLED_WIND_BG_COLOR,
                        AtelierSteve.id("textures/gui/dev/wind.png"),
                        FILLED_WIND_ICON_COLOR,
                        DEFAULT_ICON_INSET
                ),
                new ElementCellTileSpec(
                        "filled_wind_link",
                        "Wind Link",
                        FILLED_WIND_LINK_ICON_COLOR,
                        FILLED_WIND_BG_COLOR,
                        AtelierSteve.id("textures/gui/dev/wind.png"),
                        FILLED_WIND_LINK_ICON_COLOR,
                        DEFAULT_ICON_INSET
                )
        );
    }

    public static List<ElementCellTileSpec> stateSamples() {
        return List.of(
                new ElementCellTileSpec(
                        "disabled",
                        "Disabled",
                        DISABLED_BG_COLOR,
                        DISABLED_EDGE_COLOR,
                        null,
                        null,
                        DEFAULT_ICON_INSET
                )
        );
    }
}
