package com.abaan404.boatrace.game.gameplay;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.abaan404.boatrace.BoatRacePlayer;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Keeps track of positions and delta times on track.
 */
public class PositionsManager {
    private final SplitsManager splits;

    private Map<BoatRacePlayer, Integer> playerToPositions = new Object2IntOpenHashMap<>();
    private List<BoatRacePlayer> positions = new ObjectArrayList<>();
    private Set<BoatRacePlayer> waiting = new ObjectOpenHashSet<>();

    public PositionsManager(SplitsManager splits) {
        this.splits = splits;
    }

    /**
     * Update the current positions according to the current SplitsManager state.
     * The player being updated will also be removed from their waiting state.
     *
     * @param player The player being updated.
     * @param splits The split manager.
     */
    public void update(BoatRacePlayer player) {
        this.waiting.remove(player);

        // divide players into those who had update() called for and those who hadn't.
        List<BoatRacePlayer> positions = new ObjectArrayList<>();
        List<BoatRacePlayer> waiting = new ObjectArrayList<>();

        for (BoatRacePlayer bPlayer : this.positions) {
            if (!this.waiting.contains(bPlayer)) {
                positions.add(bPlayer);
            } else {
                waiting.add(bPlayer);
            }
        }

        // sort them by checkpoints completed or their delta
        positions.sort((a, b) -> {
            List<Long> aSplits = this.splits.getSplits(a);
            List<Long> bSplits = this.splits.getSplits(b);

            if (aSplits.size() != bSplits.size()) {
                return Integer.compare(bSplits.size(), aSplits.size());
            }

            long aTime = aSplits.isEmpty() ? Long.MAX_VALUE : aSplits.getLast();
            long bTime = bSplits.isEmpty() ? Long.MAX_VALUE : bSplits.getLast();

            return Long.compare(aTime, bTime);
        });

        // append waiting players to the back
        for (BoatRacePlayer bPlayer : waiting) {
            positions.add(bPlayer);
        }

        this.positions = positions;

        // update cache
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
        this.waiting.add(player);

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
        this.waiting.remove(player);

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
        long split2 = splits2.get(splitIdx) - splits2.get(splitIdxPrev);

        return split1 - split2;
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
