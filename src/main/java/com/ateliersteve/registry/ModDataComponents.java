package com.ateliersteve.registry;

import com.ateliersteve.AtelierSteve;
import com.ateliersteve.alchemy.AlchemyItemData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AtelierSteve.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AlchemyItemData>> ALCHEMY_DATA =
            DATA_COMPONENTS.register("alchemy_data", () -> DataComponentType.<AlchemyItemData>builder()
                    .persistent(AlchemyItemData.CODEC)
                    .networkSynchronized(AlchemyItemData.STREAM_CODEC)
                    .build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
