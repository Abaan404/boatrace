package com.abaan404.boatrace;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;

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
            Optional<Integer> laps) {

        public static final Codec<Qualifying> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("duration").forGetter(Qualifying::duration),
                Codec.INT.optionalFieldOf("laps").forGetter(Qualifying::laps))
                .apply(instance, Qualifying::new));
    }

    public record Race(
            long maxDuration, int maxLaps, boolean noRespawn, boolean acceptUnqualified,
            Pits pits, GridType gridType, Countdown goCountdown, List<Integer> scoring) {

        private static final List<Integer> DEFAULT_SCORING = List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);

        public static final Codec<Race> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("max_duration").forGetter(Race::maxDuration),
                Codec.INT.optionalFieldOf("max_laps", 1).forGetter(Race::maxLaps),
                Codec.BOOL.optionalFieldOf("no_respawn", false).forGetter(Race::noRespawn),
                Codec.BOOL.optionalFieldOf("accept_unqualified", false).forGetter(Race::acceptUnqualified),
                Pits.CODEC.optionalFieldOf("pits", Pits.DEFAULT).forGetter(Race::pits),
                GridType.CODEC.optionalFieldOf("grid_type", GridType.NORMAL).forGetter(Race::gridType),
                Countdown.CODEC.optionalFieldOf("go_countdown", new Countdown(5000, 2000)).forGetter(Race::goCountdown),
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

    public record Team(int size, List<Fixed> fixed) {
        public static final Team DEFAULT = new Team(1, List.of());

        public static final Codec<Team> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("size", DEFAULT.size()).forGetter(Team::size),
                Fixed.CODEC.listOf().optionalFieldOf("fixed", DEFAULT.fixed()).forGetter(Team::fixed))
                .apply(instance, Team::new));

        public record Fixed(GameTeam team, List<BoatRacePlayer> players) {
            public static final Codec<Fixed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    GameTeam.CODEC.fieldOf("team").forGetter(Fixed::team),
                    BoatRacePlayer.CODEC.listOf().fieldOf("players").forGetter(Fixed::players))
                    .apply(instance, Fixed::new));
        }
    }

    public record Countdown(long duration, long random) {
        public static final Countdown DEFAULT = new Countdown(1000, 0);

        public static final Codec<Countdown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.optionalFieldOf("duration", DEFAULT.duration()).forGetter(Countdown::duration),
                Codec.LONG.optionalFieldOf("random", DEFAULT.random()).forGetter(Countdown::random))
                .apply(instance, Countdown::new));
    }

    public record Pits(Countdown ready, Countdown failure, int count) {
        public static final Pits DEFAULT = new Pits(new Countdown(2000, 3000), new Countdown(2000, 0), 0);

        public static final Codec<Pits> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Countdown.CODEC.optionalFieldOf("success", DEFAULT.ready()).forGetter(Pits::ready),
                Countdown.CODEC.optionalFieldOf("failure", DEFAULT.failure()).forGetter(Pits::failure),
                Codec.INT.optionalFieldOf("count", DEFAULT.count()).forGetter(Pits::count))
                .apply(instance, Pits::new));
    }
}
