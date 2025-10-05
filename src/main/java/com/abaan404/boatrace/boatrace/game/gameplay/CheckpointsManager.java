package com.abaan404.boatrace.boatrace.game.gameplay;

import java.util.Map;

import com.abaan404.boatrace.boatrace.game.maps.TrackMap;
import com.abaan404.boatrace.boatrace.game.maps.TrackMap.RespawnRegion;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

/**
 * Keeps track of checkpoints and verifies if the player crossed the correct
 * checkpoints in order for a track.
 */
public class CheckpointsManager {
    private final TrackMap.Regions regions;

    private Map<PlayerRef, Integer> checkpoints = new Object2IntOpenHashMap<>();
    private Map<PlayerRef, Integer> laps = new Object2IntOpenHashMap<>();

    public CheckpointsManager(TrackMap track) {
        this.regions = track.getRegions();
    }

    /**
     * Ticks the player to update and verify checkpoints and trigger relevant
     * results.
     *
     * @param player The player to tick.
     * @return The resulting tick event.
     */
    public TickResult tick(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);

        // no checkpoints, this is just an adhoc implementation just to avoid division
        // by zero
        if (this.regions.checkpoints().size() == 0) {
            // only check finish, will cause alot of laps to get counted
            if (this.regions.finish().bounds().contains(player.getBlockPos())) {
                this.laps.put(ref, this.laps.getOrDefault(ref, 0) + 1);
                return TickResult.FINISH;
            }

            return TickResult.IDLE;
        }

        int prevCheckpointIdx = getCheckpointIndex(ref);
        int nextCheckpointIdx = (prevCheckpointIdx + 1) % this.regions.checkpoints().size();

        // player just started the run
        if (prevCheckpointIdx == -1 && this.regions.finish().bounds().contains(player.getBlockPos())) {
            return TickResult.BEGIN;
        }

        // player has reached the next checkpoint
        else if (this.regions.checkpoints().get(nextCheckpointIdx).bounds().contains(player.getBlockPos())) {
            this.checkpoints.put(ref, nextCheckpointIdx);
            return TickResult.CHECKPOINT;
        }

        // player has claimed every checkpoint and has reached the finish
        else if (this.regions.finish().bounds().contains(player.getBlockPos())
                && prevCheckpointIdx == this.regions.checkpoints().size() - 1) {
            this.laps.put(ref, this.laps.getOrDefault(ref, 0) + 1);
            this.checkpoints.put(ref, -1);
            return TickResult.FINISH;
        }

        return TickResult.IDLE;
    }

    /**
     * Reset checkpoint data for a player.
     *
     * @param player The player to reset.
     */
    public void reset(PlayerRef player) {
        this.checkpoints.remove(player);
        this.laps.remove(player);
    }

    /**
     * Get the laps completed for the player.
     *
     * @param player The player to check.
     * @return Their laps.
     */
    public int getLaps(PlayerRef player) {
        return this.laps.getOrDefault(player, 0);
    }

    /**
     * Get the last checkpoint the player used
     *
     * @param player The player to get from.
     * @return The bounds of their relevant checkpoint.
     */
    public RespawnRegion getCheckpoint(PlayerRef player) {
        // no checkpoints, return finish
        if (this.regions.checkpoints().size() == 0) {
            return this.regions.finish();
        }

        // return the last checkpoint if the player has just spawned
        int checkpointIdx = getCheckpointIndex(player);
        if (checkpointIdx == -1) {
            return this.regions.finish();
        }

        return this.regions.checkpoints().get(checkpointIdx);
    }

    public int getCheckpointIndex(PlayerRef player) {
        return this.checkpoints.getOrDefault(player, -1);
    }

    /**
     * Get regions for this track.
     *
     * @return The track regions.
     */
    public TrackMap.Regions getRegions() {
        return this.regions;
    }

    public enum TickResult {
        /**
         * Begin the lap.
         */
        BEGIN,

        /**
         * Valid lap, lap incremented.
         */
        FINISH,

        /**
         * Crossed a valid checkpoint.
         */
        CHECKPOINT,

        /**
         * Nothing happened
         */
        IDLE,
    }
}
