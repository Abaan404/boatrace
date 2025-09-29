package com.abaan404.boatrace.boatrace.game.race;

import com.abaan404.boatrace.boatrace.game.BoatRaceConfig;
import com.abaan404.boatrace.boatrace.game.maps.TrackMap;

import xyz.nucleoid.plasmid.api.game.*;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.game.GameActivity;

/**
 * boat goes nyoom here.
 */
public class Race {
    public static void open(GameActivity game, BoatRaceConfig config, ServerWorld world, TrackMap map) {
        throw new GameOpenException(Text.of("Work In Progress"));
    }
}
