package com.ateliersteve.dev;

import com.ateliersteve.AtelierSteve;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;

public final class DevUiCommand {
    private DevUiCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("atelierdev")
                        .then(Commands.literal("showcase")
                                .executes(context -> {
                                    var player = context.getSource().getPlayerOrException();
                                    PlayerUIMenuType.openUI(player, AtelierSteve.id("dev_showcase"));
                                    return 1;
                                }))
                        .then(Commands.literal("grid")
                                .executes(context -> {
                                    var player = context.getSource().getPlayerOrException();
                                    PlayerUIMenuType.openUI(player, AtelierSteve.id("dev_grid_test"));
                                    return 1;
                                }))
        );
    }
}
