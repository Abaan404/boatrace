package com.abaan404.boatrace.leaderboard;

import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.util.Uuids;

/**
 * Stores the player's time and splits and their info.
 */
public record PersonalBest(String offlineName, UUID id, long timer, List<Long> splits) {
    public static PersonalBest of() {
        return new PersonalBest("Herobrine", UUID.randomUUID(), Long.MAX_VALUE, LongArrayList.of());
    }

    public static final Codec<PersonalBest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("offlineName").forGetter(PersonalBest::offlineName),
            Uuids.CODEC.fieldOf("uuid").forGetter(PersonalBest::id),
            Codec.LONG.fieldOf("timer").forGetter(PersonalBest::timer),
            Codec.LONG.listOf().fieldOf("splits").forGetter(PersonalBest::splits))
            .apply(instance, PersonalBest::new));
}
