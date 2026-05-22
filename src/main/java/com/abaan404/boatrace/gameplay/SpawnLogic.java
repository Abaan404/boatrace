package com.abaan404.boatrace.gameplay;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import com.abaan404.boatrace.BoatRaceTrack;

/**
 * Manage entity spawns in the world.
 */
public class SpawnLogic {
    private final ServerLevel world;

    public SpawnLogic(ServerLevel world) {
        this.world = world;
    }

    /**
     * Resets the player.
     *
     * @param player The player.
     */
    public void resetPlayer(ServerPlayer player, GameType gameMode) {
        player.setGameMode(gameMode);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
    }

    /**
     * Spawn and mount a boat on the entity.
     *
     * @param entity The entity to mount.
     * @return The boat entity spawned.
     */
    public Optional<Boat> spawnVehicleAndRide(Entity entity) {
        Boat boat = EntityType.OAK_BOAT.create(this.world, EntitySpawnReason.COMMAND);
        if (boat == null) {
            return Optional.empty();
        }

        boat.snapTo(entity.position(), entity.getYRot(), entity.getXRot());
        this.world.addFreshEntity(boat);
        entity.startRiding(boat);
        return Optional.of(boat);
    }

    /**
     * Unmount and kill the vehicle the entity is riding and all further ridden
     * entities.
     *
     * @param entity The entity to dismount.
     */
    public void despawnVehicle(Entity entity) {
        if (entity.isPassenger()) {
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
    public void spawnPlayer(ServerPlayer player, BoatRaceTrack.RespawnRegion respawn) {
        BlockPos center = BlockPos.containing(respawn.bounds().center());
        BlockPos spawn = center;

        // find a solid ground from the center
        boolean solidBlockFound = false;
        while (spawn.getY() >= respawn.bounds().min().getY()) {
            if (!this.world.getBlockState(spawn.below()).isAir()) {
                solidBlockFound = true;
                break;
            }
            spawn = spawn.below();
        }

        if (!solidBlockFound) {
            spawn = center;
        }

        // avoid accidental stray boats
        this.despawnVehicle(player);

        player.connection.teleport(new PositionMoveRotation(
                spawn.getBottomCenter(),
                Vec3.ZERO,
                respawn.yaw(),
                respawn.pitch()), Set.of());
    }

    /**
     * Freeze the vehicle by making it ride an entity.
     *
     * @param player The player's boat to freeze.
     * @return The entity the boat is now riding.
     */
    public Optional<Entity> freezeVehicle(ServerPlayer player) {
        Entity boat = player.getVehicle();
        if (boat == null) {
            return Optional.empty();
        }

        if (boat.isPassenger()) {
            return Optional.of(boat.getVehicle());
        }

        AreaEffectCloud aec = EntityType.AREA_EFFECT_CLOUD.create(this.world, EntitySpawnReason.COMMAND);
        if (aec == null) {
            return Optional.empty();
        }

        aec.setCustomParticle(ParticleTypes.DUST_PLUME); // why not
        aec.setRadius(1.0f);
        aec.snapTo(player.position(), player.getYRot(), player.getXRot());
        this.world.addFreshEntity(aec);
        boat.startRiding(aec);
        return Optional.of(aec);
    }

    /**
     * Unfreeze the vehicle by killing the entity its riding.
     *
     * @param player THe player's boat to unfreeze.
     */
    public void unfreezeVehicle(ServerPlayer player) {
        Entity boat = player.getVehicle();
        if (boat == null) {
            return;
        }

        this.despawnVehicle(boat);
    }
}
