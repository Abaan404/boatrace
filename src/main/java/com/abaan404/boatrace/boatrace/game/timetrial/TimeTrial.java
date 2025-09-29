package com.abaan404.boatrace.boatrace.game.timetrial;

import com.abaan404.boatrace.boatrace.game.BoatRaceConfig;
import com.abaan404.boatrace.boatrace.game.maps.TrackMap;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

/**
 * Singleplayer game to practice a map and set a best time against a leaderboard.
 */
public class TimeTrial {
    private final GameSpace gameSpace;
    private final ServerWorld world;

    private final TimeTrialStageManager stageManager;
    private final TimeTrialLeaderboard leaderboard;
    private final TimeTrialWidgets widgets;

    private TimeTrial(GameSpace gameSpace, ServerWorld world, TrackMap map, GlobalWidgets widgets, BoatRaceConfig config) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.stageManager = new TimeTrialStageManager(world, map);
        this.widgets = new TimeTrialWidgets(widgets, map);
        this.leaderboard = new TimeTrialLeaderboard(map);
    }

    public static void open(GameActivity game, BoatRaceConfig config, ServerWorld world, TrackMap map) {
        GlobalWidgets widgets = GlobalWidgets.addTo(game);
        TimeTrial timeTrial = new TimeTrial(game.getGameSpace(), world, map, widgets, config);

        game.setRule(GameRuleType.PORTALS, EventResult.DENY);

        game.setRule(GameRuleType.PVP, EventResult.DENY);
        game.setRule(GameRuleType.HUNGER, EventResult.DENY);
        game.setRule(GameRuleType.FALL_DAMAGE, EventResult.DENY);
        game.setRule(GameRuleType.MODIFY_INVENTORY, EventResult.DENY);
        game.setRule(GameRuleType.CRAFTING, EventResult.DENY);
        game.setRule(GameRuleType.PLACE_BLOCKS, EventResult.DENY);
        game.setRule(GameRuleType.BREAK_BLOCKS, EventResult.DENY);
        game.setRule(GameRuleType.DISMOUNT_VEHICLE, EventResult.DENY);

        game.listen(PlayerDamageEvent.EVENT, (player, source, amount) -> EventResult.DENY);
        game.listen(PlayerDeathEvent.EVENT, timeTrial::onPlayerDeath);

        game.listen(GamePlayerEvents.ADD, timeTrial::addPlayer);
        game.listen(GamePlayerEvents.REMOVE, timeTrial::removePlayer);

        game.listen(GamePlayerEvents.ACCEPT, joinAcceptor -> joinAcceptor.teleport(world, map.getRegions().finish().center()));
        game.listen(GamePlayerEvents.OFFER, JoinOffer::acceptParticipants);

        game.listen(GameActivityEvents.TICK, timeTrial::tick);
    }

    private EventResult addPlayer(ServerPlayerEntity player) {
        this.stageManager.spawnPlayer(player);

        return EventResult.DENY;
    }

    private EventResult removePlayer(ServerPlayerEntity player) {
        return EventResult.DENY;
    }

    private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0f);
        this.stageManager.spawnPlayer(player);
        return EventResult.DENY;
    }

    private void tick() {
        TimeTrialStageManager.TickResult result = this.stageManager.tick(this.gameSpace, this.world);
        this.widgets.tick(this.gameSpace, this.stageManager, this.leaderboard);
        switch (result) {
            case IDLE:
                return;
        }

    }
}
