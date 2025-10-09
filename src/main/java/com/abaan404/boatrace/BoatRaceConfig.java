package com.abaan404.boatrace;

import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

public record BoatRaceConfig(Map map, Optional<Qualifying> qualifying,
        Optional<Race> race) {

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
            float duration,
            StartFrom startFrom) {

        public static final Codec<Qualifying> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("duration").forGetter(Qualifying::duration),
                StartFrom.CODEC.optionalFieldOf("start_from", StartFrom.PIT_BOX).forGetter(Qualifying::startFrom))
                .apply(instance, Qualifying::new));

        public enum StartFrom implements StringIdentifiable {
            PIT_BOX("pit_box"), GRID_BOX("grid_box");

            private final String name;

            public static final Codec<StartFrom> CODEC = StringIdentifiable.createCodec(StartFrom::values);

            StartFrom(String name) {
                this.name = name;
            }

            @Override
            public String asString() {
                return name;
            }
        }
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
