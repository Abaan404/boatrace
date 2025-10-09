package com.abaan404.boatrace.game.race;

import java.util.List;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.maps.TrackMap;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameOpenException;

/**
 * boat goes nyoom here.
 */
public class Race {
    public static void open(GameActivity game, BoatRaceConfig config, ServerWorld world, TrackMap track, List<PersonalBest> leaderboard) {
        throw new GameOpenException(Text.of("Work In Progress"));
    }
}
