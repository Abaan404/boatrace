package com.abaan404.boatrace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.abaan404.boatrace.compat.openboatutils.OBU;
import com.abaan404.boatrace.game.qualifying.Qualifying;
import com.abaan404.boatrace.game.race.Race;
import com.abaan404.boatrace.game.timetrial.TimeTrial;
import com.abaan404.boatrace.gameplay.Teams;
import com.abaan404.boatrace.leaderboard.Leaderboard;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.ModInitializer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;

public class BoatRace implements ModInitializer {
    public static final String ID = "boatrace";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<BoatRaceConfig> TYPE = GameType.register(
            Identifier.of(ID, "game"),
            BoatRaceConfig.CODEC,
            BoatRace::open);

    public static GameOpenProcedure open(GameOpenContext<BoatRaceConfig> context) {
        BoatRaceConfig config = context.config();

        BoatRaceTrack track = BoatRaceTrack.load(context.server(), config.track());
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(track.asGenerator(context.server()));

        if (config.qualifying().isPresent()) {
            if (!config.race().isPresent()) {
                throw new GameOpenException(Text.of("A race config is required to begin qualifying for."));
            }

            return context.openWithWorld(worldConfig, (game, world) -> {
                Teams teams = new Teams(config.team(), TeamManager.addTo(game));
                BoatRaceConfig.Qualifying qualifying = config.qualifying().orElseThrow();
                BoatRaceConfig.Race race = config.race().orElseThrow();

                Qualifying.open(game, qualifying, race, world, track, teams);
            });
        }

        if (config.race().isPresent()) {
            return context.openWithWorld(worldConfig, (game, world) -> {
                Teams teams = new Teams(config.team(), TeamManager.addTo(game));
                BoatRaceConfig.Race race = config.race().orElseThrow();

                Race.open(game, race, world, track, teams, ObjectArrayList.of());
            });
        }

        return context.openWithWorld(worldConfig, (game, world) -> {
            TimeTrial.open(game, world, track);
        });
    }

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets(ID);
        OBU.initialize();
        BoatRaceItems.initialize();
        BoatRaceCommands.initialize();
        Leaderboard.initialize();
    }
}
