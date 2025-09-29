package com.abaan404.boatrace.boatrace.game.timetrial;

import com.abaan404.boatrace.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.boatrace.game.gameplay.SplitsManager;
import com.abaan404.boatrace.boatrace.game.maps.TrackMap;

import it.unimi.dsi.fastutil.floats.FloatCollection;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameSpace;

/**
 * Handles time trial state.
 */
public class TimeTrialStageManager {
    private final TimeTrialSpawnLogic spawnLogic;
    private final CheckpointsManager checkpoints;
    private final SplitsManager splits;

    public TimeTrialStageManager(ServerWorld world, TrackMap trackMap) {
        this.checkpoints = new CheckpointsManager(trackMap);
        this.splits = new SplitsManager();
        this.spawnLogic = new TimeTrialSpawnLogic(world);
    }

    public TickResult tick(GameSpace gameSpace, ServerWorld world) {
        gameSpace.getPlayers().participants()
                .forEach(participant -> {
                    CheckpointsManager.TickResult result = this.checkpoints.tick(participant);
                    this.splits.tick(participant, world.getTickManager());

                    switch (result) {
                        case FINISH:
                            this.splits.reset(participant);
                            break;

                        case CHECKPOINT:
                            this.splits.recordSplit(participant);
                            break;

                        case IDLE:
                            break;
                    }
                });

        return TickResult.IDLE;
    }

    public void spawnPlayer(ServerPlayerEntity player) {
        BlockBounds checkpoint = this.checkpoints.getCheckpoint(player);

        this.spawnLogic.resetPlayer(player);
        this.spawnLogic.spawnPlayer(player, checkpoint);
    }

    public int getLaps(ServerPlayerEntity player) {
        return this.checkpoints.getLaps(player);
    }

    public float getTimer(ServerPlayerEntity player) {
        return this.splits.getTimer(player);
    }

    public FloatCollection getSplits(ServerPlayerEntity player) {
        return this.splits.getSplits(player);
    }

    public enum TickResult {
        IDLE,
    }
}
