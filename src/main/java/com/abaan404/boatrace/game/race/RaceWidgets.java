package com.abaan404.boatrace.game.race;

import java.util.Map;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.maps.TrackMap;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

public class RaceWidgets {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final GlobalWidgets widgets;
    private final TrackMap track;

    private final Map<BoatRacePlayer, SidebarWidget> sidebars = new Object2ObjectOpenHashMap<>();

    public RaceWidgets(GameSpace gameSpace, ServerWorld world, GlobalWidgets widgets, TrackMap track) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.track = track;
        this.widgets = widgets;
    }

    /**
     * Tick the UI for the player.
     *
     * @param stageManager The game's state.
     */
    public void tick(RaceStageManager stageManager) {
        this.tickActionBar(stageManager);
        this.tickSidebar(stageManager);
    }

    /**
     * Display splits and timers on the client's action bar.
     *
     * @param stageManager The stage manager.
     */
    private void tickActionBar(RaceStageManager stageManager) {
    }

    /**
     * Displays track meta and track leaderboard.
     */
    private void tickSidebar(RaceStageManager stageManager) {
    }
}
