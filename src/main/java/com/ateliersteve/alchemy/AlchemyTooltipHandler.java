package com.ateliersteve.alchemy;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.element.AlchemyElement;
import com.ateliersteve.alchemy.element.CellType;
import com.ateliersteve.alchemy.trait.AlchemyTrait;
import com.ateliersteve.alchemy.trait.TraitDefinition;
import com.ateliersteve.alchemy.trait.TraitRegistry;
import com.ateliersteve.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = AtelierSteve.MODID, value = Dist.CLIENT)
public class AlchemyTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        AlchemyItemData data = stack.get(ModDataComponents.ALCHEMY_DATA.get());

        if (data == null || data.isEmpty()) {
            return;
        }

        List<Component> tooltip = event.getToolTip();

        // Add separator
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.atelier_steve.alchemy_data")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        // Add traits
        if (!data.traits().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.atelier_steve.traits")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            for (var traitInstance : data.traits()) {
                Component name = null;

                AlchemyTrait customTrait = traitInstance.getTrait();
                if (customTrait != null) {
                    name = customTrait.getName();
                } else {
                    TraitDefinition definition = TraitRegistry.get(traitInstance.traitId());
                    if (definition != null) {
                        name = definition.getName();
                    }
                }

                if (name != null) {
                    tooltip.add(Component.literal("  - ")
                            .append(name)
                            .withStyle(ChatFormatting.WHITE));
                }
            }
        }

        // Add elements with shape visualization
        if (!data.elements().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.atelier_steve.elements")
                    .withStyle(ChatFormatting.AQUA));
            for (var element : data.elements()) {
                AlchemyElement elem = element.element();
                int color = elem.getColor();
                var shape = element.shape();

                // Element name
                tooltip.add(Component.literal("  ")
                        .append(elem.getDisplayName())
                        .withStyle(style -> style.withColor(color)));

                // Render each row of the shape grid
                for (int y = 0; y < shape.getHeight(); y++) {
                    // Find last non-empty cell to trim trailing spaces
                    int lastFilled = -1;
                    for (int x = shape.getWidth() - 1; x >= 0; x--) {
                        if (shape.getCellAt(x, y) != CellType.EMPTY) {
                            lastFilled = x;
                            break;
                        }
                    }

                    if (lastFilled < 0) continue; // skip fully empty rows

                    MutableComponent row = Component.literal("  ");
                    for (int x = 0; x <= lastFilled; x++) {
                        CellType cell = shape.getCellAt(x, y);
                        String symbol = switch (cell) {
                            case NORMAL -> "\u25A0"; // ■
                            case LINK -> "\u2605";   // ★
                            case EMPTY -> " ";
                        };
                        row.append(symbol);
                    }
                    tooltip.add(row.withStyle(style -> style.withColor(color)));
                }
            }
        }
    }
}
