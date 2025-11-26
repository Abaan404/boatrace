package com.abaan404.boatrace.compat.openboatutils;

import java.util.List;
import java.util.Set;

import com.abaan404.boatrace.BoatRacePlayer;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class OBU {
    public static final List<Integer> REJECTED_VERSIONS = List.of(12, 8);

    private OBU() {
    }

    public static void initialize() {
        OBUPackets.initialize();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            BoatRacePlayer player = BoatRacePlayer.of(handler.player);
            OBUPlayers.remove(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            BoatRacePlayer player = BoatRacePlayer.of(handler.player);
            OBUPlayers.remove(player);
        });

        ServerPlayNetworking.registerGlobalReceiver(OBUPackets.VersionC2SPayload.ID, (payload, context) -> {
            if (REJECTED_VERSIONS.contains(payload.version())) {
                return;
            }

            BoatRacePlayer player = BoatRacePlayer.of(context.player());

            OBUPlayers.add(player);
        });
    }

    private static final Set<BoatRacePlayer> OBUPlayers = new ObjectOpenHashSet<>();

    public static boolean foundOpenBoatUtils(ServerPlayerEntity player) {
        return OBUPlayers.contains(BoatRacePlayer.of(player));
    }
}
