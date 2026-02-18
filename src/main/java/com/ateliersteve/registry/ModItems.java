package com.ateliersteve.registry;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.item.AlchemyItem;
import com.ateliersteve.alchemy.item.AlchemyItemDefinitions;
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

    public static final DeferredItem<BlockItem> ALCHEMY_CAULDRON = ITEMS.registerSimpleBlockItem(
            "alchemy_cauldron",
            ModBlocks.ALCHEMY_CAULDRON
    );

    public static final DeferredItem<Item> NEUTRALIZER_RED = ITEMS.register(
            "neutralizer_red",
            () -> new AlchemyItem(
                    10,
                    AlchemyItemDefinitions.NEUTRALIZER_RED_BASE_DATA,
                    AlchemyItemDefinitions.NEUTRALIZER_RED_EFFECTS,
                    new Item.Properties()
            )
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
