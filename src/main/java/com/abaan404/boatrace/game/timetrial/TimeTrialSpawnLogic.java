package com.abaan404.boatrace.game.timetrial;

import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

import com.abaan404.boatrace.game.maps.TrackMap.RespawnRegion;

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
     * @param player      The player to spawn.
     * @param world       The world to use.
     * @param respawn     the region to spawn at.
     * @param withVehicle should the player spawn in the vehicle
     */
    public void spawnPlayer(ServerPlayerEntity player, ServerWorld world, RespawnRegion respawn, boolean withVehicle) {
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

        // kill before teleporting
        if (player.hasVehicle()) {
            BoatEntity boat = (BoatEntity) player.getVehicle();
            if (boat != null) {
                boat.kill(null);
            }
        }

        player.teleportTo(new TeleportTarget(
                this.world,
                spawn.toBottomCenterPos(),
                Vec3d.ZERO,
                respawn.respawnYaw(),
                respawn.respawnPitch(),
                TeleportTarget.NO_OP));

        if (withVehicle) {
            BoatEntity boat = EntityType.OAK_BOAT.create(world, SpawnReason.COMMAND);
            if (boat != null) {
                boat.refreshPositionAndAngles(
                        spawn.toBottomCenterPos(),
                        respawn.respawnYaw(),
                        respawn.respawnPitch());

                world.spawnEntity(boat);
                player.startRiding(boat);
            }
        }
    }
}
