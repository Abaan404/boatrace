package com.abaan404.boatrace.game.qualifying;

import java.util.List;
import java.util.Set;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.game.BoatRaceSpawnLogic;
import com.abaan404.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.game.gameplay.SplitsManager;
import com.abaan404.boatrace.game.race.Race;
import com.abaan404.boatrace.items.BoatRaceItems;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.utils.TextUtil;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameSpace;

public class QualifyingStageManager {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final BoatRaceConfig config;
    private final BoatRaceTrack track;

    public final CheckpointsManager checkpoints;
    public final SplitsManager splits;

    private final BoatRaceSpawnLogic spawnLogic;
    private final Set<BoatRacePlayer> participants;

    private final BoatRaceConfig.Qualifying qualifyingConfig;

    private long duration;

    public QualifyingStageManager(GameSpace gameSpace, BoatRaceConfig config, ServerWorld world,
            BoatRaceTrack track) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;
        this.track = track;

        this.checkpoints = new CheckpointsManager(track);
        this.splits = new SplitsManager();

        this.spawnLogic = new BoatRaceSpawnLogic(world);
        this.participants = new ObjectOpenHashSet<>();

        this.qualifyingConfig = config.qualifying().orElseThrow();
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

        // spawn spectators at spawn without boats
        if (!this.participants.contains(bPlayer)) {
            this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
            this.spawnLogic.spawnPlayer(player, regions.checkpoints().getFirst());
            return;
        }

        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        BoatRaceTrack.RespawnRegion respawn = regions.checkpoints().getFirst();

        switch (this.config.qualifying().get().startFrom()) {
            case GRID_BOX: {
                if (!regions.gridBoxes().isEmpty()) {
                    respawn = regions.gridBoxes().getLast();
                }
                break;
            }

            case PIT_BOX: {
                if (!regions.pitBoxes().isEmpty()) {
                    respawn = regions.pitBoxes().getLast();
                }
                break;
            }

            case SPAWN: {
                if (!regions.spawn().equals(BoatRaceTrack.RespawnRegion.of())) {
                    respawn = regions.spawn();
                }
            }
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
        PlayerInventory inventory = player.getInventory();
        inventory.clear();

        inventory.setStack(8, BoatRaceItems.RESET.getDefaultStack());
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
     * Tick the player, update leaderboards and splits and also check the duration
     * of this game.
     */
    public void tickPlayers() {
        this.duration += this.world.getTickManager().getMillisPerTick();

        if (this.duration > this.qualifyingConfig.duration()) {
            this.startRace();
            return;
        }

        Leaderboard leaderboard = this.world.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        this.splits.tick(this.world);

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            if (!this.participants.contains(bPlayer)) {
                continue;
            }

            Leaderboard newLeaderboard = leaderboard;
            switch (this.checkpoints.tick(player)) {
                case BEGIN: {
                    this.splits.run(bPlayer);
                    this.splits.recordSplit(bPlayer);
                    break;
                }

                case LOOP: {
                    this.splits.recordSplit(bPlayer);
                    newLeaderboard = leaderboard.trySubmit(this.world, this.track, new PersonalBest(
                            BoatRacePlayer.of(player),
                            this.splits.getSplits(bPlayer)));

                    // start a new run
                    this.splits.reset(bPlayer);
                    this.splits.recordSplit(bPlayer);
                    break;
                }

                case FINISH: {
                    this.splits.recordSplit(bPlayer);
                    newLeaderboard = leaderboard.trySubmit(this.world, this.track, new PersonalBest(
                            BoatRacePlayer.of(player),
                            this.splits.getSplits(bPlayer)));

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

            // leaderboard was updated, new pb
            if (newLeaderboard != leaderboard) {
                PersonalBest pb = newLeaderboard.getPersonalBest(this.track, bPlayer);
                int position = newLeaderboard.getLeaderboardPosition(this.track, bPlayer);

                player.sendMessage(TextUtil.chatNewPersonalBest(pb.timer(), position));
                leaderboard = newLeaderboard;
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
        // TODO teams
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
     * Get the time left in ms
     *
     * @return The time left.
     */
    public long getDurationTimer() {
        return Math.max(0, this.qualifyingConfig.duration() - this.duration);
    }

    /**
     * Get game config for qualifying.
     *
     * @return The loaded qualifying config.
     */
    public BoatRaceConfig.Qualifying getQualifyingConfig() {
        return qualifyingConfig;
    }

    private void startRace() {
        Leaderboard leaderboard = this.world.getAttachedOrCreate(Leaderboard.ATTACHMENT);
        List<PersonalBest> records = leaderboard.getLeaderboard(this.track);

        this.gameSpace.setActivity(game -> {
            Race.open(game, config, world, track, records);
        });
    }
}
