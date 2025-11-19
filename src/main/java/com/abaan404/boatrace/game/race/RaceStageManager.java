package com.abaan404.boatrace.game.race;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRaceItems;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.gameplay.Checkpoints;
import com.abaan404.boatrace.gameplay.Countdown;
import com.abaan404.boatrace.gameplay.PitStops;
import com.abaan404.boatrace.gameplay.Positions;
import com.abaan404.boatrace.gameplay.SpawnLogic;
import com.abaan404.boatrace.gameplay.Splits;
import com.abaan404.boatrace.gameplay.Teams;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.utils.TextUtils;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;

public class RaceStageManager {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final BoatRaceConfig.Race config;
    private final BoatRaceTrack track;

    public final PitStops pits;
    public final Checkpoints checkpoints;
    public final Splits splits;
    public final Positions positions;
    public final Countdown countdown;
    public final Teams teams;

    private final SpawnLogic spawnLogic;
    private final SequencedSet<BoatRacePlayer> participants = new ObjectLinkedOpenHashSet<>();

    private PersonalBest fastestLap = PersonalBest.DEFAULT;
    private long duration = 0;

    public RaceStageManager(GameSpace gameSpace, BoatRaceConfig.Race config, ServerWorld world, BoatRaceTrack track,
            Teams teams) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;
        this.track = track;
        this.teams = teams;

        this.pits = new PitStops();
        this.checkpoints = new Checkpoints(track);
        this.splits = new Splits();
        this.positions = new Positions();

        this.countdown = new Countdown();
        this.countdown.setCountdown(config.countdown(), config.countdownRandom());

        this.spawnLogic = new SpawnLogic(world);
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
            this.spawnLogic.spawnPlayer(player, regions.spawn());
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

        if (this.checkpoints.getCheckpointIndex(bPlayer) > 0) {
            respawn = this.checkpoints.getCheckpoint(bPlayer);
        } else if (gridBox <= regions.gridBoxes().size() - 1) {
            respawn = regions.gridBoxes().get(gridBox);
        } else {
            // not enough grid boxes
            respawn = regions.spawn();
        }

        this.spawnLogic.spawnPlayer(player, respawn);
        this.spawnLogic.spawnVehicleAndRide(player).orElseThrow();

