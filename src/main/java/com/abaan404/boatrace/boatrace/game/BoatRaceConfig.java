package com.abaan404.boatrace.boatrace.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.abaan404.boatrace.boatrace.game.map.BoatRaceMapConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public record BoatRaceConfig(WaitingLobbyConfig players, BoatRaceMapConfig mapConfig, int timeLimitSecs) {
    public static final MapCodec<BoatRaceConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(BoatRaceConfig::players),
            BoatRaceMapConfig.CODEC.fieldOf("map").forGetter(BoatRaceConfig::mapConfig),
            Codec.INT.fieldOf("time_limit_secs").forGetter(BoatRaceConfig::timeLimitSecs)
    ).apply(instance, BoatRaceConfig::new));
}
