package com.ateliersteve.dev;

import com.ateliersteve.AtelierSteve;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class DevShowcaseUI {
    private DevShowcaseUI() {
    }

    public static ModularUI createUI(Player player) {
        var xml = XmlUtils.loadXml(AtelierSteve.id("dev_showcase.xml"));
        if (xml == null) {
            return ModularUI.of(UI.of(new UIElement()), player);
        }

        var ui = UI.of(xml);
        var title = (Label) ui.select("#dev_title").findFirst().orElseThrow();
        var star = ui.select("#star_icon").findFirst().orElseThrow();
        var colorLabel = (Label) ui.select("#star_color_label").findFirst().orElseThrow();
        var colorInput = (TextField) ui.select("#star_color_input").findFirst().orElseThrow();
        var note = (Label) ui.select("#star_note").findFirst().orElseThrow();

        title.setText(Component.literal("Dev UI - Icon Showcase"));
        colorLabel.setText(Component.literal("Color"));
        colorInput.setText("#FFD43B", false);
        colorInput.setTextResponder(text -> {
            Integer color = parseColor(text);
            if (color != null) {
                applyStarColor(star, color);
            }
        });
        applyStarColor(star, 0xFFFFD43B);
        note.setText(Component.literal("Use #RRGGBB or AARRGGBB"));

        return ModularUI.of(ui, player);
    }

    private static void applyStarColor(UIElement star, int argb) {
        var texture = SpriteTexture.of(AtelierSteve.id("textures/gui/dev/star.png"))
                .setColor(argb);
        star.style(style -> style.backgroundTexture(texture));
    }

    private static Integer parseColor(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.startsWith("0x") || value.startsWith("0X")) {
            value = value.substring(2);
        }
        if (value.length() != 6 && value.length() != 8) {
            return null;
        }
        try {
            int color = (int) Long.parseLong(value, 16);
            if (value.length() == 6) {
                color |= 0xFF000000;
            }
            return color;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
