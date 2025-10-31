package com.abaan404.boatrace.game.gameplay;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Keeps track of positions and delta times on track.
 */
public class LapManager {
    private final BoatRaceTrack track;
    private final SplitsManager splits;

    private Map<BoatRacePlayer, Integer> playerToPositions = new Object2IntOpenHashMap<>();
    private List<BoatRacePlayer> positions = new ObjectArrayList<>();

    public LapManager(BoatRaceTrack track, SplitsManager splits) {
        this.track = track;
        this.splits = splits;
    }

    /**
     * Update the current positions according to the current SplitsManager state.
     */
    public void update() {
        this.positions.sort((a, b) -> {
            List<Long> aSplits = this.splits.getSplits(a);
            List<Long> bSplits = this.splits.getSplits(b);

            if (aSplits.size() == bSplits.size() && !aSplits.isEmpty()) {
                return Long.compare(bSplits.getLast(), aSplits.getLast());
            } else {
                return Integer.compare(bSplits.size(), aSplits.size());
            }
        });

        this.playerToPositions.clear();
        for (int i = 0; i < this.positions.size(); i++) {
            this.playerToPositions.put(this.positions.get(i), i);
        }
    }

    /**
     * Add a player to being tracked.
     *
     * @param player The player to track.
     */
    public void add(BoatRacePlayer player) {
        this.positions.add(player);

        this.playerToPositions.clear();
        for (int i = 0; i < this.positions.size(); i++) {
            this.playerToPositions.put(this.positions.get(i), i);
        }
    }

    /**
     * Remove a player from being tracked.
     *
     * @param player The player to erase.
     */
    public void remove(BoatRacePlayer player) {
        this.positions.removeIf(a -> a.equals(player));

        this.playerToPositions.clear();
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
        List<Long> splits1 = this.splits.getSplits(player1);
        List<Long> splits2 = this.splits.getSplits(player2);

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
        List<Long> splits1 = this.splits.getSplits(player1);
        List<Long> splits2 = this.splits.getSplits(player2);

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
        int curCheckpoints = this.splits.getSplits(player).size() - 1;
        int trackCheckpoints = this.track.getRegions().checkpoints().size() - 1;

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
