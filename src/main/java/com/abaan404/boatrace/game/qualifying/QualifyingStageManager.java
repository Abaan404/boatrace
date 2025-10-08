package com.abaan404.boatrace.game.qualifying;

import java.util.Map;

import com.abaan404.boatrace.game.BoatRaceConfig;
import com.abaan404.boatrace.game.BoatRaceSpawnLogic;
import com.abaan404.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.game.gameplay.SplitsManager;
import com.abaan404.boatrace.items.BoatRaceItems;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.maps.TrackMap;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class QualifyingStageManager {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final BoatRaceConfig.Qualifying config;
    private final TrackMap track;

    public final CheckpointsManager checkpoints;
    public final SplitsManager splits;

    private final BoatRaceSpawnLogic spawnLogic;
    private final Map<PlayerRef, QualifyingPlayer> participants;

    private long timeLeft;

    public QualifyingStageManager(GameSpace gameSpace, BoatRaceConfig.Qualifying config, ServerWorld world,
            TrackMap track) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;
        this.track = track;

        this.checkpoints = new CheckpointsManager(track);
        this.splits = new SplitsManager();

        this.spawnLogic = new BoatRaceSpawnLogic(world);
        this.participants = new Object2ObjectOpenHashMap<>();

        this.timeLeft = (long) (config.duration() * 1000.0f); // duration stored in seconds
    }

    /**
     * Spawn a player on the track with a boat. Spectators spawn in minecraft's
     * spectator gamemode
     *
     * @param player The player.
     */
    public void spawnPlayer(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        TrackMap.Regions regions = this.track.getRegions();

        // spawn spectators at spawn without boats
        if (!this.participants.containsKey(ref)) {
            this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
            this.spawnLogic.spawnPlayer(player, regions.checkpoints().getFirst());
            return;
        }

        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        TrackMap.RespawnRegion respawn = regions.checkpoints().getFirst();

        switch (this.config.startFrom()) {
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
        PlayerRef ref = PlayerRef.of(player);
        this.participants.remove(ref);

        this.checkpoints.reset(ref);
        this.splits.reset(ref);
        this.splits.stop(ref);

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
    }

    /**
     * Tick the player, update leaderboards and splits and also check the duration
     * of this game.
     */
    public TickResult tickPlayers() {
        if (this.timeLeft <= 0) {
            return TickResult.END;
        }

        this.timeLeft -= this.world.getTickManager().getMillisPerTick();

        Leaderboard leaderboard = this.world.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        this.splits.tick(this.world);

        for (ServerPlayerEntity player : this.gameSpace.getPlayers().participants()) {
            PlayerRef ref = PlayerRef.of(player);

            if (!this.participants.containsKey(ref)) {
                continue;
            }

            switch (this.checkpoints.tick(player)) {
                case BEGIN: {
                    this.splits.run(ref);
                    this.splits.recordSplit(ref);
                    break;
                }

                case LOOP:
                case FINISH: {
                    leaderboard = leaderboard.trySubmit(this.world, this.track, new PersonalBest(
                            player.getNameForScoreboard(),
                            player.getUuid(),
                            this.splits.getTimer(ref),
                            this.splits.getSplits(ref)));

                    this.splits.reset(ref);
                    this.splits.recordSplit(ref);
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

        return TickResult.IDLE;
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
        // TODO teams
        this.participants.put(player, new QualifyingPlayer(0));

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
        return this.participants.containsKey(PlayerRef.of(player));
    }

    /**
     * Get the time left in ms
     *
     * @return The time left.
     */
    public long getTimeLeft() {
        return timeLeft;
    }

    public enum TickResult {
        /**
         * End qualifying, Proceed to race.
         */
        END,

        /**
         * Nothing happened.
         */
        IDLE,
    }
}
