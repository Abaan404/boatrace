package com.abaan404.boatrace;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

public record BoatRaceConfig(
        Identifier track,
        Team team,
        Optional<Qualifying> qualifying, Optional<Race> race) {

    public static final MapCodec<BoatRaceConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("track").forGetter(BoatRaceConfig::track),
            Team.CODEC.optionalFieldOf("team", Team.DEFAULT).forGetter(BoatRaceConfig::team),
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
            int maxDuration, int maxLaps,
            GridType gridType,
            int countdown, int countdownRandom,
            List<Integer> scoring) {

        private static final List<Integer> DEFAULT_SCORING = List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);

        public static final Codec<Race> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("max_duration").forGetter(Race::maxDuration),
                Codec.INT.fieldOf("max_laps").forGetter(Race::maxLaps),
                GridType.CODEC.optionalFieldOf("grid_type", GridType.NORMAL).forGetter(Race::gridType),
                Codec.INT.optionalFieldOf("countdown", 5000).forGetter(Race::countdown),
                Codec.INT.optionalFieldOf("countdown_random", 2000).forGetter(Race::countdownRandom),
                Codec.INT.listOf().optionalFieldOf("scoring", DEFAULT_SCORING).forGetter(Race::scoring))
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

    public record Team(int size) {
        public static final Team DEFAULT = new Team(1);

        public static final Codec<Team> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("size", DEFAULT.size()).forGetter(Team::size))
                .apply(instance, Team::new));

    }
}
