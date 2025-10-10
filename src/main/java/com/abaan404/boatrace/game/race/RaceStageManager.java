package com.abaan404.boatrace.game.race;

import java.util.List;
import java.util.SequencedSet;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.game.BoatRaceSpawnLogic;
import com.abaan404.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.game.gameplay.SplitsManager;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.maps.TrackMap;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameSpace;

public class RaceStageManager {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final BoatRaceConfig config;
    private final TrackMap track;

    public final CheckpointsManager checkpoints;
    public final SplitsManager splits;

    private final BoatRaceSpawnLogic spawnLogic;
    private final SequencedSet<BoatRacePlayer> participants;

    private final BoatRaceConfig.Race raceConfig;
    private long duration;

    public RaceStageManager(GameSpace gameSpace, BoatRaceConfig config, ServerWorld world,
            TrackMap track, List<PersonalBest> qualifyingResults) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;
        this.track = track;

        this.checkpoints = new CheckpointsManager(track);
        this.splits = new SplitsManager();

        this.spawnLogic = new BoatRaceSpawnLogic(world);
        this.participants = new ObjectLinkedOpenHashSet<>();

        this.raceConfig = config.race().orElseThrow();
        this.duration = 0;

        for (PersonalBest pb : qualifyingResults) {
            this.toParticipant(pb.player());
        }
    }

    /**
     * Spawn a player on the track with a boat. Spectators spawn in minecraft's
     * spectator gamemode
     *
     * @param player The player.
     */
    public void spawnPlayer(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        TrackMap.Regions regions = this.track.getRegions();

        // spawn spectators or non qualified at spawn without boats
        if (!this.participants.contains(bPlayer)) {
            this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
            this.spawnLogic.spawnPlayer(player, regions.checkpoints().getFirst());
            return;
        }

        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);

        TrackMap.RespawnRegion respawn;

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
    }

    /**
     * Gives the players items to control their state on track.
     *
     * @param player The player
     */
    public void updatePlayerInventory(ServerPlayerEntity player) {
        // TODO swap widget interval type (relative, absolute)
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
        RacePosition position = this.world.getAttachedOrCreate(RacePosition.ATTACHMENT);

        // update scores every 2 seconds
        long time = this.world.getTime();
        if (time % 2 * (this.world.getTickManager().getTickRate()) == 0) {
            position = position.update(this.world, this.checkpoints, this.participants.stream()
                    .toList());
        }

        this.duration += this.world.getTickManager().getMillisPerTick();

        if (this.duration > this.raceConfig.maxDuration()) {
            return;
        }

        if (position.positions().isEmpty()
                || this.checkpoints.getLaps(position.positions().getFirst()) > this.raceConfig.maxLaps()) {
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
                    this.splits.run(bPlayer);
                    this.splits.recordSplit(bPlayer);
                    break;
                }

                case LOOP: {
                    // start a new run
                    this.splits.reset(bPlayer);
                    this.splits.recordSplit(bPlayer);
                    break;
                }

                case FINISH: {
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
    }

    /**
     * Transition the player to a spectator. They can roam freely and explore the
     * track.
     *
     * @param player The player's bPlayer
     */
    public void toSpectator(BoatRacePlayer player) {
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
        if (!this.participants.contains(player)) {
            // this game began with a qualifying session, spawn as spectator.
            if (this.config.qualifying().isPresent()) {
                this.toSpectator(player);
                return;
            }

            // this game was started without qualifying, add positions
            // TODO grid layout (normal, reversed, random)
            else {
                this.participants.add(player);
            }
        }

        this.checkpoints.reset(player);
        this.splits.reset(player);
        this.splits.stop(player);

        // update positions
        RacePosition positions = this.world.getAttachedOrCreate(RacePosition.ATTACHMENT);
        positions.update(this.world, this.checkpoints, this.participants.stream()
                .toList());
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
    public long getTimeLeft() {
        return Math.min(0, this.raceConfig.maxDuration() - this.duration);
    }
}
