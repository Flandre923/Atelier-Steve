package com.ateliersteve;

import com.ateliersteve.registry.ModBlockEntities;
import com.ateliersteve.registry.ModBlocks;
import com.ateliersteve.registry.ModCreativeTabs;
import com.ateliersteve.registry.ModDataComponents;
import com.ateliersteve.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AtelierSteve.MODID)
public class AtelierSteve {
    public static final String MODID = "atelier_steve";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AtelierSteve(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModDataComponents.register(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
