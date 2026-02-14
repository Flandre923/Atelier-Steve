package com.ateliersteve.registry;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.block.GatheringBasketBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AtelierSteve.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GatheringBasketBlockEntity>> GATHERING_BASKET =
            BLOCK_ENTITIES.register("gathering_basket", () -> BlockEntityType.Builder.of(
                    GatheringBasketBlockEntity::new,
                    ModBlocks.GATHERING_BASKET.get()
            ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
