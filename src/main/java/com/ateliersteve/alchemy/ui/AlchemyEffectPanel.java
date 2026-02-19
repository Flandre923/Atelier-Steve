package com.ateliersteve.alchemy.ui;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.element.AlchemyElement;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeDefinition;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaAlign;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class AlchemyEffectPanel {
    private static final int QUALITY_MAX = 999;
    private static final int QUALITY_SEGMENT_WIDTH = 12;
    private static final List<Integer> QUALITY_COLORS = List.of(
            0x6fa3ff,
            0xffe66d,
            0xff9f43,
            0xff6b6b,
            0xa855f7,
            0x1dd1a1,
            0x54a0ff,
            0x5f27cd,
            0x222f3e,
            0x000000
    );
    private static final Map<String, ResourceLocation> ELEMENT_ICONS = Map.of(
            "fire", AtelierSteve.id("textures/gui/elements/fire.png"),
            "water", AtelierSteve.id("textures/gui/elements/light.png"),
            "ice", AtelierSteve.id("textures/gui/elements/ice.png"),
            "wind", AtelierSteve.id("textures/gui/elements/wind.png"),
            "thunder", AtelierSteve.id("textures/gui/elements/thunder.png"),
            "light", AtelierSteve.id("textures/gui/elements/light.png")
    );

    private AlchemyEffectPanel() {
    }

    public static void buildQualityBar(UIElement bar, int quality) {
        bar.clearAllChildren();
        int clamped = Math.max(0, Math.min(quality, QUALITY_MAX));
        for (int i = 0; i < QUALITY_COLORS.size(); i++) {
            int segmentStart = i * 100;
            int segmentEnd = (i + 1) * 100;
            int fillWidth = 0;
            if (clamped >= segmentEnd) {
                fillWidth = QUALITY_SEGMENT_WIDTH;
            } else if (clamped > segmentStart) {
                float ratio = (clamped - segmentStart) / 100f;
                fillWidth = Math.max(1, Math.round(ratio * QUALITY_SEGMENT_WIDTH));
            }

            var segment = new UIElement().addClass("quality_segment");
            if (fillWidth > 0) {
                int fillWidthFinal = fillWidth;
                var fill = new UIElement()
                        .addClass("quality_segment_fill")
                        .layout(layout -> layout.width(fillWidthFinal));
                fill.lss("background", "rect(" + toHexColor(QUALITY_COLORS.get(i)) + ", 1)");
                segment.addChild(fill);
            }
            bar.addChild(segment);
        }
    }

    public static void buildEffectAttributes(
            AlchemyRecipeDefinition recipe,
            Map<String, Integer> elementValues,
            UIElement attributesScroller
    ) {
        attributesScroller.clearAllChildren();
        var attributesList = new UIElement()
                .addClass("attributes_list")
                .layout(layout -> layout.widthPercent(100));
        attributesScroller.addChild(attributesList);

        if (recipe == null || recipe.effects().isEmpty()) {
            attributesList.addChild(new Label()
                    .setText(Component.literal("\u65e0"))
                    .addClass("effects_empty"));
            return;
        }

        boolean added = false;
        for (AlchemyRecipeDefinition.EffectGroup group : recipe.effects()) {
            int max = 0;
            Set<Integer> positions = new TreeSet<>();
            for (AlchemyRecipeDefinition.EffectStep step : group.steps()) {
                int threshold = step.threshold();
                if (threshold <= 0) {
                    continue;
                }
                positions.add(threshold);
                if (threshold > max) {
                    max = threshold;
                }
            }
            if (max <= 0) {
                continue;
            }

            int value = Math.min(elementValues.getOrDefault(group.type(), 0), max);
            AlchemyElement element = AlchemyElement.fromName(group.type());
            String color = toHexColor(element.getColor());
            AlchemyRecipeDefinition.EffectStep step = group.selectStep(value);

            var row = new UIElement()
                    .addClass("attr_item")
                    .layout(layout -> layout.widthPercent(100));
            var icon = new UIElement()
                    .addClass("attr_icon");
            ResourceLocation iconTexture = resolveElementIcon(group.type());
            if (iconTexture != null) {
                icon.lss("background", "sprite(" + iconTexture + ")");
            } else {
                icon.lss("background", "rect(" + color + ", 7)");
            }
            var content = new UIElement().addClass("attr_content");
            var name = new Label()
                    .setText(step == null ? Component.literal("\u65e0") : Component.literal(step.value()))
                    .addClass("attr_name");
            var bar = new UIElement()
                    .addClass("attr_bar")
                    .layout(layout -> layout.setAlignItems(YogaAlign.FLEX_END));

            for (int n = 1; n <= max; n++) {
                var segment = new UIElement()
                        .addClass("bar_segment")
                        .layout(layout -> layout.setAlignSelf(YogaAlign.FLEX_END));
                if (positions.contains(n)) {
                    segment.addClass("bar_segment_key");
                }
                if (n <= value) {
                    segment.lss("background", "rect(" + color + ", 1)");
                }
                bar.addChild(segment);
            }

            content.addChildren(name, bar);
            row.addChildren(icon, content);
            attributesList.addChild(row);
            added = true;
        }

        if (!added) {
            attributesList.addChild(new Label()
                    .setText(Component.literal("\u65e0"))
                    .addClass("effects_empty"));
        }
    }

    public static ResourceLocation resolveElementIcon(String element) {
        if (element == null) {
            return null;
        }
        return ELEMENT_ICONS.get(element);
    }

    public static String toHexColor(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
}
