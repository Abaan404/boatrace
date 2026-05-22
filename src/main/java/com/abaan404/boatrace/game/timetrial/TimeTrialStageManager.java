package com.abaan404.boatrace.game.timetrial;

import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
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
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;

/**
 * Handles time trial state.
 */
public class TimeTrialStageManager {
    private final GameSpace gameSpace;
    private final ServerLevel world;
    private final BoatRaceTrack track;

    public final Checkpoints checkpoints;
    public final Splits splits;

    private final SpawnLogic spawnLogic;
    private final Set<BoatRacePlayer> participants = new ObjectOpenHashSet<>();

    public TimeTrialStageManager(GameSpace gameSpace, ServerLevel world, BoatRaceTrack track) {
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
    public void spawnPlayer(ServerPlayer player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        BoatRaceTrack.Regions regions = this.track.getRegions();

        if (!this.participants.contains(bPlayer)) {
            this.spawnLogic.resetPlayer(player, GameType.SPECTATOR);
            this.spawnLogic.spawnPlayer(player, regions.spawn());
            return;
        }

        this.spawnLogic.resetPlayer(player, GameType.ADVENTURE);
        this.spawnLogic.spawnPlayer(player, regions.spawn());
        this.spawnLogic.spawnVehicleAndRide(player).orElseThrow();
    }

    /**
     * Respawn a player to their last checkpoint.
     *
     * @param player The player.
     */
    public void respawnPlayer(ServerPlayer player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        BoatRaceTrack.Regions regions = this.track.getRegions();

        if (!this.participants.contains(bPlayer)) {
            return;
        }

        this.spawnLogic.resetPlayer(player, GameType.ADVENTURE);
        this.spawnLogic.spawnPlayer(player, this.checkpoints.getCheckpoint(bPlayer).orElse(regions.spawn()));
        this.spawnLogic.spawnVehicleAndRide(player).orElseThrow();
    }

    /**
     * Gives the players items to control their state on track.
     *
     * @param player The player
     */
    public void updatePlayerInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        inventory.clearContent();

        if (this.participants.contains(BoatRacePlayer.of(player))) {
            inventory.setItem(8, BoatRaceItems.RESET.getDefaultInstance());
            inventory.setItem(7, BoatRaceItems.RESPAWN.getDefaultInstance());
        } else {
            inventory.setItem(8, BoatRaceItems.RESET.getDefaultInstance());
        }
    }

    /**
     * Despawn a player from the game.
     *
     * @param player The player.
     */
    public void despawnPlayer(ServerPlayer player) {
        this.toSpectator(BoatRacePlayer.of(player));

        Inventory inventory = player.getInventory();
        inventory.clearContent();
    }

    /**
     * Tick the player and act on events from checkpoints and/or splits.
     */
    public void tickPlayers() {
        for (ServerPlayer player : this.gameSpace.getPlayers()) {
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
                    Tuple<Component, Component> titles = TextUtils.titleAlertCheckpoint();
                    player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 20));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(titles.getB()));
                    player.connection.send(new ClientboundSetTitleTextPacket(titles.getA()));
                    break;
                }

                case PIT_ENTER:
                case PIT_EXIT:
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
    private void submit(ServerPlayer player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        // use the overworld for persistent storage
        ServerLevel overworld = this.gameSpace.getServer().getLevel(Level.OVERWORLD);
        Leaderboard leaderboard = overworld.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        PersonalBest pb = new PersonalBest(bPlayer, this.splits.getSplits(bPlayer));
        Leaderboard newLeaderboard = leaderboard.trySubmit(overworld, this.track, pb);

        if (newLeaderboard != leaderboard) {
            int position = newLeaderboard.getLeaderboardPosition(this.track, bPlayer);
            GameSpacePlayers players = this.gameSpace.getPlayers();

            players.sendMessage(TextUtils.chatNewPersonalBest(pb, position));
            PlayerUtil.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.UI, 1.0f,
                    NoteBlock.getPitchFromNote(18));
        } else {
            player.sendSystemMessage(TextUtils.chatNewTime(pb.timer()));
        }
    }
}
