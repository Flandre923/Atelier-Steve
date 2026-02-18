package com.ateliersteve.registry;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.block.AlchemyCauldronBlock;
import com.ateliersteve.block.GatheringBasketBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AtelierSteve.MODID);

    public static final DeferredBlock<GatheringBasketBlock> GATHERING_BASKET = BLOCKS.register(
            "gathering_basket",
            GatheringBasketBlock::new
    );

    public static final DeferredBlock<AlchemyCauldronBlock> ALCHEMY_CAULDRON = BLOCKS.register(
            "alchemy_cauldron",
            AlchemyCauldronBlock::new
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
