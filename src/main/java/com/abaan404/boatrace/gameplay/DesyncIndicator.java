package com.abaan404.boatrace.gameplay;

import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

import com.abaan404.boatrace.BoatRacePlayer;
import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;

public class DesyncIndicator {
    private final GameSpace gameSpace;
    private final ServerLevel world;
    private final Map<BoatRacePlayer, Entity> indicator = new Object2ObjectOpenHashMap<>();

    private DesyncIndicator(GameSpace gameSpace, ServerLevel world) {
        this.gameSpace = gameSpace;
        this.world = world;
    }

    public static void addTo(GameActivity game, ServerLevel world) {
        DesyncIndicator desyncIndicator = new DesyncIndicator(game.getGameSpace(), world);

        game.listen(GameActivityEvents.TICK, desyncIndicator::onTick);
    }

    private void onTick() {
        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            // does not have a vehicle or vehicle is not a vehicle
            if (!player.isPassenger() || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
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
            entity.snapTo(vehicle.position(), 0.0f, 0.0f);
        }
    }

    private Entity spawnEntity() {
        // use a lighting rod since it seems to be standard
        Display.BlockDisplay entity = EntityType.BLOCK_DISPLAY.create(this.world, EntitySpawnReason.COMMAND);
        Transformation transformation = new Transformation(
                new Vector3f(-0.5f, 0.0f, -0.5f),
                null,
                new Vector3f(0.75f, 0.75f, 0.75f),
                null);

        entity.setBlockState(Blocks.LIGHTNING_ROD.defaultBlockState());
        entity.setTransformation(transformation);

        this.world.addFreshEntity(entity);
        return entity;
    }

    private void despawnEntity(Entity entity) {
        if (entity != null) {
            entity.kill(this.world);
        }
    }
}
