package com.abaan404.boatrace.boatrace.game.timetrial;

import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.util.Set;

/**
 * Handle's spawning in the lobby.
 */
public class TimeTrialSpawnLogic {
    private final ServerWorld world;

    public TimeTrialSpawnLogic(ServerWorld world) {
        this.world = world;
    }

    /**
     * Resets the player.
     *
     * @param player The player.
     */
    public void resetPlayer(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.ADVENTURE);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0f;
    }

    /**
     * Spawns the player at a safe location in a checkpoint.
     *
     * @param player     The player to spawn.
     * @param checkpoint the checkpoint to spawn at.
     */
    public void spawnPlayer(ServerPlayerEntity player, BlockBounds checkpoint) {
        BlockPos center = BlockPos.ofFloored(checkpoint.center());
        BlockPos spawn = center;

        // find a solid ground from the center
        while (spawn.getY() >= checkpoint.min().getY()) {
            if (!this.world.getBlockState(spawn).isAir()) {
                spawn = spawn.up();
                break;
            }
            spawn = spawn.down();
        }

        // no solid groud, spawn mid air
        if (spawn.getY() < checkpoint.min().getY()) {
            spawn = center;
        }

        player.teleport(this.world, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, Set.of(), 0.0F, 0.0F, true);
    }
}
