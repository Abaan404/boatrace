package com.abaan404.boatrace;

import com.abaan404.boatrace.screen.PitBoxGui;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class BoatRaceCommands {
    static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("pit").executes(BoatRaceCommands::pit));
        });
    }

    private static int pit(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();

        try {
            PitBoxGui gui = new PitBoxGui(player);
            gui.open();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
