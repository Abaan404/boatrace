package com.abaan404.boatrace.boatrace;

import net.fabricmc.api.ModInitializer;
import xyz.nucleoid.plasmid.api.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.abaan404.boatrace.boatrace.game.BoatRaceConfig;
import com.abaan404.boatrace.boatrace.game.BoatRaceWaiting;

public class BoatRace implements ModInitializer {

    public static final String ID = "boatrace";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<BoatRaceConfig> TYPE = GameType.register(
            Identifier.of(ID, "boatrace"),
            BoatRaceConfig.CODEC,
            BoatRaceWaiting::open
    );

    @Override
    public void onInitialize() {}
}
