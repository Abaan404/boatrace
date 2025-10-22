package com.abaan404.boatrace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.abaan404.boatrace.game.qualifying.Qualifying;
import com.abaan404.boatrace.game.race.Race;
import com.abaan404.boatrace.game.timetrial.TimeTrial;
import com.abaan404.boatrace.items.BoatRaceItems;
import com.abaan404.boatrace.leaderboard.Leaderboard;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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

        BoatRaceTrack map = BoatRaceTrack.load(context.server(), config.track()).orElseThrow();
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
                Race.open(game, config, world, map, ObjectArrayList.of());
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
        Leaderboard.initialize();
    }
}
