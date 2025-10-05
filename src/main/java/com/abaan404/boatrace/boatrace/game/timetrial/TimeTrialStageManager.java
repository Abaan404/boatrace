package com.abaan404.boatrace.boatrace.game.timetrial;

import java.util.Set;

import com.abaan404.boatrace.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.boatrace.game.gameplay.SplitsManager;
import com.abaan404.boatrace.boatrace.game.items.BoatRaceItems;
import com.abaan404.boatrace.boatrace.game.maps.TrackMap;
import com.abaan404.boatrace.boatrace.game.maps.TrackMap.Regions;
import com.abaan404.boatrace.boatrace.game.maps.TrackMap.RespawnRegion;
import com.abaan404.boatrace.boatrace.game.timetrial.TimeTrialLeaderboard.PersonalBest;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

    private final TimeTrialSpawnLogic spawnLogic;
    private final Set<PlayerRef> spectators;

    public TimeTrialStageManager(GameSpace gameSpace, ServerWorld world, TrackMap track) {
        this.gameSpace = gameSpace;
        this.track = track;
        this.world = world;

        this.checkpoints = new CheckpointsManager(track);
        this.splits = new SplitsManager();

        this.spawnLogic = new TimeTrialSpawnLogic(world);
        this.spectators = new ObjectOpenHashSet<>();
    }

    /**
     * Spawn a player on the track, if they are a spectator, spawn them with a boat.
     *
     * @param player The player.
     */
    public void spawnPlayer(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        boolean withVehicle = !this.spectators.contains(ref);

        Regions regions = this.track.getRegions();
        this.spawnLogic.resetPlayer(player);

        if (!regions.gridBoxes().isEmpty()) {
            RespawnRegion gridBox = regions.gridBoxes().getFirst();
            this.spawnLogic.spawnPlayer(player, this.world, gridBox, withVehicle);

        } else if (!regions.checkpoints().isEmpty()) {
            RespawnRegion checkpoint = regions.checkpoints().getLast();
            this.spawnLogic.spawnPlayer(player, this.world, checkpoint, withVehicle);

        } else {
            this.spawnLogic.spawnPlayer(player, this.world, regions.finish(), withVehicle);
        }
    }

    /**
     * Respawn a player to their last checkpoint.
     *
     * @param player The player.
     */
    public void respawnPlayer(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        boolean withVehicle = !this.spectators.contains(ref);

        this.spawnLogic.resetPlayer(player);

        if (this.checkpoints.getCheckpointIndex(ref) != -1) {
            RespawnRegion checkpoint = this.checkpoints.getCheckpoint(ref);
            this.spawnLogic.spawnPlayer(player, this.world, checkpoint, withVehicle);

        } else {
            this.spawnPlayer(player);
        }
    }

    /**
     * Despawn (or untrack) a player from the game.
     *
     * @param player The player.
     */
    public void despawnPlayer(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        this.spectators.remove(ref);
    }

    /**
     * Tick the player and act on events from checkpoints and/or splits.
     */
    public void tickPlayers() {
        ServerWorld overworld = this.gameSpace.getServer().getWorld(World.OVERWORLD);
        TimeTrialLeaderboard leaderboard = overworld.getAttachedOrCreate(TimeTrialLeaderboard.ATTACHMENT);

        for (ServerPlayerEntity player : this.gameSpace.getPlayers().participants()) {
            PlayerRef ref = PlayerRef.of(player);

            if (this.spectators.contains(ref)) {
                continue;
            }

            this.splits.tick(ref, this.world);

            switch (this.checkpoints.tick(player)) {
                case BEGIN: {
                    this.splits.run(ref);
                    break;
                }

                case FINISH: {
                    PersonalBest pb = leaderboard.getPersonalBest(this.track, ref.id());
                    float currentTimer = this.splits.getTimer(ref);

                    if (currentTimer < pb.timer() || Float.isNaN(pb.timer())) {
                        leaderboard = overworld.setAttached(TimeTrialLeaderboard.ATTACHMENT,
                                leaderboard.setPersonalBest(this.track, new PersonalBest(
                                        player.getNameForScoreboard(),
                                        player.getUuid(),
                                        currentTimer,
                                        this.splits.getSplits(ref))));
                    }

                    this.splits.reset(ref);
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
     * @param player The player
     */
    public void toSpectator(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        this.spectators.add(ref);

        this.checkpoints.reset(ref);
        this.splits.reset(ref);
        this.splits.stop(ref);

        PlayerInventory inventory = player.getInventory();

        inventory.clear();
        inventory.setStack(8, BoatRaceItems.TIME_TRIAL_RESET.getDefaultStack());
    }

    /**
     * Transition a player to a participant. They can set and submit runs in this
     * mode.
     *
     * @param player The player.
     */
    public void toParticipant(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        this.spectators.remove(ref);

        this.checkpoints.reset(ref);
        this.splits.reset(ref);
        this.splits.stop(ref);

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setStack(8, BoatRaceItems.TIME_TRIAL_RESET.getDefaultStack());
        inventory.setStack(7, BoatRaceItems.TIME_TRIAL_RESPAWN.getDefaultStack());
    }

    /**
     * Check if the player is a spectator.
     *
     * @param player The player.
     * @return If they are in free roam spectator.
     */
    public boolean isSpectator(ServerPlayerEntity player) {
        return this.spectators.contains(PlayerRef.of(player));
    }
}
