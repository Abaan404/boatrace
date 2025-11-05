package com.abaan404.boatrace.game.qualifying;

import java.util.List;
import java.util.Set;

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
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;

public class QualifyingStageManager {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final BoatRaceConfig.Qualifying config;
    private final BoatRaceConfig.Race configRace;
    private final BoatRaceTrack track;

    public final Checkpoints checkpoints;
    public final Splits splits;
    public final Teams teams;

    private final SpawnLogic spawnLogic;
    private final Set<BoatRacePlayer> participants;

    private long duration;

    public QualifyingStageManager(GameSpace gameSpace, BoatRaceConfig.Qualifying config, BoatRaceConfig.Race configRace,
            ServerWorld world, BoatRaceTrack track, Teams teams) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;
        this.configRace = configRace;
        this.track = track;
        this.teams = teams;

        this.checkpoints = new Checkpoints(track);
        this.splits = new Splits();

        this.spawnLogic = new SpawnLogic(world);
        this.participants = new ObjectOpenHashSet<>();

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

        if (this.participants.contains(BoatRacePlayer.of(player))) {
            inventory.setStack(8, BoatRaceItems.RESET.getDefaultStack());
        }
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
        if (this.duration > this.config.duration()) {
            this.startRace();
            return;
        }

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

                case IDLE: {
                    break;
                }
            }
        }

        this.duration += this.world.getTickManager().getMillisPerTick();
        this.splits.tick(this.world);
    }

    /**
     * Transition the player to a spectator. They can roam freely and explore the
     * track.
     *
     * @param player The player's bPlayer
     */
    public void toSpectator(BoatRacePlayer player) {
        this.teams.remove(player);
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
        this.teams.add(player);
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
    private void submit(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        Leaderboard leaderboard = this.world.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        PersonalBest pb = new PersonalBest(bPlayer, this.splits.getSplits(bPlayer));
        Leaderboard newLeaderboard = leaderboard.trySubmit(this.world, this.track, pb);

        if (newLeaderboard != leaderboard) {
            int position = newLeaderboard.getLeaderboardPosition(this.track, bPlayer);
            GameSpacePlayers players = this.gameSpace.getPlayers();

            players.sendMessage(TextUtils.chatNewPersonalBest(pb, position));
        } else {
            player.sendMessage(TextUtils.chatNewTime(pb.timer()));
        }
    }
}
