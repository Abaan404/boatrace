package com.abaan404.boatrace.boatrace;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.abaan404.boatrace.boatrace.game.BoatRaceConfig;
import com.abaan404.boatrace.boatrace.game.items.BoatRaceItems;
import com.abaan404.boatrace.boatrace.game.lobby.Lobby;
import com.abaan404.boatrace.boatrace.game.maps.LobbyMap;
import com.abaan404.boatrace.boatrace.game.maps.TrackMap;
import com.abaan404.boatrace.boatrace.game.qualifying.Qualifying;
import com.abaan404.boatrace.boatrace.game.race.Race;
import com.abaan404.boatrace.boatrace.game.timetrial.TimeTrial;
import com.abaan404.boatrace.boatrace.game.timetrial.TimeTrialLeaderboard;

import net.fabricmc.api.ModInitializer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameType;

public class BoatRace implements ModInitializer {
    public static final String ID = "boatrace";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<BoatRaceConfig> TYPE = GameType.register(
            Identifier.of(ID, "game"),
            BoatRaceConfig.CODEC,
            BoatRace::open);

    public static GameOpenProcedure open(GameOpenContext<BoatRaceConfig> context) {
        BoatRaceConfig config = context.config();

        Optional<Identifier> lobbyMap = config.map().lobbyOrTrack().left();
        Optional<Identifier> trackMap = config.map().lobbyOrTrack().right();

        // open the lobby map if present
        if (lobbyMap.isPresent()) {
            List<TrackMap> tracks = TrackMap
                    .loadAll(context.server());

            LobbyMap map = LobbyMap.load(context.server(), lobbyMap.get()).orElseThrow();
            RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                    .setGenerator(map.asGenerator(context.server()));

            return context.openWithWorld(worldConfig, (game, world) -> {
                Lobby.open(game, config, world, map, tracks);
            });
        }

        // otherwise move directly to the track
        TrackMap map = TrackMap.load(context.server(), trackMap.get()).orElseThrow();
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asGenerator(context.server()));

        // qualifying must also come with a race config
        if (config.qualifying().isPresent() && config.race().isPresent()) {
            return context.openWithWorld(worldConfig, (game, world) -> {
                Qualifying.open(game, config, world, map);
            });
        }

        // random order race
        if (config.race().isPresent()) {
            return context.openWithWorld(worldConfig, (game, world) -> {
                Race.open(game, config, world, map);
            });
        }

        // time trial
        if (config.race().isEmpty() && config.race().isEmpty()) {
            return context.openWithWorld(worldConfig, (game, world) -> {
                TimeTrial.open(game, config, world, map);
            });
        }

        throw new GameOpenException(Text.of("invalid config detected, someone was bald."));
    }

    @Override
    public void onInitialize() {
        BoatRaceItems.initialize();
        TimeTrialLeaderboard.initialize();
    }
}
