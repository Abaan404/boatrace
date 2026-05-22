package com.abaan404.boatrace.game.race;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRaceGameRules;
import com.abaan404.boatrace.BoatRaceItems;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.events.PlayerDismountEvent;
import com.abaan404.boatrace.events.PlayerPitSuccess;
import com.abaan404.boatrace.gameplay.DesyncIndicator;
import com.abaan404.boatrace.gameplay.Teams;
import com.mojang.authlib.GameProfile;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
    private final Set<BoatRacePlayer> qualified;
    private final boolean acceptUnqualified;

    private Race(GameSpace gameSpace, BoatRaceConfig.Race config, BoatRaceTrack track, Teams teams,
            ServerLevel world, GlobalWidgets widgets, List<BoatRacePlayer> gridOrder) {
        this.stageManager = new RaceStageManager(gameSpace, config, world, track, teams);
        this.widgets = new RaceWidgets(gameSpace, widgets, track);
        this.qualified = Set.copyOf(gridOrder);
        this.acceptUnqualified = config.acceptUnqualified() || this.qualified.isEmpty();

        switch (config.gridType()) {
            case NORMAL: {
                break;
            }

            case RANDOM: {
                Collections.shuffle(gridOrder);
                break;
            }

            case REVERSED: {
                Collections.reverse(gridOrder);
                break;
            }
        }

        for (BoatRacePlayer player : gridOrder) {
            this.stageManager.toParticipant(player);
        }

        if (this.acceptUnqualified) {
            for (ServerPlayer player : gameSpace.getPlayers().participants()) {
                this.stageManager.toParticipant(BoatRacePlayer.of(player));
            }
        }
    }

    public static void open(GameActivity game, BoatRaceConfig.Race config, ServerLevel world, BoatRaceTrack track,
            Teams teams, List<BoatRacePlayer> gridOrder) {
        GlobalWidgets widgets = GlobalWidgets.addTo(game);
        DesyncIndicator.addTo(game, world);

        Race race = new Race(game.getGameSpace(), config, track, teams, world, widgets, gridOrder);

        world.getGameRules().set(GameRules.ADVANCE_TIME, false, game.getGameSpace().getServer());
        world.setDayTime(track.getAttributes().timeOfDay());

        game.setRule(GameRuleType.PORTALS, EventResult.DENY);
        game.setRule(GameRuleType.ICE_MELT, EventResult.DENY);
        game.setRule(GameRuleType.PVP, EventResult.DENY);
        game.setRule(GameRuleType.HUNGER, EventResult.DENY);
        game.setRule(GameRuleType.FALL_DAMAGE, EventResult.DENY);
        game.setRule(GameRuleType.SWAP_OFFHAND, EventResult.DENY);
        game.setRule(GameRuleType.THROW_ITEMS, EventResult.DENY);
        game.setRule(GameRuleType.CRAFTING, EventResult.DENY);
        game.setRule(GameRuleType.PLACE_BLOCKS, EventResult.DENY);
        game.setRule(GameRuleType.BREAK_BLOCKS, EventResult.DENY);
        game.setRule(GameRuleType.DISMOUNT_VEHICLE, EventResult.DENY);
        game.setRule(BoatRaceGameRules.SINGLE_SEAT, EventResult.ALLOW);
        game.setRule(BoatRaceGameRules.MODIFY_INVENTORIES, EventResult.DENY);

        game.listen(PlayerDamageEvent.EVENT, (player, source, amount) -> EventResult.DENY);
        game.listen(PlayerDeathEvent.EVENT, race::onPlayerDeath);
        game.listen(ItemUseEvent.EVENT, race::onItemUse);
        game.listen(PlayerDismountEvent.EVENT, race::onDismount);
        game.listen(PlayerPitSuccess.EVENT, race::onPitSuccess);

        game.listen(GamePlayerEvents.OFFER, race::offerPlayer);
        game.listen(GamePlayerEvents.ACCEPT, joinAcceptor -> joinAcceptor.teleport(world, Vec3.ZERO));
        game.listen(GamePlayerEvents.ADD, race::addPlayer);
        game.listen(GamePlayerEvents.REMOVE, race::removePlayer);

        game.listen(GameActivityEvents.TICK, race::onTick);
    }

    private JoinOfferResult.Accept offerPlayer(JoinOffer offer) {
        List<BoatRacePlayer> toAssign = new ObjectArrayList<>();

        for (GameProfile profile : offer.players()) {
            BoatRacePlayer player = BoatRacePlayer.of(profile);

            // race has begun, spectate only unless a participant already
            if (!this.stageManager.goCountdown.isCounting() && !this.stageManager.isParticipant(player)) {
                this.stageManager.toSpectator(player);
                this.stageManager.teams.unassign(player);
            }

            // accepting anyone, respect intent
            else if (this.acceptUnqualified) {
                switch (offer.intent()) {
                    case PLAY:
                        this.stageManager.toParticipant(player);
                        toAssign.add(player);
                        break;
                    case SPECTATE:
                        this.stageManager.toSpectator(player);
                        this.stageManager.teams.unassign(player);
                        break;
                }
            }

            // only qualified players can participate
            else if (this.qualified.contains(player)) {
                this.stageManager.toParticipant(player);
                toAssign.add(player);
            }

            // fallback spectator
            else {
                this.stageManager.toSpectator(player);
                this.stageManager.teams.unassign(player);
            }
        }

        Collections.shuffle(toAssign);
        for (BoatRacePlayer player : toAssign) {
            this.stageManager.teams.assign(player);
        }

        return offer.accept();
    }

    private void addPlayer(ServerPlayer player) {
        this.widgets.sendTrackMessage(player);
        this.stageManager.spawnPlayer(player);
        this.stageManager.updatePlayerInventory(player);
    }

    private void removePlayer(ServerPlayer player) {
        this.stageManager.despawnPlayer(player);
    }

    private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
        player.setHealth(20.0f);
        this.stageManager.spawnPlayer(player);
        this.stageManager.updatePlayerInventory(player);
        return EventResult.DENY;
    }

    private InteractionResult onItemUse(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // only respawn the player at their last checkpoint
        if (stack.getItem().equals(BoatRaceItems.RESPAWN)) {
            this.stageManager.respawnPlayer(player);
            this.stageManager.updatePlayerInventory(player);
            return InteractionResult.CONSUME;
        }

        // cycle leaderboard type
        else if (stack.getItem().equals(BoatRaceItems.CYCLE_LEADERBOARD)) {
            RaceWidgets.LeaderboardType nextType = this.widgets.cycleLeaderboard(player);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                    List.of(),
                    List.of(),
                    List.of(nextType.toString()),
                    List.of()));

            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private EventResult onDismount(ServerPlayer player, Entity vehicle) {
        this.stageManager.respawnPlayer(player);
        this.stageManager.updatePlayerInventory(player);

        return EventResult.DENY;
    }

    private EventResult onPitSuccess(ServerPlayer player) {
        if (!this.stageManager.isParticipant(BoatRacePlayer.of(player))) {
            return EventResult.PASS;
        }

        if (!this.stageManager.pits.finishPit(player)) {
            return EventResult.DENY;
        }

        return EventResult.ALLOW;
    }

    private void onTick() {
        this.stageManager.tickPlayers();
        this.widgets.tick(this.stageManager);
    }
}
