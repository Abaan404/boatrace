package com.abaan404.boatrace;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

public record BoatRaceConfig(Identifier track, Optional<Qualifying> qualifying,
        Optional<Race> race) {

    public static final MapCodec<BoatRaceConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("track").forGetter(BoatRaceConfig::track),
            Qualifying.CODEC.optionalFieldOf("qualifying").forGetter(BoatRaceConfig::qualifying),
            Race.CODEC.optionalFieldOf("race").forGetter(BoatRaceConfig::race))
            .apply(instance, BoatRaceConfig::new));

    public record Qualifying(
            long duration,
            StartFrom startFrom) {

        public static final Codec<Qualifying> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("duration").forGetter(Qualifying::duration),
                StartFrom.CODEC.optionalFieldOf("start_from", StartFrom.SPAWN).forGetter(Qualifying::startFrom))
                .apply(instance, Qualifying::new));

        public enum StartFrom implements StringIdentifiable {
            PIT_BOX("pit_box"), GRID_BOX("grid_box"), SPAWN("spawn");

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
            int maxDuration, int maxLaps, int requiredPits,
            GridType gridType,
            int countdown, int countdownRandom,
            boolean collision) {

        public static final Codec<Race> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("max_duration").forGetter(Race::maxDuration),
                Codec.INT.fieldOf("max_laps").forGetter(Race::maxLaps),
                Codec.INT.optionalFieldOf("required_pits", 0).forGetter(Race::requiredPits),
                GridType.CODEC.optionalFieldOf("grid_type", GridType.NORMAL).forGetter(Race::gridType),
                Codec.INT.optionalFieldOf("countdown", 5000).forGetter(Race::countdown),
                Codec.INT.optionalFieldOf("countdown_random", 2000).forGetter(Race::countdownRandom),
                Codec.BOOL.optionalFieldOf("collision", false).forGetter(Race::collision))
                .apply(instance, Race::new));

        public enum GridType implements StringIdentifiable {
            NORMAL("normal"), REVERSED("reversed"), RANDOM("random");

            private final String name;

            public static final Codec<GridType> CODEC = StringIdentifiable.createCodec(GridType::values);

            GridType(String name) {
                this.name = name;
            }

            @Override
            public String asString() {
                return name;
            }
        }
    }
}
