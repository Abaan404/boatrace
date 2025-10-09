package com.abaan404.boatrace.game.lobby;

import java.util.List;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.game.timetrial.TimeTrial;
import com.abaan404.boatrace.maps.LobbyMap;
import com.abaan404.boatrace.maps.TrackMap;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

/**
 * A lobby where the player can configure a game to play it.
 */
public class Lobby {
    private final GameSpace gameSpace;
    private final ServerWorld world;

    private final BoatRaceConfig config;
    private final LobbySpawnLogic spawnLogic;
    private final LobbyStageManager stageManager;

    private Lobby(GameSpace gameSpace, ServerWorld world, LobbyMap map, List<TrackMap> tracks, BoatRaceConfig config) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.world = world;
        this.stageManager = new LobbyStageManager(tracks);
        this.spawnLogic = new LobbySpawnLogic(world, map);
    }

    public static void open(GameActivity game, BoatRaceConfig config, ServerWorld world, LobbyMap map,
            List<TrackMap> tracks) {
        Lobby lobby = new Lobby(game.getGameSpace(), world, map, tracks, config);

        game.listen(GameActivityEvents.REQUEST_START, lobby::requestStart);
        game.listen(GamePlayerEvents.ADD, lobby::addPlayer);
        game.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
        game.listen(GamePlayerEvents.ACCEPT, joinAcceptor -> joinAcceptor.teleport(world, Vec3d.ZERO));
        game.listen(BlockUseEvent.EVENT, lobby::onUse);
        game.listen(PlayerDeathEvent.EVENT, lobby::onPlayerDeath);
    }

    private ActionResult onUse(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        // TODO allow player to configure the game
        return ActionResult.PASS;
    }

    private GameResult requestStart() {
        if (this.stageManager.getTrack().isEmpty()) {
            return GameResult.error(Text.of("No Track chosen"));
        }

        this.gameSpace.setActivity(game -> {
            TimeTrial.open(game, this.config, this.world, this.stageManager.getTrack().get());
        });

        return GameResult.ok();
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0f);
        this.spawnPlayer(player);
        return EventResult.DENY;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);
    }
}
