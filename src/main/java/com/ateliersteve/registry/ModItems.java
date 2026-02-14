package com.ateliersteve.registry;

import com.ateliersteve.AtelierSteve;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AtelierSteve.MODID);

    public static final DeferredItem<BlockItem> GATHERING_BASKET = ITEMS.registerSimpleBlockItem(
            "gathering_basket",
            ModBlocks.GATHERING_BASKET
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
