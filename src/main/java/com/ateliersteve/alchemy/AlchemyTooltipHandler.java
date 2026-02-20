package com.ateliersteve.alchemy;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.registry.ModDataComponents;
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
        AlchemyTooltipText.appendAlchemyDetails(tooltip, data);
    }
}
