package com.abaan404.boatrace.game.qualifying;

import java.util.Map;

import com.abaan404.boatrace.game.BoatRaceConfig;
import com.abaan404.boatrace.game.BoatRaceSpawnLogic;
import com.abaan404.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.game.gameplay.SplitsManager;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.maps.TrackMap;

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
    private final Map<PlayerRef, QualifyingPlayer> participants;

    public final CheckpointsManager checkpoints;
    public final SplitsManager splits;

    private final BoatRaceSpawnLogic spawnLogic;

    private long timeLeft;

    public QualifyingStageManager(GameSpace gameSpace, BoatRaceConfig.Qualifying config, ServerWorld world,
            TrackMap track, Map<PlayerRef, QualifyingPlayer> participants) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;
        this.track = track;
        this.participants = participants;

        this.checkpoints = new CheckpointsManager(track);
        this.splits = new SplitsManager();

        this.spawnLogic = new BoatRaceSpawnLogic(world);

        this.timeLeft = (long) (config.duration() * 1000.0f); // duration stored in seconds
    }

    /**
     * Spawn a player on the track, if they aren't a spectator, spawn them with a
     * boat.
     *
     * @param player The player.
     */
    public void spawnPlayer(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        TrackMap.Regions regions = this.track.getRegions();

        if (this.participants.containsKey(ref)) {
            spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
            this.spawnLogic.spawnPlayer(player, regions.finish());
            return;
        }

        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        TrackMap.RespawnRegion respawn = regions.finish();

        switch (this.config.startFrom()) {
            case FINISH: {
                respawn = regions.finish();
                break;
            }

            case PIT_BOX: {
                respawn = regions.pitBoxes().getLast();
                break;
            }
        }

        this.spawnLogic.spawnPlayer(player, respawn);
        this.spawnLogic.spawnVehicle(player).orElseThrow();
    }

    /**
     * Despawn (or untrack) a player from the game.
     *
     * @param player The player.
     */
    public void despawnPlayer(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        this.participants.remove(ref);
    }

    /**
     * Tick the player, update leaderboards and splits and also check the duration
     * of this game.
     */
    public TickResult tickPlayers() {
        // time ran out
        if (this.timeLeft < 0) {
            return TickResult.END;
        }

        Leaderboard leaderboard = this.world.getAttachedOrCreate(Leaderboard.ATTACHMENT);
        this.timeLeft -= this.world.getTickManager().getMillisPerTick();

        for (ServerPlayerEntity player : this.gameSpace.getPlayers().participants()) {
            PlayerRef ref = PlayerRef.of(player);

            if (!this.participants.containsKey(ref)) {
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
                        leaderboard = this.world.setAttached(Leaderboard.ATTACHMENT,
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

        return TickResult.IDLE;
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
