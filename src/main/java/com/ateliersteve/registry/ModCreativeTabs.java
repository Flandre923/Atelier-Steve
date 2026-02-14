package com.ateliersteve.registry;

import com.ateliersteve.AtelierSteve;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AtelierSteve.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ATELIER_TAB = CREATIVE_TABS.register(
            "atelier_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.atelier_steve"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.GATHERING_BASKET.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.GATHERING_BASKET.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
