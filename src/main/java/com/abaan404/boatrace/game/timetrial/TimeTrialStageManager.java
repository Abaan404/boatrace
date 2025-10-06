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
    private final Set<PlayerRef> spectators;

    public TimeTrialStageManager(GameSpace gameSpace, ServerWorld world, TrackMap track) {
        this.gameSpace = gameSpace;
        this.track = track;
        this.world = world;

        this.checkpoints = new CheckpointsManager(track);
        this.splits = new SplitsManager();

        this.spawnLogic = new BoatRaceSpawnLogic(world);
        this.spectators = new ObjectOpenHashSet<>();
    }

    /**
     * Spawn a player on the track, if they are a spectator, spawn them with a boat.
     *
     * @param player The player.
     */
    public void spawnPlayer(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        TrackMap.Regions regions = this.track.getRegions();

        spawnLogic.resetPlayer(player, GameMode.ADVENTURE);

        if (this.spectators.contains(ref)) {
            this.spawnLogic.spawnPlayer(player, regions.finish());
            return;
        }

        TrackMap.RespawnRegion respawn = regions.finish();

        if (!regions.gridBoxes().isEmpty()) {
            respawn = regions.gridBoxes().getFirst();
        } else if (!regions.checkpoints().isEmpty()) {
            respawn = regions.checkpoints().getLast();
        }

        this.spawnLogic.spawnPlayer(player, respawn);
        this.spawnLogic.spawnVehicle(player).orElseThrow();
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
            this.spawnLogic.spawnVehicle(player).orElseThrow();

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
        Leaderboard leaderboard = overworld.getAttachedOrCreate(Leaderboard.ATTACHMENT);

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
                        leaderboard = overworld.setAttached(Leaderboard.ATTACHMENT,
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
        inventory.setStack(8, BoatRaceItems.RESET.getDefaultStack());
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
        inventory.setStack(8, BoatRaceItems.RESET.getDefaultStack());
        inventory.setStack(7, BoatRaceItems.TIME_TRIAL_RESPAWN.getDefaultStack());
    }

    /**
     * Check if the player is a participant.
     *
     * @param player The player.
     * @return If they are on track ready to set a time.
     */
    public boolean isParticipant(ServerPlayerEntity player) {
        return !this.spectators.contains(PlayerRef.of(player));
    }
}
