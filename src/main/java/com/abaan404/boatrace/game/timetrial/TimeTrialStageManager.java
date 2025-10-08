package com.abaan404.boatrace.game.timetrial;

import java.util.Set;

import com.abaan404.boatrace.game.BoatRaceSpawnLogic;
import com.abaan404.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.game.gameplay.SplitsManager;
import com.abaan404.boatrace.items.BoatRaceItems;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.maps.TrackMap;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

/**
 * Handles time trial state.
 */
public class TimeTrialStageManager {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final TrackMap track;

    public final CheckpointsManager checkpoints;
    public final SplitsManager splits;

    private final BoatRaceSpawnLogic spawnLogic;
    private final Set<PlayerRef> participants;

    public TimeTrialStageManager(GameSpace gameSpace, ServerWorld world, TrackMap track) {
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
        PlayerRef ref = PlayerRef.of(player);
        TrackMap.Regions regions = this.track.getRegions();
        TrackMap.Meta meta = this.track.getMeta();

        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);

        TrackMap.RespawnRegion respawn = regions.checkpoints().getFirst();

        // spawn spectators at spawn without boats
        if (!this.participants.contains(ref)) {
            this.spawnLogic.spawnPlayer(player, respawn);
            return;
        }

        if (!regions.gridBoxes().isEmpty()) {
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
        PlayerRef ref = PlayerRef.of(player);

        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);

        if (this.checkpoints.getCheckpointIndex(ref) != -1) {
            this.spawnLogic.spawnPlayer(player, this.checkpoints.getCheckpoint(ref));
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

        if (this.participants.contains(PlayerRef.of(player))) {
            inventory.setStack(8, BoatRaceItems.RESET.getDefaultStack());
            inventory.setStack(7, BoatRaceItems.TIME_TRIAL_RESPAWN.getDefaultStack());
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
        PlayerRef ref = PlayerRef.of(player);
        this.participants.remove(ref);

        this.checkpoints.reset(ref);
        this.splits.reset(ref);
        this.splits.stop(ref);

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
    }

    /**
     * Tick the player and act on events from checkpoints and/or splits.
     */
    public void tickPlayers() {
        ServerWorld overworld = this.gameSpace.getServer().getWorld(World.OVERWORLD);
        Leaderboard leaderboard = overworld.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        this.splits.tick(this.world);

        for (ServerPlayerEntity player : this.gameSpace.getPlayers().participants()) {
            PlayerRef ref = PlayerRef.of(player);

            if (!this.participants.contains(ref)) {
                continue;
            }

            switch (this.checkpoints.tick(player)) {
                case BEGIN: {
                    this.splits.run(ref);
                    this.splits.recordSplit(ref);
                    break;
                }

                case LOOP: {
                    leaderboard = leaderboard.trySubmit(overworld, this.track, new PersonalBest(
                            player.getNameForScoreboard(),
                            player.getUuid(),
                            this.splits.getTimer(ref),
                            this.splits.getSplits(ref)));

                    // start a new run
                    this.splits.reset(ref);
                    this.splits.recordSplit(ref);
                    break;
                }

                case FINISH: {
                    leaderboard = leaderboard.trySubmit(overworld, this.track, new PersonalBest(
                            player.getNameForScoreboard(),
                            player.getUuid(),
                            this.splits.getTimer(ref),
                            this.splits.getSplits(ref)));

                    // stop the timer
                    this.splits.stop(ref);
                    break;
                }

                case CHECKPOINT: {
                    this.splits.recordSplit(ref);
                    break;
                }

                case IDLE: {
                    break;
                }
            }
        }
    }

    /**
     * Transition the player to a spectator. They can roam freely and explore the
     * track.
     *
     * @param player The player's ref
     */
    public void toSpectator(PlayerRef player) {
        this.participants.remove(player);

        this.checkpoints.reset(player);
        this.splits.reset(player);
        this.splits.stop(player);
    }

    /**
     * Transition a player to a participant. They can set and submit runs in this
     * mode.
     *
     * @param player The player's ref
     */
    public void toParticipant(PlayerRef player) {
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
        return this.participants.contains(PlayerRef.of(player));
    }
}
