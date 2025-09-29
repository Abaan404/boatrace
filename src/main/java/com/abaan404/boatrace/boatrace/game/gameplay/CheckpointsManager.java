package com.abaan404.boatrace.boatrace.game.gameplay;

import com.abaan404.boatrace.boatrace.game.maps.TrackMap;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

/**
 * Keeps track of checkpoints and verifies if the player crossed the correct
 * checkpoints in order for a track.
 */
public class CheckpointsManager {
    private final TrackMap.Regions regions;

    private Object2IntMap<PlayerRef> checkpoints = new Object2IntOpenHashMap<>();
    private Object2IntMap<PlayerRef> laps = new Object2IntOpenHashMap<>();

    /**
     * A checkpoint manager.
     *
     * @param map The track to use this on.
     */
    public CheckpointsManager(TrackMap map) {
        this.regions = map.getRegions();
    }

    /**
     * Ticks the player to update and verify checkpoints and trigger relevant
     * results.
     *
     * @param player The player to tick.
     * @return The resulting tick.
     */
    public TickResult tick(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);

        // no checkpoints, this is just an adhoc implementation just to avoid division
        // by zero
        if (this.regions.checkpoints().size() == 0) {
            // only check finish, may cause alot of laps to get counted
            if (this.regions.finish().contains(player.getBlockPos())) {
                this.laps.put(ref, this.laps.getOrDefault(ref, 0) + 1);
                return TickResult.FINISH;
            }

            return TickResult.IDLE;
        }

        int prevCheckpointIdx = this.checkpoints.getOrDefault(ref, -1);
        int nextCheckpointIdx = (prevCheckpointIdx + 1) % this.regions.checkpoints().size();

        // player has reached the next checkpoint
        if (this.regions.checkpoints().get(nextCheckpointIdx).contains(player.getBlockPos())) {
            this.checkpoints.put(ref, nextCheckpointIdx);

            return TickResult.CHECKPOINT;
        }

        // player has claimed every checkpoint and has reached the finish
        if (this.regions.finish().contains(player.getBlockPos())
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
    public void reset(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        this.checkpoints.removeInt(ref);
        this.laps.removeInt(ref);
    }

    /**
     * Get the laps completed for the player.
     *
     * @param player The player to check.
     * @return Their laps.
     */
    public int getLaps(ServerPlayerEntity player) {
        return this.laps.getOrDefault(PlayerRef.of(player), 0);
    }

    /**
     * Get the last checkpoint the player used, or the checkpoint before the finish.
     *
     * @param player The player to get from.
     * @return The bounds of their relevant checkpoint.
     */
    public BlockBounds getCheckpoint(ServerPlayerEntity player) {
        // no checkpoints, return finish
        if (this.regions.checkpoints().size() == 0) {
            return this.regions.finish();
        }

        // return the last checkpoint if the player has just spawned
        int prevCheckpointIdx = this.checkpoints.getOrDefault(PlayerRef.of(player),
                this.regions.checkpoints().size() - 1);

        return this.regions.checkpoints().get(prevCheckpointIdx);
    }

    public TrackMap.Regions getRegions() {
        return this.regions;
    }

    public enum TickResult {
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
