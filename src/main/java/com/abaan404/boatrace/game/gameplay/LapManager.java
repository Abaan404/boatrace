package com.abaan404.boatrace.game.gameplay;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.maps.TrackMap;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Keeps track of positions and delta times on track.
 */
public class LapManager {
    private final TrackMap track;

    private List<BoatRacePlayer> positions = new ObjectArrayList<>();
    private Map<BoatRacePlayer, List<Long>> splits = new Object2ObjectOpenHashMap<>();

    public LapManager(TrackMap track) {
        this.track = track;
    }

    /**
     * Submit a checkpoint split, will update lap positions accordingly.
     *
     * @param player The player's split to submit, will create an entry if it does
     *               not exist.
     * @param splits The checkpoint splits.
     */
    public void submit(BoatRacePlayer player, List<Long> splits) {
        this.splits.put(player, splits);
        this.positions = this.splits.keySet()
                .stream()
                .sorted((a, b) -> {
                    List<Long> aLaps = this.splits.get(a);
                    List<Long> bLaps = this.splits.get(b);

                    if (aLaps.size() == bLaps.size() && !aLaps.isEmpty()) {
                        return Long.compare(bLaps.getLast(), aLaps.getLast());
                    } else {
                        return Integer.compare(bLaps.size(), aLaps.size());
                    }
                })
                .toList();
    }

    /**
     * Get the lap position for this player.
     *
     * @param player The player.
     * @return Their position.
     */
    public int getPosition(BoatRacePlayer player) {
        for (int i = 0; i < this.positions.size(); i++) {
            if (this.positions.get(i).equals(player)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Get the delta for this player against every other player according to their
     * position. +ve is ahead while -ve is behind.
     *
     * @param player The player to be considered.
     * @return A map of every participating player and their relative delta to the
     *         considered player.
     */
    public Map<BoatRacePlayer, Long> getDeltas(BoatRacePlayer player) {
        Map<BoatRacePlayer, Long> deltas = new Object2LongOpenHashMap<>();

        List<Long> curSplits = this.splits.getOrDefault(player, LongArrayList.of(0l));
        int cur = curSplits.size() - 1;

        for (BoatRacePlayer otherPlayer : this.positions) {
            List<Long> othSplits = this.splits.getOrDefault(otherPlayer, LongArrayList.of(0l));
            int oth = othSplits.size() - 1;

            long delta;

            // equal or ahead checkpoint
            if (oth >= cur) {
                delta = curSplits.get(cur) + othSplits.get(oth) - 2 * othSplits.get(cur);
            }

            // behind
            else {
                delta = -1 * (othSplits.get(oth) + curSplits.get(cur) - 2 * curSplits.get(oth));
            }

            deltas.put(otherPlayer, delta);
        }

        return deltas;
    }

    /**
     * Get the number of laps by the leader.
     *
     * @return The leader's laps.
     */
    public long getLeadingLaps() {
        if (this.positions.isEmpty()) {
            return 0;
        }

        return this.getLaps(this.positions.getFirst());
    }

    /**
     * Get the number of laps for a player.
     *
     * @param player The player.
     * @return Their completed laps.
     */
    public long getLaps(BoatRacePlayer player) {
        int curCheckpoints = this.splits.getOrDefault(player, LongArrayList.of()).size() - 2;
        int trackCheckpoints = this.track.getRegions().checkpoints().size();

        // -2 player hasnt started, -1 player hasnt reached first checkpoint.
        if (curCheckpoints < 0) {
            return 0;
        }

        return switch (this.track.getMeta().layout()) {
            case CIRCULAR -> Math.floorDiv(curCheckpoints, trackCheckpoints);
            case LINEAR -> curCheckpoints >= trackCheckpoints ? 1 : 0;
        };
    }

    /**
     * Get a ordered list of track positions.
     *
     * @return A list of players sorted to their lap positions.
     */
    public List<BoatRacePlayer> getPositions() {
        return Collections.unmodifiableList(this.positions);
    }
}
