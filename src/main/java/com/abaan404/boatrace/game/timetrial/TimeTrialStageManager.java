package com.abaan404.boatrace.game.timetrial;

import java.util.Set;

import com.abaan404.boatrace.BoatRaceItems;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.gameplay.Checkpoints;
import com.abaan404.boatrace.gameplay.SpawnLogic;
import com.abaan404.boatrace.gameplay.Splits;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.utils.TextUtils;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;

/**
 * Handles time trial state.
 */
public class TimeTrialStageManager {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final BoatRaceTrack track;

    public final Checkpoints checkpoints;
    public final Splits splits;

    private final SpawnLogic spawnLogic;
    private final Set<BoatRacePlayer> participants = new ObjectOpenHashSet<>();

    public TimeTrialStageManager(GameSpace gameSpace, ServerWorld world, BoatRaceTrack track) {
        this.gameSpace = gameSpace;
        this.track = track;
        this.world = world;

        this.checkpoints = new Checkpoints(track);
        this.splits = new Splits();

        this.spawnLogic = new SpawnLogic(world);
    }

    /**
     * Spawn a player on the track with a boat. Spectators spawn in free roam
     * 
     * @param player The player.
     */
    public void spawnPlayer(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        BoatRaceTrack.Regions regions = this.track.getRegions();
        BoatRaceTrack.Attributes attributes = this.track.getAttributes();

        if (!this.participants.contains(bPlayer)) {
            this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
            this.spawnLogic.spawnPlayer(player, regions.spawn());
            return;
        }

        BoatRaceTrack.RespawnRegion respawn = regions.checkpoints().getFirst();

        if (!regions.spawn().equals(BoatRaceTrack.RespawnRegion.DEFAULT)) {
            respawn = regions.spawn();
        } else if (!regions.gridBoxes().isEmpty()) {
            respawn = regions.gridBoxes().getFirst();
        } else if (!regions.checkpoints().isEmpty()) {
            switch (attributes.layout()) {
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
            inventory.setStack(8, BoatRaceItems.RESET.getDefaultStack());
            inventory.setStack(7, BoatRaceItems.RESPAWN.getDefaultStack());
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
        this.toSpectator(BoatRacePlayer.of(player));

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
    }

    /**
     * Tick the player and act on events from checkpoints and/or splits.
     */
    public void tickPlayers() {
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
                    this.splits.recordSplit(bPlayer);
                    this.submit(player);

                    // start a new run
                    this.splits.reset(bPlayer);
                    this.splits.recordSplit(bPlayer);
                    break;
                }

                case FINISH: {
                    this.splits.recordSplit(bPlayer);
                    this.submit(player);

                    // stop the timer
                    this.splits.stop(bPlayer);
                    break;
                }

                case CHECKPOINT: {
                    this.splits.recordSplit(bPlayer);
                    break;
                }

                case MISSED: {
                    Pair<Text, Text> titles = TextUtils.titleAlertCheckpoint();
                    player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 30, 20));
                    player.networkHandler.sendPacket(new SubtitleS2CPacket(titles.getRight()));
                    player.networkHandler.sendPacket(new TitleS2CPacket(titles.getLeft()));
                    break;
                }

                case IDLE: {
                    break;
                }
            }
        }

        this.splits.tick(this.world);
    }

    /**
     * Transition the player to a spectator.
     *
     * @param player The player.
     */
    public void toSpectator(BoatRacePlayer player) {
        if (!this.participants.contains(player)) {
            return;
        }

        this.participants.remove(player);

        this.checkpoints.reset(player);
        this.splits.reset(player);
        this.splits.stop(player);
    }

    /**
     * Transition a player to a participant.
     *
     * @param player The player.
     */
    public void toParticipant(BoatRacePlayer player) {
        if (this.participants.contains(player)) {
            return;
        }

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
    public boolean isParticipant(BoatRacePlayer player) {
        return this.participants.contains(player);
    }

    /**
     * Submit a leaderboard time.
     *
     * @param player The player to create a new pb for.
     */
    private void submit(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        // use the overworld for persistent storage
        ServerWorld overworld = this.gameSpace.getServer().getWorld(World.OVERWORLD);
        Leaderboard leaderboard = overworld.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        PersonalBest pb = new PersonalBest(bPlayer, this.splits.getSplits(bPlayer));
        Leaderboard newLeaderboard = leaderboard.trySubmit(overworld, this.track, pb);

        if (newLeaderboard != leaderboard) {
            int position = newLeaderboard.getLeaderboardPosition(this.track, bPlayer);
            GameSpacePlayers players = this.gameSpace.getPlayers();

            players.sendMessage(TextUtils.chatNewPersonalBest(pb, position));
        } else {
            player.sendMessage(TextUtils.chatNewTime(pb.timer()));
        }
    }
}
