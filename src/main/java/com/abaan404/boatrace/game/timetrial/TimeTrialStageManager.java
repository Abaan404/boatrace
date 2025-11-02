package com.abaan404.boatrace.game.timetrial;

import java.util.Set;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.game.BoatRaceItems;
import com.abaan404.boatrace.game.BoatRaceSpawnLogic;
import com.abaan404.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.game.gameplay.SplitsManager;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.utils.TextUtil;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;

/**
 * Handles time trial state.
 */
public class TimeTrialStageManager {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final BoatRaceTrack track;

    public final CheckpointsManager checkpoints;
    public final SplitsManager splits;

    private final BoatRaceSpawnLogic spawnLogic;
    private final Set<BoatRacePlayer> participants;

    public TimeTrialStageManager(GameSpace gameSpace, ServerWorld world, BoatRaceTrack track) {
        this.gameSpace = gameSpace;
        this.track = track;
        this.world = world;

        this.checkpoints = new CheckpointsManager(track);
        this.splits = new SplitsManager();

        this.spawnLogic = new BoatRaceSpawnLogic(world);
        this.participants = new ObjectOpenHashSet<>();
    }

    /**
     * Spawn a player on the track with a boat. Spectators spawn in free roam
     * 
     * @param player The player.
     */
    public void spawnPlayer(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        BoatRaceTrack.Regions regions = this.track.getRegions();
        BoatRaceTrack.Meta meta = this.track.getMeta();

        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);

        BoatRaceTrack.RespawnRegion respawn = regions.checkpoints().getFirst();

        // spawn spectators at spawn without boats
        if (!this.participants.contains(bPlayer)) {
            this.spawnLogic.spawnPlayer(player, respawn);
            return;
        }

        if (!regions.spawn().equals(BoatRaceTrack.RespawnRegion.of())) {
            respawn = regions.spawn();
        } else if (!regions.gridBoxes().isEmpty()) {
            respawn = regions.gridBoxes().getFirst();
        } else if (!regions.checkpoints().isEmpty()) {
            switch (meta.layout()) {
                case CIRCULAR: {
                    respawn = regions.checkpoints().getLast();
                    break;
                }
                case LINEAR: {
                    respawn = regions.checkpoints().getFirst();
                    break;
                }
            }
        }

        this.spawnLogic.spawnPlayer(player, respawn);
        this.spawnLogic.spawnVehicleAndRide(player).orElseThrow();
    }

    /**
     * Respawn a player to their last checkpoint.
     *
     * @param player The player.
     */
    public void respawnPlayer(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);

        if (this.checkpoints.getCheckpointIndex(bPlayer) != -1) {
            this.spawnLogic.spawnPlayer(player, this.checkpoints.getCheckpoint(bPlayer));
            this.spawnLogic.spawnVehicleAndRide(player).orElseThrow();

        } else {
            this.spawnPlayer(player);
        }
    }

    /**
     * Gives the players items to control their state on track.
     *
     * @param player The player
     */
    public void updatePlayerInventory(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();

        if (this.participants.contains(BoatRacePlayer.of(player))) {
            inventory.setStack(8, BoatRaceItems.RESET.getDefaultStack());
            inventory.setStack(7, BoatRaceItems.RESPAWN.getDefaultStack());
        } else {
            inventory.setStack(8, BoatRaceItems.RESET.getDefaultStack());
        }
    }

    /**
     * Despawn a player from the game.
     *
     * @param player The player.
     */
    public void despawnPlayer(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        this.participants.remove(bPlayer);

        this.checkpoints.reset(bPlayer);
        this.splits.reset(bPlayer);
        this.splits.stop(bPlayer);

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
    }

    /**
     * Tick the player and act on events from checkpoints and/or splits.
     */
    public void tickPlayers() {
        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            if (!this.participants.contains(bPlayer)) {
                continue;
            }

            switch (this.checkpoints.tick(player)) {
                case BEGIN: {
                    this.splits.run(bPlayer);
                    this.splits.recordSplit(bPlayer);
                    break;
                }

                case LOOP: {
                    this.splits.recordSplit(bPlayer);
                    this.submit(player);

                    // start a new run
                    this.splits.reset(bPlayer);
                    this.splits.recordSplit(bPlayer);
                    break;
                }

                case FINISH: {
                    this.splits.recordSplit(bPlayer);
                    this.submit(player);

                    // stop the timer
                    this.splits.stop(bPlayer);
                    break;
                }

                case CHECKPOINT: {
                    this.splits.recordSplit(bPlayer);
                    break;
                }

                case IDLE: {
                    break;
                }
            }
        }

        this.splits.tick(this.world);
    }

    /**
     * Transition the player to a spectator. They can roam freely and explore the
     * track.
     *
     * @param player The player's bPlayer
     */
    public void toSpectator(BoatRacePlayer player) {
        this.participants.remove(player);

        this.checkpoints.reset(player);
        this.splits.reset(player);
        this.splits.stop(player);
    }

    /**
     * Transition a player to a participant. They can set and submit runs in this
     * mode.
     *
     * @param player The player's bPlayer
     */
    public void toParticipant(BoatRacePlayer player) {
        this.participants.add(player);

        this.checkpoints.reset(player);
        this.splits.reset(player);
        this.splits.stop(player);
    }

    /**
     * Check if the player is a participant.
     *
     * @param player The player.
     * @return If they are on track ready to set a time.
     */
    public boolean isParticipant(ServerPlayerEntity player) {
        return this.participants.contains(BoatRacePlayer.of(player));
    }

    /**
     * Submit a leaderboard time.
     *
     * @param player The player to create a new pb for.
     */
    private void submit(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        // use the overworld for persistent storage
        ServerWorld overworld = this.gameSpace.getServer().getWorld(World.OVERWORLD);
        Leaderboard leaderboard = overworld.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        PersonalBest pb = new PersonalBest(bPlayer, this.splits.getSplits(bPlayer));
        Leaderboard newLeaderboard = leaderboard.trySubmit(overworld, this.track, pb);

        if (newLeaderboard != leaderboard) {
            int position = newLeaderboard.getLeaderboardPosition(this.track, bPlayer);
            GameSpacePlayers players = this.gameSpace.getPlayers();

            players.sendMessage(TextUtil.chatNewPersonalBest(pb, position));
        } else {
            player.sendMessage(TextUtil.chatNewTime(pb.timer()));
        }
    }
}
