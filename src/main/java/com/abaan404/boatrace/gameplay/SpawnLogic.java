package com.abaan404.boatrace.gameplay;

import java.util.Optional;
import java.util.Set;

import com.abaan404.boatrace.BoatRaceTrack;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * Manage entity spawns in the world.
 */
public class SpawnLogic {
    private final ServerWorld world;

    public SpawnLogic(ServerWorld world) {
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
     * Spawn and mount a boat on the entity.
     *
     * @param entity The entity to mount.
     * @return The boat entity spawned.
     */
    public Optional<BoatEntity> spawnVehicleAndRide(Entity entity) {
        BoatEntity boat = EntityType.OAK_BOAT.create(this.world, SpawnReason.COMMAND);
        if (boat == null) {
            return Optional.empty();
        }

        boat.refreshPositionAndAngles(entity.getPos(), entity.getYaw(), entity.getPitch());
        this.world.spawnEntity(boat);
        entity.startRiding(boat, true);
        return Optional.of(boat);
    }

    /**
     * Unmount and kill the vehicle the entity is riding and all further ridden
     * entities.
     *
     * @param entity The entity to dismount.
     */
    public void despawnVehicle(Entity entity) {
        if (entity.hasVehicle()) {
            Entity vehicle = entity.getVehicle();
            this.despawnVehicle(vehicle);

            vehicle.stopRiding();
            vehicle.kill(this.world);
        }
    }

    /**
     * Spawn a player in the world on a solid block within a respawn region.
     *
     * @param player  The player.
     * @param respawn The region to spawn in.
     */
    public void spawnPlayer(ServerPlayerEntity player, BoatRaceTrack.RespawnRegion respawn) {
        BlockPos center = BlockPos.ofFloored(respawn.bounds().center());
        BlockPos spawn = center;

        // find a solid ground from the center
        boolean solidBlockFound = false;
        while (spawn.getY() >= respawn.bounds().min().getY()) {
            if (!this.world.getBlockState(spawn.down()).isAir()) {
                solidBlockFound = true;
                break;
            }
            spawn = spawn.down();
        }

        if (!solidBlockFound) {
            spawn = center;
        }

        // avoid accidental stray boats
        this.despawnVehicle(player);

        player.networkHandler.requestTeleport(new PlayerPosition(
                spawn.toBottomCenterPos(),
                Vec3d.ZERO,
                respawn.yaw(),
                respawn.pitch()), Set.of());
    }

    /**
     * Freeze the vehicle by making it ride an entity.
     *
     * @param player The player's boat to freeze.
     * @return The entity the boat is now riding.
     */
    public Optional<Entity> freezeVehicle(ServerPlayerEntity player) {
        Entity boat = player.getVehicle();
        if (boat == null) {
            return Optional.empty();
        }

        if (boat.hasVehicle()) {
            return Optional.of(boat.getVehicle());
        }

        AreaEffectCloudEntity aec = EntityType.AREA_EFFECT_CLOUD.create(this.world, SpawnReason.COMMAND);
        if (aec == null) {
            return Optional.empty();
        }

        aec.setParticleType(ParticleTypes.DUST_PLUME); // why not
        aec.setRadius(1.0f);
        aec.refreshPositionAndAngles(player.getPos(), player.getYaw(), player.getPitch());
        this.world.spawnEntity(aec);
        boat.startRiding(aec);
        return Optional.of(aec);
    }

    /**
     * Unfreeze the vehicle by killing the entity its riding.
     *
     * @param player THe player's boat to unfreeze.
     */
    public void unfreezeVehicle(ServerPlayerEntity player) {
        Entity boat = player.getVehicle();
        if (boat == null) {
            return;
        }

        this.despawnVehicle(boat);
    }
}
