package com.abaan404.boatrace.leaderboard;

import java.util.List;

import com.abaan404.boatrace.BoatRacePlayer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.LongArrayList;

/**
 * Stores the player's time and splits and their info.
 */
public record PersonalBest(BoatRacePlayer player, long timer, List<Long> splits) {

    public static final Codec<PersonalBest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BoatRacePlayer.CODEC.fieldOf("player").forGetter(PersonalBest::player),
            Codec.LONG.fieldOf("timer").forGetter(PersonalBest::timer),
            Codec.LONG.listOf().fieldOf("splits").forGetter(PersonalBest::splits))
            .apply(instance, PersonalBest::new));

    public static PersonalBest of() {
        return new PersonalBest(BoatRacePlayer.of(), Long.MAX_VALUE, LongArrayList.of());
    }

    public boolean exists() {
        return this.timer != Long.MAX_VALUE && this.splits().size() > 0;
    }

    /**
     * Compare the split at an index. The length of splits in both personal bests
     * must match and the checkpoint index must be valid for this pb.
     *
     * @param currentSplits The other personal best to compare against.
     * @param checkpointIdx The checkpoint to compare.
     * @return The delta between the split compared against the other.
     */
    public long getCheckpointDelta(List<Long> currentSplits, int checkpointIdx) {
        if (checkpointIdx < 0 || checkpointIdx > this.splits().size() || checkpointIdx > currentSplits.size()) {
            throw new IndexOutOfBoundsException("Invalid checkpoint index for personal bests to compare against.");
        }

        return currentSplits.get(checkpointIdx) - this.splits().get(checkpointIdx);
    }

    /**
     * Compare the split at an index. The length of splits in both personal bests
     * must match and the checkpoint index must be valid for this pb.
     *
     * @param other         The other personal best to compare against.
     * @param checkpointIdx The checkpoint to compare.
     * @return The delta between the split compared against the other.
     */
    public long getCheckpointDelta(PersonalBest other, int checkpointIdx) {
        if (other.splits().size() != this.splits().size()) {
            throw new IllegalArgumentException("Personal bests do not have equal splits.");
        }

        return this.getCheckpointDelta(other.splits(), checkpointIdx);
    }
}
