package com.abaan404.boatrace;

import com.abaan404.boatrace.screen.PitBoxGui;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class BoatRaceCommands {
    static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("pit").executes(BoatRaceCommands::pit));
        });
    }

    private static int pit(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();

        try {
            PitBoxGui gui = new PitBoxGui(player);
            gui.open();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
