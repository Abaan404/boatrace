package com.abaan404.boatrace.game.timetrial;

import com.abaan404.boatrace.BoatRaceItems;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.BoatRacePlayerEvents;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
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
 * Singleplayer game to practice a map and set a best time against a
 * leaderboard.
 */
public class TimeTrial {
    private final TimeTrialStageManager stageManager;
    private final TimeTrialWidgets widgets;

    private TimeTrial(GameSpace gameSpace, ServerWorld world, BoatRaceTrack track, GlobalWidgets widgets) {
        this.stageManager = new TimeTrialStageManager(gameSpace, world, track);
        this.widgets = new TimeTrialWidgets(gameSpace, widgets, track);
    }

    public static void open(GameActivity game, ServerWorld world, BoatRaceTrack track) {
        GlobalWidgets widgets = GlobalWidgets.addTo(game);

        TimeTrial timeTrial = new TimeTrial(game.getGameSpace(), world, track, widgets);

        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, game.getGameSpace().getServer());
        world.setTimeOfDay(track.getAttributes().timeOfDay());

        game.setRule(GameRuleType.PORTALS, EventResult.DENY);
        game.setRule(GameRuleType.ICE_MELT, EventResult.DENY);
        game.setRule(GameRuleType.PVP, EventResult.DENY);
        game.setRule(GameRuleType.HUNGER, EventResult.DENY);
        game.setRule(GameRuleType.FALL_DAMAGE, EventResult.DENY);
        game.setRule(GameRuleType.MODIFY_INVENTORY, EventResult.DENY);
        game.setRule(GameRuleType.SWAP_OFFHAND, EventResult.DENY);
        game.setRule(GameRuleType.THROW_ITEMS, EventResult.DENY);
        game.setRule(GameRuleType.CRAFTING, EventResult.DENY);
        game.setRule(GameRuleType.PLACE_BLOCKS, EventResult.DENY);
        game.setRule(GameRuleType.BREAK_BLOCKS, EventResult.DENY);

        game.listen(PlayerDamageEvent.EVENT, (player, source, amount) -> EventResult.DENY);
        game.listen(PlayerDeathEvent.EVENT, timeTrial::onPlayerDeath);
        game.listen(ItemUseEvent.EVENT, timeTrial::onItemUse);

        game.listen(BoatRacePlayerEvents.DISMOUNT, timeTrial::onDismount);

        game.listen(GamePlayerEvents.OFFER, timeTrial::offerPlayer);
        game.listen(GamePlayerEvents.ACCEPT, joinAcceptor -> joinAcceptor.teleport(world, Vec3d.ZERO));
        game.listen(GamePlayerEvents.ADD, timeTrial::addPlayer);
        game.listen(GamePlayerEvents.REMOVE, timeTrial::removePlayer);

        game.listen(GameActivityEvents.TICK, timeTrial::tick);
    }

    private JoinOfferResult.Accept offerPlayer(JoinOffer offer) {
        for (GameProfile profile : offer.players()) {
            BoatRacePlayer player = BoatRacePlayer.of(profile);

            switch (offer.intent()) {
                case PLAY:
                    this.stageManager.toParticipant(player);
                    break;
                case SPECTATE:
                    this.stageManager.toSpectator(player);
                    break;
            }
        }

        return offer.accept();
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.stageManager.spawnPlayer(player);
        this.stageManager.updatePlayerInventory(player);
    }

    private void removePlayer(ServerPlayerEntity player) {
        this.stageManager.despawnPlayer(player);
    }

    private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0f);
        this.stageManager.respawnPlayer(player);
        this.stageManager.updatePlayerInventory(player);
        return EventResult.DENY;
    }

    private EventResult onDismount(ServerPlayerEntity player, Entity vehicle) {
        vehicle.discard();
        this.stageManager.toSpectator(BoatRacePlayer.of(player));
        this.stageManager.updatePlayerInventory(player);

        return EventResult.DENY;
    }

    private ActionResult onItemUse(ServerPlayerEntity player, Hand hand) {
        ItemStack item = player.getStackInHand(hand);

        // turn them into a participant and spawn them as if they just started
        if (item.getItem().equals(BoatRaceItems.RESET)) {
            this.stageManager.toSpectator(BoatRacePlayer.of(player));
            this.stageManager.toParticipant(BoatRacePlayer.of(player));

            this.stageManager.spawnPlayer(player);
            this.stageManager.updatePlayerInventory(player);
            return ActionResult.CONSUME;
        }

        // only respawn the player at their last checkpoint
        else if (item.getItem().equals(BoatRaceItems.RESPAWN)) {
            this.stageManager.respawnPlayer(player);
            this.stageManager.updatePlayerInventory(player);
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    private void tick() {
        this.widgets.tick(stageManager);
        this.stageManager.tickPlayers();
    }
}
