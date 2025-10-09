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
    public static PersonalBest of() {
        return new PersonalBest(BoatRacePlayer.of(), Long.MAX_VALUE, LongArrayList.of());
    }

    public static final Codec<PersonalBest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BoatRacePlayer.CODEC.fieldOf("player").forGetter(PersonalBest::player),
            Codec.LONG.fieldOf("timer").forGetter(PersonalBest::timer),
            Codec.LONG.listOf().fieldOf("splits").forGetter(PersonalBest::splits))
            .apply(instance, PersonalBest::new));
}
