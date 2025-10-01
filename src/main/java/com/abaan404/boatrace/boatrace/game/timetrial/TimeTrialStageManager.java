package com.abaan404.boatrace.boatrace.game.timetrial;

import java.util.List;

import com.abaan404.boatrace.boatrace.BoatRace;
import com.abaan404.boatrace.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.boatrace.game.gameplay.SplitsManager;
import com.abaan404.boatrace.boatrace.game.maps.TrackMap;
import com.abaan404.boatrace.boatrace.game.timetrial.TimeTrialLeaderboard.PersonalBest;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

/**
 * Handles time trial state.
 */
public class TimeTrialStageManager {
    private final GameSpace gameSpace;

    private final TrackMap track;
    private final ServerWorld world;

    private final TimeTrialSpawnLogic spawnLogic;

    private final CheckpointsManager checkpoints;
    private final SplitsManager splits;

    public TimeTrialStageManager(GameSpace gameSpace, ServerWorld world, TrackMap track) {
        this.gameSpace = gameSpace;
        this.track = track;
        this.world = world;

        this.spawnLogic = new TimeTrialSpawnLogic(world);

        this.checkpoints = new CheckpointsManager(track);
        this.splits = new SplitsManager();
    }

    public TickResult tick() {
        ServerWorld overworld = this.gameSpace.getServer().getWorld(World.OVERWORLD);

        TimeTrialLeaderboard leaderboard = overworld.getAttachedOrCreate(BoatRace.LEADERBOARD_ATTACHMENT);

        this.gameSpace.getPlayers().participants()
                .forEach(participant -> {
                    PlayerRef ref = PlayerRef.of(participant);

                    CheckpointsManager.TickResult result = this.checkpoints.tick(participant);
                    switch (result) {
                        case FINISH: {
                            PersonalBest pb = leaderboard.getPersonalBest(this.track, ref.id());
                            float currentTimer = this.splits.getTimer(ref);

                            if (currentTimer < pb.timer() || Float.isNaN(pb.timer())) {
                                overworld.setAttached(BoatRace.LEADERBOARD_ATTACHMENT, leaderboard.setPersonalBest(
                                        this.track,
                                        new PersonalBest(
                                                participant.getNameForScoreboard(),
                                                participant.getUuid(),
                                                currentTimer,
                                                this.getSplits(ref))));
                            }

                            this.splits.reset(ref);
                            break;
                        }

                        case CHECKPOINT: {
                            this.splits.tick(ref, this.world.getTickManager());
                            this.splits.recordSplit(ref);
                            break;
                        }

                        case IDLE: {
                            this.splits.tick(ref, this.world.getTickManager());
                            break;
                        }
                    }
                });

        return TickResult.IDLE;
    }

    public void spawnPlayer(ServerPlayerEntity player) {
        BlockBounds checkpoint = this.checkpoints.getCheckpoint(PlayerRef.of(player));

        this.spawnLogic.resetPlayer(player);
        this.spawnLogic.spawnPlayer(player, checkpoint);
    }

    public int getLaps(PlayerRef player) {
        return this.checkpoints.getLaps(player);
    }

    public int getCheckpointIndex(PlayerRef player) {
        return this.checkpoints.getCheckpointIndex(player);
    }

    public float getTimer(PlayerRef player) {
        return this.splits.getTimer(player);
    }

    public List<Float> getSplits(PlayerRef player) {
        return this.splits.getSplits(player);
    }

    public enum TickResult {
        IDLE,
    }
}
