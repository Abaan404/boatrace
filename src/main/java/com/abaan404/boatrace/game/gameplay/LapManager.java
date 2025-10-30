package com.abaan404.boatrace.game.gameplay;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Keeps track of positions and delta times on track.
 */
public class LapManager {
    private final BoatRaceTrack track;

    private Map<BoatRacePlayer, Integer> playerToPositions = new Object2IntOpenHashMap<>();
    private List<BoatRacePlayer> positions = new ObjectArrayList<>();
    private Map<BoatRacePlayer, List<Long>> splits = new Object2ObjectOpenHashMap<>();

    public LapManager(BoatRaceTrack track) {
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
                    List<Long> aSplits = this.splits.get(a);
                    List<Long> bSplits = this.splits.get(b);

                    if (aSplits.size() == bSplits.size() && !aSplits.isEmpty()) {
                        return Long.compare(bSplits.getLast(), aSplits.getLast());
                    } else {
                        return Integer.compare(bSplits.size(), aSplits.size());
                    }
                })
                .toList();

        for (int i = 0; i < this.positions.size(); i++) {
            this.playerToPositions.put(this.positions.get(i), i);
        }
    }

    /**
     * Get the total delta against another player.
     *
     * @param player1 The player to compare for.
     * @param player2 The player compared against.
     * @return The total delta at a common checkpoint.
     */
    public long getDelta(BoatRacePlayer player1, BoatRacePlayer player2) {
        List<Long> splits1 = this.splits.getOrDefault(player1, LongArrayList.of(0l));
        List<Long> splits2 = this.splits.getOrDefault(player2, LongArrayList.of(0l));

        int splitIdx = Math.max(0, Math.min(splits1.size() - 1, splits2.size() - 1));

        long split1 = splits1.get(splitIdx);
        long split2 = splits2.get(splitIdx);

        return split1 - split2;
    }

    /**
     * Get the delta against another player at a checkpoint.
     *
     * @param player1 The player to compare for.
     * @param player2 The player compared against.
     * @return The delta at a common checkpoint.
     */
    public long getDeltaCheckpoint(BoatRacePlayer player1, BoatRacePlayer player2) {
        List<Long> splits1 = this.splits.getOrDefault(player1, LongArrayList.of(0l));
        List<Long> splits2 = this.splits.getOrDefault(player2, LongArrayList.of(0l));

        int splitIdx = Math.max(0, Math.min(splits1.size() - 1, splits2.size() - 1));
        int splitIdxPrev = Math.max(0, splitIdx - 1);

        long split1 = splits1.get(splitIdx) - splits1.get(splitIdxPrev);
        long split2 = splits2.get(splitIdx) - splits1.get(splitIdxPrev);

        return split1 - split2;
    }

    /**
     * Get the number of laps by the leader.
     *
     * @return The leader's laps.
     */
    public int getLeadingLaps() {
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
    public int getLaps(BoatRacePlayer player) {
        int curCheckpoints = this.splits.getOrDefault(player, LongArrayList.of()).size() - 1;
        int trackCheckpoints = this.track.getRegions().checkpoints().size();

        // player hasnt reached first checkpoint.
        if (curCheckpoints < 0) {
            return 0;
        }

        return switch (this.track.getMeta().layout()) {
            case CIRCULAR -> Math.floorDiv(curCheckpoints, trackCheckpoints);
            case LINEAR -> curCheckpoints >= trackCheckpoints ? 1 : 0;
        };
    }

    /**
     * Get the lap position for this player.
     *
     * @param player The player.
     * @return Their position.
     */
    public int getPosition(BoatRacePlayer player) {
        return this.playerToPositions.getOrDefault(player, -1);
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
