package com.abaan404.boatrace.game.race;

import java.util.SequencedSet;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.game.BoatRaceItems;
import com.abaan404.boatrace.game.BoatRaceSpawnLogic;
import com.abaan404.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.game.gameplay.CountdownManager;
import com.abaan404.boatrace.game.gameplay.LapManager;
import com.abaan404.boatrace.game.gameplay.SplitsManager;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameSpace;

public class RaceStageManager {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final BoatRaceConfig.Race config;
    private final BoatRaceTrack track;

    public final CheckpointsManager checkpoints;
    public final SplitsManager splits;
    public final LapManager laps;
    public final CountdownManager countdown;

    private final BoatRaceSpawnLogic spawnLogic;
    private final SequencedSet<BoatRacePlayer> participants;

    private long duration;

    public RaceStageManager(GameSpace gameSpace, BoatRaceConfig.Race config, ServerWorld world,
            BoatRaceTrack track) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;
        this.track = track;

        this.checkpoints = new CheckpointsManager(track);
        this.splits = new SplitsManager();
        this.laps = new LapManager(track);
        this.countdown = new CountdownManager(5000, world.getRandom().nextBetween(0, 1000));

        this.spawnLogic = new BoatRaceSpawnLogic(world);
        this.participants = new ObjectLinkedOpenHashSet<>();

        this.duration = 0;
    }

    /**
     * Spawn a player on the track with a boat. Spectators spawn in minecraft's
     * spectator gamemode
     *
     * @param player The player.
     */
    public void spawnPlayer(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        BoatRaceTrack.Regions regions = this.track.getRegions();

        // spawn spectators or non qualified at spawn without boats
        if (!this.participants.contains(bPlayer)) {
            this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
            this.spawnLogic.spawnPlayer(player, regions.checkpoints().getFirst());
            return;
        }

        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);

        BoatRaceTrack.RespawnRegion respawn;

        // this.sparticipants is sequenced by starting grid positions
        int gridBox = 0;
        for (BoatRacePlayer participant : this.participants) {
            if (participant.equals(bPlayer)) {
                break;
            }

            gridBox++;
        }

        // not enough grid boxes, spawn at pit entry
        if (gridBox > regions.gridBoxes().size() - 1) {
            respawn = regions.pitEntry();
        } else {
            respawn = regions.gridBoxes().get(gridBox);
        }

        this.spawnLogic.spawnPlayer(player, respawn);
        this.spawnLogic.spawnVehicleAndRide(player).orElseThrow();

        if (this.splits.getTimer(bPlayer) == 0l) {
            this.splits.run(bPlayer);
        }

        if (this.countdown.getCountdown() > 0) {
            this.spawnLogic.freezeVehicle(player);
        }
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
            inventory.setStack(8, BoatRaceItems.CYCLE_LEADERBOARD.getDefaultStack());
            inventory.setStack(7, BoatRaceItems.RESPAWN.getDefaultStack());
        }
    }

    /**
     * Despawn a player from the game.
     *
     * @param player The player.
     */
    public void despawnPlayer(ServerPlayerEntity player) {
        // do nothing
    }

    /**
     * Tick the game.
     */
    public void tickPlayers() {
        // check if countdown is ready
        switch (this.countdown.tick(this.world)) {
            case FINISH: {
                for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                    BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

                    if (!this.participants.contains(bPlayer)) {
                        continue;
                    }

                    this.spawnLogic.unfreezeVehicle(player);
                }
                break;
            }

            case COUNTDOWN: {
                return;
            }

            case IDLE: {
                break;
            }
        }

        this.duration += this.world.getTickManager().getMillisPerTick();

        if (this.duration > this.config.maxDuration()) {
            return;
        }

        if (this.laps.getLeadingLaps() > this.config.maxLaps()) {
            return;
        }

        this.splits.tick(this.world);

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            if (!this.participants.contains(bPlayer)) {
                continue;
            }

            switch (this.checkpoints.tick(player)) {
                case BEGIN: {
                    this.splits.recordSplit(bPlayer);
                    this.laps.submit(bPlayer, this.splits.getSplits(bPlayer));
                    break;
                }

                case LOOP: {
                    this.splits.recordSplit(bPlayer);
                    this.laps.submit(bPlayer, this.splits.getSplits(bPlayer));
                    break;
                }

                case FINISH: {
                    // stop the timer
                    // this.splits.stop(bPlayer);
                    break;
                }

                case CHECKPOINT: {
                    this.splits.recordSplit(bPlayer);
                    this.laps.submit(bPlayer, this.splits.getSplits(bPlayer));
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
        this.laps.submit(player, this.splits.getSplits(player));
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
     * Get the time left in ms
     *
     * @return The time left.
     */
    public long getDurationTimer() {
        return Math.max(0, this.config.maxDuration() - this.duration);
    }

    /**
     * Get game config for race.
     *
     * @return The loaded race config.
     */
    public BoatRaceConfig.Race getConfig() {
        return this.config;
    }
}
