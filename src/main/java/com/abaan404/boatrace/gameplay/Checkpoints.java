package com.abaan404.boatrace.gameplay;

import java.util.Map;
import java.util.Set;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Keeps track of checkpoints and verifies if the player crossed the correct
 * checkpoints in order for a track.
 */
public class Checkpoints {
    private final BoatRaceTrack track;

    private Map<BoatRacePlayer, Vec3d> prevPositions = new Object2ObjectOpenHashMap<>();
    private Map<BoatRacePlayer, Integer> checkpoints = new Object2IntOpenHashMap<>();
    private Map<BoatRacePlayer, Integer> laps = new Object2IntOpenHashMap<>();
    private Set<BoatRacePlayer> began = new ObjectOpenHashSet<>();
    private Set<BoatRacePlayer> canPit = new ObjectOpenHashSet<>();
    private Set<BoatRacePlayer> inPit = new ObjectOpenHashSet<>();

    public Checkpoints(BoatRaceTrack track) {
        this.track = track;
    }

    /**
     * Ticks the player to update and verify checkpoints and pits then trigger
     * relevant results.
     *
     * @param player The player to tick.
     * @return The resulting tick event.
     */
    public TickResult tick(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        BoatRaceTrack.Regions regions = track.getRegions();
        BoatRaceTrack.Attributes attributes = track.getAttributes();

        Vec3d pos = player.getEyePos();
        Vec3d prevPos = this.prevPositions.getOrDefault(bPlayer, pos);
        this.prevPositions.put(bPlayer, pos);

        // no checkpoints, do nothing
        if (regions.checkpoints().size() == 0) {
            return TickResult.IDLE;
        }

        int prevCheckpointIdx = this.getCheckpointIndex(bPlayer);
        int nextCheckpointIdx = (prevCheckpointIdx + 1) % regions.checkpoints().size();

        BoatRaceTrack.RespawnRegion start = regions.checkpoints().getFirst();

        // player has reached the starting line
        if (!this.began.contains(bPlayer) && start.intersect(pos, prevPos)) {
            this.laps.put(bPlayer, this.laps.getOrDefault(bPlayer, 0) + 1);
            this.began.add(bPlayer);
            this.checkpoints.put(bPlayer, nextCheckpointIdx);
            this.canPit.add(bPlayer);
            return TickResult.BEGIN;
        }

        // run hasnt began yet, do nothing
        if (!this.began.contains(bPlayer)) {
            return TickResult.IDLE;
        }

        // reached the next checkpoint
        if (regions.checkpoints().get(nextCheckpointIdx).intersect(pos, prevPos)) {
            this.checkpoints.put(bPlayer, nextCheckpointIdx);

            switch (attributes.layout()) {
                case CIRCULAR: {
                    // checkpoint was looped back to the start
                    if (start.intersect(pos, prevPos)) {
                        this.laps.put(bPlayer, this.laps.getOrDefault(bPlayer, 0) + 1);
                        this.canPit.add(bPlayer);
                        return TickResult.LOOP;
                    }

                    break;
                }

                case LINEAR: {
                    BoatRaceTrack.RespawnRegion finish = regions.checkpoints().getLast();

                    // checkpoint reached the end
                    if (finish.intersect(pos, prevPos)) {
                        this.checkpoints.remove(bPlayer);
                        this.began.remove(bPlayer);
                        return TickResult.FINISH;
                    }

                    break;
                }
            }

            return TickResult.CHECKPOINT;
        }

        if (this.canPit.contains(bPlayer)) {
            if (!this.inPit.contains(bPlayer) && regions.pitLane().intersect(pos, prevPos)) {
                this.inPit.add(bPlayer);

                return TickResult.PIT_ENTER;
            }

            if (this.inPit.contains(bPlayer) && !regions.pitLane().intersect(pos, prevPos)) {
                this.inPit.remove(bPlayer);
                this.canPit.remove(bPlayer);

                return TickResult.PIT_EXIT;
            }
        }

        // test if player went to an incorrect checkpoint
        for (int i = 0; i < regions.checkpoints().size(); i++) {
            BoatRaceTrack.RespawnRegion checkpoint = regions.checkpoints().get(i);

            if (checkpoint.intersect(pos, prevPos)) {
                if (i != nextCheckpointIdx && i != prevCheckpointIdx) {
                    return TickResult.MISSED;
                }
                break;
            }
        }

        return TickResult.IDLE;
    }

    /**
     * Reset checkpoint data for a player.
     *
     * @param player The player to reset.
     */
    public void reset(BoatRacePlayer player) {
        this.inPit.remove(player);
        this.canPit.remove(player);
        this.prevPositions.remove(player);
        this.checkpoints.remove(player);
        this.laps.remove(player);
        this.began.remove(player);
    }

    /**
     * Get the last checkpoint the player used
     *
     * @param player The player to get from.
     * @return The bounds of their relevant checkpoint.
     */
    public BoatRaceTrack.RespawnRegion getCheckpoint(BoatRacePlayer player) {
        BoatRaceTrack.Regions regions = track.getRegions();

        // no checkpoints, return default
        if (regions.checkpoints().size() == 0) {
            return BoatRaceTrack.RespawnRegion.DEFAULT;
        }

        // return the first checkpoint if the player has just spawned
        int checkpointIdx = this.getCheckpointIndex(player);
        if (checkpointIdx == -1) {
            return regions.checkpoints().getFirst();
        }

        return regions.checkpoints().get(checkpointIdx);
    }

    public int getCheckpointIndex(BoatRacePlayer player) {
        return this.checkpoints.getOrDefault(player, -1);
    }

    /**
     * Get the number of laps for a player.
     *
     * @param player The player.
     * @return Their completed laps.
     */
    public int getLaps(BoatRacePlayer player) {
        return this.laps.getOrDefault(player, 0);
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
         * Entered a pit lane.
         */
        PIT_ENTER,

        /**
         * Exited a pit lane.
         */
        PIT_EXIT,

        /**
         * Missed a checkpoint.
         */
        MISSED,

        /**
         * Nothing happened
         */
        IDLE,
    }
}
