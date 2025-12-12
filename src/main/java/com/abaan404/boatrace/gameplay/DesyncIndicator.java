package com.abaan404.boatrace.gameplay;

import java.util.Map;

import org.joml.Vector3f;

import com.abaan404.boatrace.BoatRacePlayer;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.AffineTransformation;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;

public class DesyncIndicator {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final Map<BoatRacePlayer, Entity> indicator = new Object2ObjectOpenHashMap<>();

    private DesyncIndicator(GameSpace gameSpace, ServerWorld world) {
        this.gameSpace = gameSpace;
        this.world = world;
    }

    public static void addTo(GameActivity game, ServerWorld world) {
        DesyncIndicator desyncIndicator = new DesyncIndicator(game.getGameSpace(), world);

        game.listen(GameActivityEvents.TICK, desyncIndicator::onTick);
    }

    private void onTick() {
        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            // does not have a vehicle or vehicle is not a vehicle
            if (!player.hasVehicle() || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
                if (this.indicator.containsKey(bPlayer)) {
                    this.despawnEntity(this.indicator.get(bPlayer));
                    this.indicator.remove(bPlayer);
                }
                continue;
            }

            if (!this.indicator.containsKey(bPlayer)) {
                this.indicator.put(bPlayer, this.spawnEntity());
            }

            Entity entity = this.indicator.get(bPlayer);
            entity.refreshPositionAndAngles(vehicle.getPos(), 0.0f, 0.0f);
        }
    }

    private Entity spawnEntity() {
        // use a lighting rod since it seems to be standard
        DisplayEntity.BlockDisplayEntity entity = EntityType.BLOCK_DISPLAY.create(this.world, SpawnReason.COMMAND);
        AffineTransformation transformation = new AffineTransformation(
                new Vector3f(-0.5f, 0.0f, -0.5f),
                null,
                new Vector3f(0.75f, 0.75f, 0.75f),
                null);

        entity.setBlockState(Blocks.LIGHTNING_ROD.getDefaultState());
        entity.setTransformation(transformation);

        this.world.spawnEntity(entity);
        return entity;
    }

    private void despawnEntity(Entity entity) {
        if (entity != null) {
            entity.kill(this.world);
        }
    }
}
