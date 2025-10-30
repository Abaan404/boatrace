package com.abaan404.boatrace.game.race;

import java.util.List;
import java.util.function.Consumer;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.game.BoatRaceItems;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.player.JoinOfferResult;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

/**
 * boat goes nyoom here.
 */
public class Race {
    private final RaceStageManager stageManager;
    private final RaceWidgets widgets;

    private Race(GameSpace gameSpace, ServerWorld world, BoatRaceTrack track, GlobalWidgets widgets,
            BoatRaceConfig config, List<PersonalBest> qualifyingRecords) {
        this.stageManager = new RaceStageManager(gameSpace, config, world, track, qualifyingRecords);
        this.widgets = new RaceWidgets(gameSpace, widgets, track);
    }

    public static void open(GameActivity game, BoatRaceConfig config, ServerWorld world, BoatRaceTrack track,
            List<PersonalBest> records) {
        GlobalWidgets widgets = GlobalWidgets.addTo(game);

        Race race = new Race(game.getGameSpace(), world, track, widgets, config, records);

        game.setRule(GameRuleType.PORTALS, EventResult.DENY);

        game.setRule(GameRuleType.PVP, EventResult.DENY);
        game.setRule(GameRuleType.HUNGER, EventResult.DENY);
        game.setRule(GameRuleType.FALL_DAMAGE, EventResult.DENY);
        game.setRule(GameRuleType.MODIFY_INVENTORY, EventResult.DENY);
        game.setRule(GameRuleType.SWAP_OFFHAND, EventResult.DENY);
        game.setRule(GameRuleType.THROW_ITEMS, EventResult.DENY);
        game.setRule(GameRuleType.CRAFTING, EventResult.DENY);
        game.setRule(GameRuleType.PLACE_BLOCKS, EventResult.DENY);
        game.setRule(GameRuleType.BREAK_BLOCKS, EventResult.DENY);
        game.setRule(GameRuleType.DISMOUNT_VEHICLE, EventResult.DENY);

        game.listen(PlayerDamageEvent.EVENT, (player, source, amount) -> EventResult.DENY);
        game.listen(PlayerDeathEvent.EVENT, race::onPlayerDeath);
        game.listen(ItemUseEvent.EVENT, race::onItemUse);

        game.listen(GamePlayerEvents.OFFER, race::offerPlayer);
        game.listen(GamePlayerEvents.ACCEPT, joinAcceptor -> joinAcceptor.teleport(world, Vec3d.ZERO));
        game.listen(GamePlayerEvents.ADD, race::addPlayer);
        game.listen(GamePlayerEvents.REMOVE, race::removePlayer);

        game.listen(GameActivityEvents.TICK, race::onTick);
    }

    private JoinOfferResult.Accept offerPlayer(JoinOffer offer) {
        Consumer<BoatRacePlayer> mode = this.stageManager::toSpectator;

        switch (offer.intent()) {
            case PLAY:
                mode = this.stageManager::toParticipant;
                break;
            case SPECTATE:
                mode = this.stageManager::toSpectator;
                break;
        }

        for (GameProfile profile : offer.players()) {
            mode.accept(BoatRacePlayer.of(profile));
        }

        return offer.accept();
    }

    private EventResult addPlayer(ServerPlayerEntity player) {
        this.stageManager.spawnPlayer(player);
        this.stageManager.updatePlayerInventory(player);
        return EventResult.DENY;
    }

    private EventResult removePlayer(ServerPlayerEntity player) {
        this.stageManager.despawnPlayer(player);
        return EventResult.DENY;
    }

    private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0f);
        this.stageManager.spawnPlayer(player);
        this.stageManager.updatePlayerInventory(player);
        return EventResult.DENY;
    }

    private ActionResult onItemUse(ServerPlayerEntity player, Hand hand) {
        Item item = player.getStackInHand(hand).getItem();

        // only respawn the player at their last checkpoint
        if (item.equals(BoatRaceItems.RESPAWN)) {
            this.stageManager.respawnPlayer(player);
            this.stageManager.updatePlayerInventory(player);
            return ActionResult.CONSUME;
        }

        // cycle leaderboard type
        else if (item.equals(BoatRaceItems.CYCLE_LEADERBOARD)) {
            this.widgets.cycleLeaderboard(player);
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    private void onTick() {
        this.stageManager.tickPlayers();
        this.widgets.tick(this.stageManager);
    }
}
