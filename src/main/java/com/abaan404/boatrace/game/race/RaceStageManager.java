package com.abaan404.boatrace.game.race;

import java.util.Collections;
import java.util.List;
import java.util.SequencedSet;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.game.BoatRaceItems;
import com.abaan404.boatrace.game.BoatRaceSpawnLogic;
import com.abaan404.boatrace.game.gameplay.CheckpointsManager;
import com.abaan404.boatrace.game.gameplay.CountdownManager;
import com.abaan404.boatrace.game.gameplay.PositionsManager;
import com.abaan404.boatrace.game.gameplay.SplitsManager;
import com.abaan404.boatrace.utils.TextUtil;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;

public class RaceStageManager {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final BoatRaceConfig.Race config;
    private final BoatRaceTrack track;

    public final CheckpointsManager checkpoints;
    public final SplitsManager splits;
    public final PositionsManager positions;
    public final CountdownManager countdown;

    private final BoatRaceSpawnLogic spawnLogic;
    private final SequencedSet<BoatRacePlayer> participants;

    private long duration;

    public RaceStageManager(GameSpace gameSpace, BoatRaceConfig.Race config, ServerWorld world,
            BoatRaceTrack track, List<BoatRacePlayer> gridOrder) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;
        this.track = track;

        this.checkpoints = new CheckpointsManager(track);
        this.splits = new SplitsManager();
        this.positions = new PositionsManager(this.splits);

        Random random = world.getRandom();
        this.countdown = new CountdownManager(config.countdown(), random.nextBetween(0, config.countdownRandom()));

        this.spawnLogic = new BoatRaceSpawnLogic(world);
        this.participants = new ObjectLinkedOpenHashSet<>();

        List<BoatRacePlayer> gridOrderMutable = new ObjectArrayList<>(gridOrder);

        switch (this.config.gridType()) {
            case NORMAL: {
                break;
            }

            case RANDOM: {
                Collections.shuffle(gridOrderMutable);
                break;
            }

            case REVERSED: {
                Collections.reverse(gridOrderMutable);
                break;
            }
        }

        for (BoatRacePlayer player : gridOrderMutable) {
            this.toParticipant(player);
        }

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
                    this.splits.run(bPlayer);
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

        // max time reached or every player finished their laps
        if (this.duration > this.config.maxDuration() || this.participants.size() == 0) {
            this.endGame();
        }

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            if (!this.participants.contains(bPlayer)) {
                continue;
            }

            switch (this.checkpoints.tick(player)) {
                case CHECKPOINT: {
                    this.splits.recordSplit(bPlayer);
                    this.positions.update(bPlayer);
                    break;
                }

                case BEGIN:
                case FINISH:
                case LOOP: {
                    this.splits.recordSplit(bPlayer);
                    this.positions.update(bPlayer);

                    if (this.getLeadingLaps() > this.config.maxLaps()) {
                        this.participants.remove(bPlayer);
                        this.splits.stop(bPlayer);
                        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);

                        Entity boat = player.getVehicle();
                        if (boat != null) {
                            boat.kill(this.world);
                        }
                    }
                    break;
                }

                case IDLE: {
                    break;
                }
            }
        }

        this.splits.tick(this.world);
        this.duration += this.world.getTickManager().getMillisPerTick();
    }

    /**
     * Transition the player to a spectator.
     *
     * @param player The player's bPlayer
     */
    public void toSpectator(BoatRacePlayer player) {
        this.participants.remove(player);

        this.checkpoints.reset(player);
        this.splits.reset(player);
        this.splits.stop(player);
        this.positions.remove(player);
    }

    /**
     * Transition a player to a participant.
     *
     * @param player The player's bPlayer
     */
    public void toParticipant(BoatRacePlayer player) {
        this.participants.add(player);

        this.checkpoints.reset(player);
        this.splits.reset(player);
        this.splits.stop(player);
        this.positions.add(player);
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

    public int getLeadingLaps() {
        List<BoatRacePlayer> positions = this.positions.getPositions();

        if (positions.isEmpty()) {
            return -1;
        }

        return this.checkpoints.getLaps(positions.getFirst());
    }

    /**
     * Get game config for race.
     *
     * @return The loaded race config.
     */
    public BoatRaceConfig.Race getConfig() {
        return this.config;
    }

    private void endGame() {
        GameSpacePlayers players = this.gameSpace.getPlayers();
        List<BoatRacePlayer> positions = this.positions.getPositions();

        if (positions.size() == 0) {
            MutableText positionsText = Text.empty();
            positionsText.append(" ");
            positionsText.append(TextUtil.scoreboardPosition(true, 0)).append("  ");
            positionsText.append(TextUtil.scoreboardName(BoatRacePlayer.of(), false, 0)).append(" ");

            positionsText.append(Text.literal(" / ").formatted(Formatting.RED, Formatting.BOLD));

            positionsText.append(TextUtil.actionBarTimer(0));

            players.sendMessage(positionsText);
        }

        for (int i = 0; i < positions.size(); i++) {
            BoatRacePlayer player = positions.get(i);
            int laps = this.checkpoints.getLaps(player);

            MutableText positionsText = Text.empty();
            positionsText.append(" ");
            positionsText.append(TextUtil.scoreboardPosition(true, i)).append("  ");
            positionsText.append(TextUtil.scoreboardName(player, false, i)).append(" ");

            positionsText.append(Text.literal(" / ").formatted(Formatting.RED, Formatting.BOLD));

            if (laps < this.getLeadingLaps()) {
                positionsText.append(TextUtil.chatLapsDelta(this.getLeadingLaps(), this.checkpoints.getLaps(player)));
            } else {
                positionsText.append(TextUtil.actionBarTimer(this.splits.getTimer(player)));
            }

            players.sendMessage(positionsText);
        }

        this.gameSpace.close(GameCloseReason.FINISHED);
    }
}