        if (this.countdown.isCounting()) {
            this.spawnLogic.freezeVehicle(player);
        } else {
            this.spawnLogic.unfreezeVehicle(player);
        }
    }

    /**
     * Respawn a player to their last checkpoint.
     *
     * @param player The player.
     */
    public void respawnPlayer(ServerPlayerEntity player) {
        if (this.config.noRespawn()) {
            return;
        }

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

            if (!this.config.noRespawn()) {
                inventory.setStack(7, BoatRaceItems.RESPAWN.getDefaultStack());
            }
        }
    }

    /**
     * Despawn a player from the game.
     *
     * @param player The player.
     */
    public void despawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.despawnVehicle(player);
    }

    /**
     * Tick the game.
     */
    public void tickPlayers() {
        // check if countdown is ready
        switch (this.countdown.tick(this.world)) {
            case FINISH: {
                for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                    if (!this.participants.contains(BoatRacePlayer.of(player))) {
                        continue;
                    }

                    this.spawnLogic.unfreezeVehicle(player);
                }

                // start positions timer for non server players
                for (BoatRacePlayer bPlayer : this.participants) {
                    this.positions.run(bPlayer);
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

        boolean allDisconnected = true;
        for (BoatRacePlayer player : this.participants) {
            allDisconnected &= !player.ref().isOnline(this.gameSpace);
        }

        // max time reached or every player finished their laps or every participant has
        // disconnected
        if (this.duration > this.config.maxDuration() || this.participants.isEmpty() || allDisconnected) {
            this.endGame();
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
                    this.positions.update(bPlayer);
                    break;
                }

                case LOOP: {
                    this.positions.update(bPlayer);
                    this.splits.recordSplit(bPlayer);
                    this.submit(player);

                    // start a new lap time
                    this.splits.reset(bPlayer);
                    this.splits.recordSplit(bPlayer);

                    if (this.getLeadingLaps() > this.getMaxLaps()) {
                        this.toFinisher(player);
                    }
                    break;
                }

                case FINISH: {
                    this.toFinisher(player);
                    break;
                }

                case MISSED: {
                    Pair<Text, Text> titles = TextUtils.titleAlertCheckpoint();
                    player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 30, 20));
                    player.networkHandler.sendPacket(new SubtitleS2CPacket(titles.getRight()));
                    player.networkHandler.sendPacket(new TitleS2CPacket(titles.getLeft()));
                    break;
                }

                case CHECKPOINT: {
                    this.splits.recordSplit(bPlayer);
                    this.positions.update(bPlayer);
                    break;
                }

                case PIT_ENTER: {
                    if (this.pits.getPitCount(bPlayer) < this.config.maxPits()) {
                        this.pits.startPit(player);
                    }
                    break;
                }

                case PIT_EXIT: {
                    this.pits.stopPit(player);
                    break;
                }

                case IDLE: {
                    break;
                }
            }
        }

        this.positions.tick(this.world);
        this.splits.tick(this.world);
        this.duration += this.world.getTickManager().getMillisPerTick();
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

        this.pits.reset(player);
        this.checkpoints.reset(player);
        this.splits.reset(player);
        this.splits.stop(player);
        this.positions.remove(player);
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

        this.pits.reset(player);
        this.checkpoints.reset(player);
        this.splits.reset(player);
        this.splits.stop(player);
        this.positions.add(player);
    }

    public void toFinisher(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        if (this.positions.getPosition(bPlayer) == 0) {
            for (ServerPlayerEntity player2 : this.gameSpace.getPlayers()) {
                BoatRacePlayer bPlayer2 = BoatRacePlayer.of(player2);
                if (this.checkpoints.getLaps(bPlayer2) >= this.config.maxLaps()) {
                    continue;
                }

                player2.sendMessage(TextUtils.chatFinalLap());
            }
        }

        this.participants.remove(bPlayer);
        this.splits.stop(bPlayer);
        this.positions.stop(bPlayer);
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
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
     * Get the max laps for this race.
     *
     * @return The number of laps to finish.
     */
    public int getMaxLaps() {
        return switch (this.track.getAttributes().layout()) {
            case CIRCULAR -> this.config.maxLaps();
            case LINEAR -> 1;
        };
    }

    /**
     * Get game config for race.
     *
     * @return The loaded race config.
     */
    public BoatRaceConfig.Race getConfig() {
        return this.config;
    }

    /**
     * Submit a leaderboard time.
     *
     * @param player The player to create a new pb for.
     */
    private void submit(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        PersonalBest pb = new PersonalBest(bPlayer, this.splits.getSplits(bPlayer));

        if (Leaderboard.validate(this.track, pb) && pb.timer() < this.fastestLap.timer()) {
            this.fastestLap = pb;

            GameSpacePlayers players = this.gameSpace.getPlayers();
            players.sendMessage(TextUtils.chatNewFastestLap(pb));
        } else {
            player.sendMessage(TextUtils.chatNewTime(pb.timer()));
        }

    }

    /**
     * End the game.
     */
    private void endGame() {
        GameSpacePlayers players = this.gameSpace.getPlayers();
        List<BoatRacePlayer> positions = this.positions.getPositions();

        if (positions.size() == 0) {
            int points = this.config.scoring().isEmpty() ? 1 : this.config.scoring().getFirst();

            MutableText positionsText = Text.empty();
            positionsText.append(" ");
            positionsText.append(TextUtils.scoreboardPosition(true, 0)).append(" ");
            positionsText.append(TextUtils.scoreboardName(BoatRacePlayer.DEFAULT, GameTeamConfig.DEFAULT, false, 0))
                    .append(" ");

            positionsText.append(Text.literal("/").formatted(Formatting.RED, Formatting.BOLD));

            positionsText.append(TextUtils.actionBarTimer(0)).append("  ");
            positionsText.append(TextUtils.chatPoints(points));

            players.sendMessage(positionsText);
        }

        Map<GameTeamKey, Integer> teamPoints = new Object2IntOpenHashMap<>();

        for (int i = 0; i < positions.size(); i++) {
            BoatRacePlayer player = positions.get(i);
            GameTeamKey team = this.teams.getTeamFor(player);

            int laps = this.checkpoints.getLaps(player);
            int points = i < this.config.scoring().size() ? this.config.scoring().get(i) : 0;
            teamPoints.put(team, teamPoints.getOrDefault(team, 0) + points);

            MutableText positionsText = Text.empty();
            positionsText.append(" ");
            positionsText.append(TextUtils.scoreboardPosition(true, i)).append(" ");
            positionsText.append(TextUtils.scoreboardName(player, this.teams.getConfig(team), false, i)).append(" ");
            positionsText.append(Text.literal("/").formatted(Formatting.RED, Formatting.BOLD)).append(" ");

            if (laps < this.getLeadingLaps()) {
                positionsText.append(TextUtils.chatLapsDelta(this.getLeadingLaps(), this.checkpoints.getLaps(player)))
                        .append("  ");
            } else {
                positionsText.append(TextUtils.actionBarTimer(this.positions.getTimer(player))).append("  ");
            }

            positionsText.append(TextUtils.chatPoints(points));

            players.sendMessage(positionsText);
        }

        Optional<GameTeamKey> winner = Optional.empty();
        int winnerPoints = Integer.MIN_VALUE;
        int winnerBestMemberPosition = positions.size() - 1;

        for (Map.Entry<GameTeamKey, Integer> entry : teamPoints.entrySet()) {
            GameTeamKey teamKey = entry.getKey();
            int points = entry.getValue();

            int memberBestPosition = positions.size() - 1;
            Set<BoatRacePlayer> members = this.teams.getPlayersIn(teamKey, positions);
            for (BoatRacePlayer player : members) {
                int position = this.positions.getPosition(player);
                if (position < memberBestPosition) {
                    memberBestPosition = position;
                }
            }

            // tie break with leading member
            if (points > winnerPoints || (points == winnerPoints && memberBestPosition < winnerBestMemberPosition)) {
                winnerPoints = points;
                winnerBestMemberPosition = memberBestPosition;
                winner = Optional.of(teamKey);
            }
        }

        if (winner.isPresent()) {
            MutableText teamsText = Text.empty();

            List<String> playerNames = new ObjectArrayList<>();
            for (BoatRacePlayer winners : this.teams.getPlayersIn(winner.get(), positions)) {
                playerNames.add(winners.offlineName());
            }

            teamsText.append(" ");
            teamsText.append(String.join(", ", playerNames));
            teamsText.append(String.format(" won the game with %d point(s).", winnerPoints));

            players.sendMessage(Text.empty());
            players.sendMessage(teamsText.formatted(Formatting.GOLD));
        }

        this.gameSpace.close(GameCloseReason.FINISHED);
    }
}
