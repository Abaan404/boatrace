package com.abaan404.boatrace.game;

import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;

public record BoatRaceConfig(BoatRaceConfig.Map map, Optional<BoatRaceConfig.Qualifying> qualifying,
        Optional<BoatRaceConfig.Race> race) {

    public static final MapCodec<BoatRaceConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Map.CODEC.fieldOf("map").forGetter(BoatRaceConfig::map),
            Qualifying.CODEC.optionalFieldOf("qualifying").forGetter(BoatRaceConfig::qualifying),
            Race.CODEC.optionalFieldOf("race").forGetter(BoatRaceConfig::race))
            .apply(instance, BoatRaceConfig::new));

    public record Map(Either<Identifier, Identifier> lobbyOrTrack) {
        public static final Codec<Map> CODEC = Codec.either(
                Identifier.CODEC.fieldOf("lobby").codec(),
                Identifier.CODEC.fieldOf("track").codec())
                .xmap(Map::new, Map::lobbyOrTrack);
    }

    public record Qualifying(
            float duration) {

        public static final Codec<Qualifying> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("duration").forGetter(Qualifying::duration))
                .apply(instance, Qualifying::new));
    }

    public record Race(
            int maxDuration, int laps,
            int requiredPits,
            boolean collision) {

        public static final Codec<Race> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("max_duration").forGetter(Race::maxDuration),
                Codec.INT.fieldOf("laps").forGetter(Race::laps),
                Codec.INT.fieldOf("required_pits").forGetter(Race::requiredPits),
                Codec.BOOL.fieldOf("collision").forGetter(Race::collision))
                .apply(instance, Race::new));
    }
}
