package com.abaan404.boatrace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.abaan404.boatrace.game.qualifying.Qualifying;
import com.abaan404.boatrace.game.race.Race;
import com.abaan404.boatrace.game.timetrial.TimeTrial;
import com.abaan404.boatrace.gameplay.Teams;
import com.abaan404.boatrace.leaderboard.Leaderboard;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;

public class BoatRace implements ModInitializer {
    public static final String ID = "boatrace";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<BoatRaceConfig> TYPE = GameTypes.register(
            Identifier.fromNamespaceAndPath(ID, "game"),
            BoatRaceConfig.CODEC,
            BoatRace::open);

    public static GameOpenProcedure open(GameOpenContext<BoatRaceConfig> context) {
        BoatRaceConfig config = context.config();

        BoatRaceTrack track = BoatRaceTrack.load(context.server(), config.track());
        RuntimeLevelConfig levelConfig = new RuntimeLevelConfig()
                .setGenerator(track.asGenerator(context.server()));

        if (config.qualifying().isPresent()) {
            if (!config.race().isPresent()) {
                throw new GameOpenException(Component.literal("A race config is required to begin qualifying for."));
            }

            return context.openWithLevel(levelConfig, (game, level) -> {
                Teams teams = new Teams(config.team(), TeamManager.addTo(game));
                BoatRaceConfig.Qualifying qualifying = config.qualifying().orElseThrow();
                BoatRaceConfig.Race race = config.race().orElseThrow();

                Qualifying.open(game, qualifying, race, level, track, teams);
            });
        }

        if (config.race().isPresent()) {
            return context.openWithLevel(levelConfig, (game, level) -> {
                Teams teams = new Teams(config.team(), TeamManager.addTo(game));
                BoatRaceConfig.Race race = config.race().orElseThrow();

                Race.open(game, race, level, track, teams, ObjectArrayList.of());
            });
        }

        return context.openWithLevel(levelConfig, (game, level) -> {
            TimeTrial.open(game, level, track);
        });
    }

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets(ID);
        BoatRaceItems.initialize();
        BoatRaceCommands.initialize();
        Leaderboard.initialize();
    }
}
