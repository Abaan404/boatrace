package com.abaan404.boatrace.game.race;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRaceItems;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.gameplay.Teams;
import com.mojang.authlib.GameProfile;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
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
    private final Set<BoatRacePlayer> qualified;

    private Race(GameSpace gameSpace, BoatRaceConfig.Race config, BoatRaceTrack track, Teams teams,
            ServerWorld world, GlobalWidgets widgets, List<BoatRacePlayer> gridOrder) {
        this.stageManager = new RaceStageManager(gameSpace, config, world, track, teams);
        this.widgets = new RaceWidgets(gameSpace, widgets, track);
        this.qualified = Set.copyOf(gridOrder);

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
    }

    public static void open(GameActivity game, BoatRaceConfig.Race config, ServerWorld world, BoatRaceTrack track,
            Teams teams, List<BoatRacePlayer> gridOrder) {
        GlobalWidgets widgets = GlobalWidgets.addTo(game);

        Race race = new Race(game.getGameSpace(), config, track, teams, world, widgets, gridOrder);

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
        for (GameProfile profile : offer.players()) {
            BoatRacePlayer player = BoatRacePlayer.of(profile);

            // noone qualified, respect intent
            if (this.qualified.isEmpty()) {
                switch (offer.intent()) {
                    case PLAY:
                        this.stageManager.toParticipant(player);
                        this.stageManager.teams.assign(player);
                        break;
                    case SPECTATE:
                        this.stageManager.toSpectator(player);
                        this.stageManager.teams.unassign(player);
                        break;
                }
            }

            // only qualified players can participate
            else {
                if (this.qualified.contains(player)) {
                    this.stageManager.toParticipant(player);
                } else {
                    this.stageManager.toSpectator(player);
                }
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
        this.stageManager.spawnPlayer(player);
        this.stageManager.updatePlayerInventory(player);
        return EventResult.DENY;
    }

    private ActionResult onItemUse(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // only respawn the player at their last checkpoint
        if (stack.getItem().equals(BoatRaceItems.RESPAWN)) {
            this.stageManager.respawnPlayer(player);
            this.stageManager.updatePlayerInventory(player);
            return ActionResult.CONSUME;
        }

        // cycle leaderboard type
        else if (stack.getItem().equals(BoatRaceItems.CYCLE_LEADERBOARD)) {
            RaceWidgets.LeaderboardType nextType = this.widgets.cycleLeaderboard(player);
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                    List.of(),
                    List.of(),
                    List.of(nextType.toString()),
                    List.of()));

            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    private void onTick() {
        this.stageManager.tickPlayers();
        this.widgets.tick(this.stageManager);
    }
}
