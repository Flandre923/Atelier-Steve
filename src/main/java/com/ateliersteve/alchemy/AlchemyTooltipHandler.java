package com.ateliersteve.alchemy;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.element.AlchemyElement;
import com.ateliersteve.alchemy.trait.AlchemyTrait;
import com.ateliersteve.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
                AlchemyTrait trait = traitInstance.getTrait();
                if (trait != null) {
                    tooltip.add(Component.literal("  - ")
                            .append(trait.getName())
                            .withStyle(ChatFormatting.WHITE));
                }
            }
        }

        // Add elements
        if (!data.elements().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.atelier_steve.elements")
                    .withStyle(ChatFormatting.AQUA));
            for (var element : data.elements()) {
                AlchemyElement elem = element.element();
                int color = elem.getColor();
                int normalCount = element.getNormalCount();
                int linkCount = element.getLinkCount();
                var shape = element.shape();

                // Format: "  - Fire [2x2] Normal:3 Link:1"
                var line = Component.literal("  - ")
                        .append(elem.getDisplayName())
                        .append(Component.literal(" [" + shape.getWidth() + "x" + shape.getHeight() + "] "))
                        .append(Component.translatable("tooltip.atelier_steve.normal_count", normalCount))
                        .append(Component.literal(" "))
                        .append(Component.translatable("tooltip.atelier_steve.link_count", linkCount));
                tooltip.add(line.withStyle(style -> style.withColor(color)));
            }
        }
    }
}
