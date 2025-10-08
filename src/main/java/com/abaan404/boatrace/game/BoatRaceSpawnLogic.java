package com.abaan404.boatrace.game;

import java.util.Optional;
import java.util.Set;

import com.abaan404.boatrace.maps.TrackMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * Manage entity spawns in the world.
 */
public class BoatRaceSpawnLogic {
    private final ServerWorld world;

    public BoatRaceSpawnLogic(ServerWorld world) {
        this.world = world;
    }

    /**
     * Resets the player.
     *
     * @param player The player.
     */
    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.changeGameMode(gameMode);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0f;
    }

    /**
     * Spawn and mount a boat on the player.
     *
     * @param player The player to mount.
     * @return The entity spawned.
     */
    public Optional<VehicleEntity> spawnVehicleAndRide(ServerPlayerEntity player) {
        BoatEntity boat = EntityType.OAK_BOAT.create(this.world, SpawnReason.COMMAND);
        if (boat == null) {
            return Optional.empty();
        }

        boat.refreshPositionAndAngles(player.getPos(), player.getYaw(), player.getPitch());
        this.world.spawnEntity(boat);
        player.startRiding(boat);
        return Optional.of(boat);
    }

    /**
     * Spawn a player in the world on a solid block within a respawn region.
     *
     * @param player  The player.
     * @param respawn The region to spawn in.
     */
    public void spawnPlayer(ServerPlayerEntity player, TrackMap.RespawnRegion respawn) {
        BlockPos center = BlockPos.ofFloored(respawn.bounds().center());
        BlockPos spawn = center;

        // find a solid ground from the center
        while (spawn.getY() >= respawn.bounds().min().getY()) {
            if (!this.world.getBlockState(spawn).isAir()) {
                spawn = spawn.up();
                break;
            }
            spawn = spawn.down();
        }

        // no solid groud, spawn mid air
        if (this.world.getBlockState(spawn.down()).isAir()) {
            spawn = center;
        }

        // avoid accidental stray boats
        Entity boat = player.getVehicle();
        if (boat != null) {
            boat.kill(null);
        }

        player.networkHandler.requestTeleport(new PlayerPosition(
                spawn.toBottomCenterPos(),
                Vec3d.ZERO,
                respawn.respawnYaw(),
                respawn.respawnPitch()), Set.of());
    }
}
