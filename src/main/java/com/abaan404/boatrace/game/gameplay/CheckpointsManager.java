package com.abaan404.boatrace.game.gameplay;

import java.util.Map;
import java.util.Set;

import com.abaan404.boatrace.maps.TrackMap;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

/**
 * Keeps track of checkpoints and verifies if the player crossed the correct
 * checkpoints in order for a track.
 */
public class CheckpointsManager {
    private final TrackMap track;

    private Map<PlayerRef, Integer> checkpoints = new Object2IntOpenHashMap<>();
    private Map<PlayerRef, Integer> laps = new Object2IntOpenHashMap<>();
    private Set<PlayerRef> began = new ObjectOpenHashSet<>();

    public CheckpointsManager(TrackMap track) {
        this.track = track;
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

        TrackMap.Regions regions = track.getRegions();
        TrackMap.Meta meta = track.getMeta();

        // no checkpoints, do nothing
        if (regions.checkpoints().size() == 0) {
            return TickResult.IDLE;
        }

        int prevCheckpointIdx = this.getCheckpointIndex(ref);
        int nextCheckpointIdx = (prevCheckpointIdx + 1) % regions.checkpoints().size();

        TrackMap.RespawnRegion start = regions.checkpoints().getFirst();

        // player has reached the starting line
        if (!this.began.contains(ref) && start.bounds().contains(player.getBlockPos())) {
            this.began.add(ref);
            this.checkpoints.put(ref, nextCheckpointIdx);
            return TickResult.BEGIN;
        }

        // run hasnt began yet, do nothing
        if (!this.began.contains(ref)) {
            return TickResult.IDLE;
        }

        // reached the next checkpoint
        if (regions.checkpoints().get(nextCheckpointIdx).bounds().contains(player.getBlockPos())) {
            this.checkpoints.put(ref, nextCheckpointIdx);

            switch (meta.layout()) {
                case CIRCULAR: {
                    // checkpoint was looped back to the start
                    if (start.bounds().contains(player.getBlockPos())) {
                        this.checkpoints.remove(ref);
                        this.laps.put(ref, this.laps.getOrDefault(ref, 0) + 1);
                        return TickResult.LOOP;
                    }

                    break;
                }

                case LINEAR: {
                    TrackMap.RespawnRegion finish = regions.checkpoints().getLast();

                    // checkpoint reached the end
                    if (finish.bounds().contains(player.getBlockPos())) {
                        this.checkpoints.remove(ref);
                        this.began.remove(ref);
                        return TickResult.FINISH;
                    }

                    break;
                }
            }

            return TickResult.CHECKPOINT;
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
        this.began.remove(player);
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
    public TrackMap.RespawnRegion getCheckpoint(PlayerRef player) {
        TrackMap.Regions regions = track.getRegions();

        // no checkpoints, return default
        if (regions.checkpoints().size() == 0) {
            return TrackMap.RespawnRegion.of();
        }

        // return the first checkpoint if the player has just spawned
        int checkpointIdx = this.getCheckpointIndex(player);
        if (checkpointIdx == -1) {
            return regions.checkpoints().getFirst();
        }

        return regions.checkpoints().get(checkpointIdx);
    }

    public int getCheckpointIndex(PlayerRef player) {
        return this.checkpoints.getOrDefault(player, -1);
    }

    public enum TickResult {
        /**
         * Begin the lap.
         */
        BEGIN,

        /**
         * Reached the end of a linear track.
         */
        FINISH,

        /**
         * Reached the end and started a new lap.
         */
        LOOP,

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
