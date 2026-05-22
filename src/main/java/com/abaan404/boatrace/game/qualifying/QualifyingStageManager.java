package com.abaan404.boatrace.game.qualifying;

import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.NoteBlock;
import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRaceItems;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.game.race.Race;
import com.abaan404.boatrace.gameplay.Checkpoints;
import com.abaan404.boatrace.gameplay.SpawnLogic;
import com.abaan404.boatrace.gameplay.Splits;
import com.abaan404.boatrace.gameplay.Teams;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.utils.TextUtils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;

public class QualifyingStageManager {
    private final GameSpace gameSpace;
    private final ServerLevel world;
    private final BoatRaceConfig.Qualifying config;
    private final BoatRaceConfig.Race configRace;
    private final BoatRaceTrack track;

    public final Checkpoints checkpoints;
    public final Splits splits;
    public final Teams teams;

    private final SpawnLogic spawnLogic;
    private final Set<BoatRacePlayer> participants = new ObjectOpenHashSet<>();;

    private long duration = 0;

    public QualifyingStageManager(GameSpace gameSpace, BoatRaceConfig.Qualifying config, BoatRaceConfig.Race configRace,
            ServerLevel world, BoatRaceTrack track, Teams teams) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;
        this.configRace = configRace;
        this.track = track;
        this.teams = teams;

        this.checkpoints = new Checkpoints(track);
        this.splits = new Splits();

        this.spawnLogic = new SpawnLogic(world);
    }

    /**
     * Spawn a player on the track with a boat. Spectators spawn in minecraft's
     * spectator gamemode
     *
     * @param player The player.
     */
    public void spawnPlayer(ServerPlayer player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        BoatRaceTrack.Regions regions = this.track.getRegions();

        // spawn spectators at spawn without boats
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
     * Gives the players items to control their state on track.
     *
     * @param player The player
     */
    public void updatePlayerInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        inventory.clearContent();

        if (this.participants.contains(BoatRacePlayer.of(player))) {
            inventory.setItem(8, BoatRaceItems.RESET.getDefaultInstance());
        }
    }

    /**
     * Despawn a player from the game.
     *
     * @param player The player.
     */
    public void despawnPlayer(ServerPlayer player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        this.toSpectator(bPlayer);
        this.spawnLogic.despawnVehicle(player);

        Inventory inventory = player.getInventory();
        inventory.clearContent();

        Leaderboard leaderboard = this.world.getAttachedOrCreate(Leaderboard.ATTACHMENT);
        leaderboard.delete(this.world, this.track, bPlayer);
    }

    /**
     * Tick the player, update leaderboards and splits and also check the
     * duration/lap count of this game.
     */
    public void tickPlayers() {
        if (this.duration > this.config.duration()) {
            this.startRace();
            return;
        }

        // there is a rare chance tick() is called before any players are handled
        // workaround this issue by checking if duration has incremented
        if (this.duration > 0 && this.participants.isEmpty()) {
            this.startRace();
            return;
        }

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

                    this.config.laps().ifPresent(laps -> {
                        if (this.checkpoints.getLaps(bPlayer) > laps) {
                            this.toFinisher(player);
                        }
                    });

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

        this.duration += this.world.tickRateManager().millisecondsPerTick();
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
     * Transition a player to spectator but without resetting
     * their state.
     *
     * @param player The player.
     */
    public void toFinisher(ServerPlayer player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        this.participants.remove(bPlayer);

        this.splits.stop(bPlayer);
        this.splits.reset(bPlayer);
        this.spawnLogic.resetPlayer(player, GameType.SPECTATOR);
        this.spawnLogic.despawnVehicle(player);
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
     * Get the time left in ms
     *
     * @return The time left.
     */
    public long getDurationTimer() {
        return Math.max(0, this.config.duration() - this.duration);
    }

    /**
     * Get game config for qualifying.
     *
     * @return The loaded qualifying config.
     */
    public BoatRaceConfig.Qualifying getConfig() {
        return this.config;
    }

    /**
     * Begin the race.
     */
    private void startRace() {
        Leaderboard leaderboard = this.world.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        List<BoatRacePlayer> records = new ObjectArrayList<>();
        for (PersonalBest pb : leaderboard.getLeaderboard(this.track)) {
            records.add(pb.player());
        }

        this.gameSpace.setActivity(game -> {
            Teams teams = new Teams(this.teams, TeamManager.addTo(game));
            Race.open(game, this.configRace, this.world, this.track, teams, records);
        });
    }

    /**
     * Submit a leaderboard time.
     *
     * @param player The player to create a new pb for.
     */
    private void submit(ServerPlayer player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        Leaderboard leaderboard = this.world.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        PersonalBest pb = new PersonalBest(bPlayer, this.splits.getSplits(bPlayer));
        Leaderboard newLeaderboard = leaderboard.trySubmit(this.world, this.track, pb);

        if (newLeaderboard != leaderboard) {
            int position = newLeaderboard.getLeaderboardPosition(this.track, bPlayer);
            GameSpacePlayers players = this.gameSpace.getPlayers();

            players.sendMessage(TextUtils.chatNewPersonalBest(pb, position));
            player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0f, NoteBlock.getPitchFromNote(18));
        } else {
            player.sendSystemMessage(TextUtils.chatNewTime(pb.timer()));
        }
    }
}
