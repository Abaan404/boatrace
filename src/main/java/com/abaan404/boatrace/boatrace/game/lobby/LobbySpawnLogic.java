package com.abaan404.boatrace.boatrace.game.lobby;

import net.minecraft.util.math.Vec3d;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

import java.util.Set;

import com.abaan404.boatrace.boatrace.game.maps.LobbyMap;

/**
 * Handle's spawning in the lobby.
 */
public class LobbySpawnLogic {
    private final LobbyMap map;
    private final ServerWorld world;

    public LobbySpawnLogic(ServerWorld world, LobbyMap map) {
        this.map = map;
        this.world = world;
    }

    /**
     * Resets the player.
     *
     * @param player The player.
     * @param gameMode The gamemode to apply.
     */
    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.changeGameMode(gameMode);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0f;
    }

    /**
     * Spawns the player at the map's spawn.
     *
     * @param player The player to spawn.
     */
    public void spawnPlayer(ServerPlayerEntity player) {
        BlockPos pos = this.map.getRegions().spawn();

        float radius = 4.5f;
        float x = pos.getX() + MathHelper.nextFloat(player.getRandom(), -radius, radius);
        float z = pos.getZ() + MathHelper.nextFloat(player.getRandom(), -radius, radius);

        player.teleport(this.world, x, pos.getY(), z, Set.of(), 0.0F, 0.0F, true);
    }
}
