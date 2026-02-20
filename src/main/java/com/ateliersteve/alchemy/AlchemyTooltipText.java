package com.ateliersteve.alchemy;

import com.ateliersteve.alchemy.element.AlchemyElement;
import com.ateliersteve.alchemy.element.CellType;
import com.ateliersteve.alchemy.category.AlchemyCategoryRegistry;
import com.ateliersteve.alchemy.trait.AlchemyTrait;
import com.ateliersteve.alchemy.trait.TraitDefinition;
import com.ateliersteve.alchemy.trait.TraitRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AlchemyTooltipText {
    private AlchemyTooltipText() {
    }

    public static List<Component> buildStackTooltip(ItemStack stack, boolean includeItemName) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        List<Component> tooltip = new ArrayList<>();
        if (includeItemName) {
            tooltip.add(stack.getHoverName());
        }
        AlchemyItemData data = stack.get(com.ateliersteve.registry.ModDataComponents.ALCHEMY_DATA.get());
        appendAlchemyDetails(tooltip, data);
        return List.copyOf(tooltip);
    }

    public static void appendAlchemyDetails(List<Component> tooltip, AlchemyItemData data) {
        if (tooltip == null || data == null || data.isEmpty()) {
            return;
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.atelier_steve.alchemy_data")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        if (data.cole() > 0) {
            tooltip.add(Component.translatable("tooltip.atelier_steve.cole", data.cole())
                    .withStyle(ChatFormatting.YELLOW));
        }

        if (data.quality() > 0) {
            tooltip.add(Component.translatable("tooltip.atelier_steve.quality", data.quality())
                    .withStyle(ChatFormatting.YELLOW));
        }

        if (!data.categories().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.atelier_steve.categories")
                    .withStyle(ChatFormatting.GREEN));
            for (var categoryId : data.categories()) {
                tooltip.add(Component.literal("  - ")
                        .append(Component.translatable(AlchemyCategoryRegistry.resolveTranslationKey(categoryId)))
                        .withStyle(ChatFormatting.WHITE));
            }
        }

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

        if (!data.elements().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.atelier_steve.elements")
                    .withStyle(ChatFormatting.AQUA));
            for (var element : data.elements()) {
                AlchemyElement elem = element.element();
                int color = elem.getColor();
                var shape = element.shape();

                tooltip.add(Component.literal("  ")
                        .append(elem.getDisplayName())
                        .withStyle(style -> style.withColor(color)));

                for (int y = 0; y < shape.getHeight(); y++) {
                    MutableComponent row = Component.literal("  ");
                    for (int x = 0; x < shape.getWidth(); x++) {
                        CellType cell = shape.getCellAt(x, y);
                        String symbol = switch (cell) {
                            case NORMAL -> "■";
                            case LINK -> "★";
                            case EMPTY -> "░";
                        };
                        String cellText = symbol + " ";
                        row.append(
                                cell == CellType.EMPTY
                                        ? Component.literal(cellText).withStyle(ChatFormatting.DARK_GRAY)
                                        : Component.literal(cellText).withStyle(style -> style.withColor(color))
                        );
                    }
                    tooltip.add(row);
                }
            }
        }
    }
}
