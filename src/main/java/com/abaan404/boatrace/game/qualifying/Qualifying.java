package com.abaan404.boatrace.game.qualifying;

import java.util.function.Consumer;

import com.abaan404.boatrace.game.BoatRaceConfig;
import com.abaan404.boatrace.items.BoatRaceItems;
import com.abaan404.boatrace.maps.TrackMap;
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
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

/**
 * A qualification game to set the best lap in limited time to determine the
 * player's order in a race.
 */
public class Qualifying {
    private final QualifyingStageManager stageManager;
    private final QualifyingWidgets widgets;

    private Qualifying(GameSpace gameSpace, ServerWorld world, TrackMap track, GlobalWidgets widgets,
            BoatRaceConfig config) {
        this.stageManager = new QualifyingStageManager(gameSpace, config.qualifying().orElseThrow(), world, track);
        this.widgets = new QualifyingWidgets(gameSpace, world, widgets, track);
    }

    public static void open(GameActivity game, BoatRaceConfig config, ServerWorld world, TrackMap track) {
        GlobalWidgets widgets = GlobalWidgets.addTo(game);

        Qualifying qualifying = new Qualifying(game.getGameSpace(), world, track, widgets, config);

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
        game.listen(PlayerDeathEvent.EVENT, qualifying::onPlayerDeath);
        game.listen(ItemUseEvent.EVENT, qualifying::onItemUse);

        game.listen(GamePlayerEvents.OFFER, qualifying::offerPlayer);
        game.listen(GamePlayerEvents.ACCEPT, joinAcceptor -> joinAcceptor.teleport(world, Vec3d.ZERO));
        game.listen(GamePlayerEvents.ADD, qualifying::addPlayer);
        game.listen(GamePlayerEvents.REMOVE, qualifying::removePlayer);

        game.listen(GameActivityEvents.TICK, qualifying::tick);
    }

    private JoinOfferResult.Accept offerPlayer(JoinOffer offer) {
        Consumer<PlayerRef> mode = this.stageManager::toSpectator;
        switch (offer.intent()) {
            case PLAY:
                mode = this.stageManager::toParticipant;
                break;
            case SPECTATE:
                mode = this.stageManager::toSpectator;
                break;
        }

        for (GameProfile profile : offer.players()) {
            mode.accept(PlayerRef.of(profile));
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

        // turn them into a participant and spawn them as if they just started
        if (item.equals(BoatRaceItems.RESET)) {
            this.stageManager.toParticipant(PlayerRef.of(player));
            this.stageManager.spawnPlayer(player);
            this.stageManager.updatePlayerInventory(player);
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    private void tick() {
        this.stageManager.tickPlayers();
        this.widgets.tick(this.stageManager);
    }
}
