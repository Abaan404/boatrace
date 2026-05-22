package com.abaan404.boatrace.gameplay;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.BoatRaceTrack.RespawnRegion;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Keeps track of checkpoints and verifies if the player crossed the correct
 * checkpoints in order for a track.
 */
public class Checkpoints {
    private final BoatRaceTrack track;

    private Map<BoatRacePlayer, Vec3> prevPositions = new Object2ObjectOpenHashMap<>();
    private Map<BoatRacePlayer, Tuple<Integer, RespawnRegion>> checkpoints = new Object2ObjectOpenHashMap<>();
    private Map<BoatRacePlayer, Integer> laps = new Object2IntOpenHashMap<>();
    private Set<BoatRacePlayer> inRestart = new ObjectOpenHashSet<>();
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
    public TickResult tick(ServerPlayer player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        BoatRaceTrack.Regions regions = track.getRegions();
        BoatRaceTrack.Attributes attributes = track.getAttributes();

        Vec3 pos = player.getEyePosition();
        Vec3 prevPos = this.prevPositions.getOrDefault(bPlayer, pos);
        this.prevPositions.put(bPlayer, pos);

        // no checkpoints, do nothing
        if (regions.checkpoints().isEmpty()) {
            return TickResult.IDLE;
        }

        int prevCheckpointIdx = this.getCheckpointIndex(bPlayer);
        int nextCheckpointIdx = (prevCheckpointIdx + 1) % regions.checkpoints().size();

        Optional<RespawnRegion> start = this.intersectAny(regions.checkpoints().getFirst(), pos, prevPos);

        // player has reached the starting line
        if (!this.began.contains(bPlayer) && start.isPresent()) {
            if (!this.inRestart.contains(bPlayer)) {
                this.laps.put(bPlayer, this.laps.getOrDefault(bPlayer, 0) + 1);
            }

            this.began.add(bPlayer);
            this.checkpoints.put(bPlayer, new Tuple<>(nextCheckpointIdx, start.orElseThrow()));
            this.canPit.add(bPlayer);
            return TickResult.BEGIN;
        }

        // run hasnt began yet, do nothing
        if (!this.began.contains(bPlayer)) {
            return TickResult.IDLE;
        }

        Optional<RespawnRegion> next = this.intersectAny(regions.checkpoints().get(nextCheckpointIdx), pos, prevPos);

        // reached the next checkpoint
        if (next.isPresent()) {
            this.checkpoints.put(bPlayer, new Tuple<>(nextCheckpointIdx, next.orElseThrow()));

            switch (attributes.layout()) {
                case CIRCULAR: {
                    // checkpoint was looped back to the start
                    if (nextCheckpointIdx == 0) {
                        this.laps.put(bPlayer, this.laps.getOrDefault(bPlayer, 0) + 1);
                        this.canPit.add(bPlayer);
                        this.inRestart.remove(bPlayer);
                        return TickResult.LOOP;
                    }

                    break;
                }

                case LINEAR: {
                    // checkpoint reached the end
                    if (nextCheckpointIdx == regions.checkpoints().size() - 1) {
                        this.checkpoints.remove(bPlayer);
                        this.began.remove(bPlayer);
                        this.inRestart.remove(bPlayer);
                        return TickResult.FINISH;
                    }

                    break;
                }
            }

            return TickResult.CHECKPOINT;
        }

        if (this.canPit.contains(bPlayer) && regions.pitLane().isPresent()) {
            RespawnRegion pitLane = regions.pitLane().orElseThrow();

            if (!this.inPit.contains(bPlayer) && pitLane.intersect(pos, prevPos)) {
                this.inPit.add(bPlayer);

                return TickResult.PIT_ENTER;
            }

            if (this.inPit.contains(bPlayer) && !pitLane.intersect(pos, prevPos)) {
                this.inPit.remove(bPlayer);
                this.canPit.remove(bPlayer);

                return TickResult.PIT_EXIT;
            }
        }

        // test if player went to an incorrect checkpoint
        for (int i = 0; i < regions.checkpoints().size(); i++) {
            Optional<RespawnRegion> checkpoint = this.intersectAny(regions.checkpoints().get(i), pos, prevPos);

            if (checkpoint.isPresent()) {
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
     * Restart a lap for a player.
     *
     * @param player The player to restart.
     */
    public void restart(BoatRacePlayer player) {
        this.inPit.remove(player);
        this.canPit.remove(player);
        this.prevPositions.remove(player);
        this.checkpoints.remove(player);
        this.inRestart.add(player);
        this.began.remove(player);
    }

    /**
     * Get the last checkpoint the player used
     *
     * @param player The player to get from.
     * @return The checkpoint region, empty if none found.
     */
    public Optional<RespawnRegion> getCheckpoint(BoatRacePlayer player) {
        return Optional.ofNullable(this.checkpoints.get(player))
                .map(p -> p.getB());
    }

    /**
     * Get the last checkpoint's index the player used
     *
     * @param player The player to get from.
     * @return The checkpoint region, empty if none found.
     */
    public int getCheckpointIndex(BoatRacePlayer player) {
        return Optional.ofNullable(this.checkpoints.get(player))
                .map(p -> p.getA())
                .orElse(-1);
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

    /**
     * Check if an entity's intersected any region bounds.
     *
     * @param regions The regions to test.
     * @param pos     The entity's current pos.
     * @param lastPos The entity's pos last tick.
     * @return The region it intersected.
     */
    private Optional<RespawnRegion> intersectAny(Set<RespawnRegion> regions, Vec3 pos, Vec3 lastPos) {
        for (RespawnRegion region : regions) {
            if (region.intersect(pos, lastPos)) {
                return Optional.of(region);
            }
        }

        return Optional.empty();
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
