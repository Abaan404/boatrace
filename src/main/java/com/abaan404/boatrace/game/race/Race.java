package com.abaan404.boatrace.game.race;

import com.abaan404.boatrace.game.BoatRaceConfig;
import com.abaan404.boatrace.maps.TrackMap;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameOpenException;

/**
 * boat goes nyoom here.
 */
public class Race {
    public static void open(GameActivity game, BoatRaceConfig config, ServerWorld world, TrackMap map) {
        throw new GameOpenException(Text.of("Work In Progress"));
    }
}
