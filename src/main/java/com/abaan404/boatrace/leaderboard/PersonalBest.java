package com.abaan404.boatrace.leaderboard;

import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.util.Uuids;

/**
 * Stores the player's time and splits and their info.
 */
public record PersonalBest(String offlineName, UUID id, float timer, List<Float> splits) {
    public static PersonalBest of() {
        return new PersonalBest("Herobrine", UUID.randomUUID(), Float.NaN, FloatArrayList.of());
    }

    public static final Codec<PersonalBest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("offlineName").forGetter(PersonalBest::offlineName),
            Uuids.CODEC.fieldOf("uuid").forGetter(PersonalBest::id),
            Codec.FLOAT.fieldOf("timer").forGetter(PersonalBest::timer),
            Codec.FLOAT.listOf().fieldOf("splits").forGetter(PersonalBest::splits))
            .apply(instance, PersonalBest::new));
}
