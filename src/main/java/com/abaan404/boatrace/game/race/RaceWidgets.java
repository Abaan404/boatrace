package com.abaan404.boatrace.game.race;

import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.utils.TextUtils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

public class RaceWidgets {
    private final GameSpace gameSpace;
    private final GlobalWidgets widgets;
    private final BoatRaceTrack track;

    private static final int SIDEBAR_RANKING_COMPARED = 2;
    private static final int SIDEBAR_RANKING_TOP = 5;

    private final Map<BoatRacePlayer, LeaderboardType> leaderboardType = new Object2ObjectOpenHashMap<>();
    private final Map<BoatRacePlayer, SidebarWidget> sidebars = new Object2ObjectOpenHashMap<>();

    private boolean shownGo = false;

    public RaceWidgets(GameSpace gameSpace, GlobalWidgets widgets, BoatRaceTrack track) {
        this.gameSpace = gameSpace;
        this.track = track;
        this.widgets = widgets;
    }

    /**
     * Send track info through chat.
     *
     * @param player The player to send the message to.
     */
    public void sendTrackMessage(ServerPlayer player) {
        TextUtils.chatMeta(this.track.getMeta()).forEach(player::sendSystemMessage);
    }

    /**
     * Tick the UI for the player.
     *
     * @param stageManager The game's state.
     */
    public void tick(RaceStageManager stageManager) {
        this.tickActionBar(stageManager);
        this.tickSidebar(stageManager);
        this.tickTitle(stageManager);
    }

    /**
     * Display a countdown using a title.
     *
     * @param stageManager The stage manager.
     */
    private void tickTitle(RaceStageManager stageManager) {
        if (this.shownGo) {
            return;
        }

        long countdown = Math.ceilDiv(stageManager.goCountdown.getCountdown(), 1000);
        if (countdown <= 0) {
            this.shownGo = true;
        }

        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(TextUtils.titleCountdown(countdown)));
        }
    }

    /**
     * Display splits and timers on the client's action bar.
     *
     * @param stageManager The stage manager.
     */
    private void tickActionBar(RaceStageManager stageManager) {
        int maxCheckpoints = switch (this.track.getAttributes().layout()) {
            // dont count start
            case CIRCULAR -> this.track.getRegions().checkpoints().size() - 1;
            // dont count start and end
            case LINEAR -> this.track.getRegions().checkpoints().size() - 2;
        };

        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
            if (!stageManager.isParticipant(bPlayer)) {
                continue;
            }

            long timer = stageManager.splits.getTimer(bPlayer);
            int position = stageManager.positions.getPosition(bPlayer);
            int checkpoint = stageManager.checkpoints.getCheckpointIndex(bPlayer);

            MutableComponent actionBarText = Component.empty();

            actionBarText.append(TextUtils.actionBarPosition(position)).append(" ");
            actionBarText.append(TextUtils.actionBarTimer(timer)).append(" ");

            if (position > 0) {
                BoatRacePlayer playerAhead = stageManager.positions.getPositions().get(position - 1);
                long delta = stageManager.positions.getDeltaCheckpoint(bPlayer, playerAhead);

                actionBarText.append(TextUtils.actionBarDelta(delta)).append(" ");
            }

            actionBarText.append(TextUtils.actionBarCheckpoint(Math.max(0, checkpoint), maxCheckpoints));

            player.connection.send(new ClientboundSetActionBarTextPacket(actionBarText));
        }
    }

    /**
     * Displays track meta and track leaderboard.
     */
    private void tickSidebar(RaceStageManager stageManager) {
        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            if (!this.sidebars.containsKey(bPlayer)) {
                SidebarWidget newSidebar = this.widgets.addSidebar(
                        TextUtils.scoreboardTitleText("Race"),
                        p -> BoatRacePlayer.of(p).equals(bPlayer));
                newSidebar.addPlayer(player);
                this.sidebars.put(bPlayer, newSidebar);
            }

            SidebarWidget sidebar = this.sidebars.get(bPlayer);

            sidebar.set(content -> {
                content.add(Component.empty());
                TextUtils.scoreboardMeta(this.track.getMeta()).forEach(content::add);
                content.add(Component.empty());

                content.add(TextUtils.scoreboardLaps(
                        stageManager.checkpoints.getLaps(bPlayer),
                        stageManager.getMaxLaps()));

                if (stageManager.getRequiredPits() > 0) {
                    content.add(TextUtils.scoreboardPits(
                            stageManager.pits.getPits(bPlayer),
                            stageManager.getRequiredPits()));
                }

                content.add(TextUtils.scoreboardDuration(
                        stageManager.getDurationTimer(),
                        stageManager.getConfig().maxDuration()));
                content.add(Component.empty());

                List<BoatRacePlayer> positions = stageManager.positions.getPositions();

                if (positions.isEmpty()) {
                    content.add(Component.literal(" No times submitted.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    return;
                }

                int position = stageManager.positions.getPosition(bPlayer);
                LeaderboardType leaderboardType = this.leaderboardType.getOrDefault(bPlayer, LeaderboardType.PLAYER);

                for (Tuple<Integer, BoatRacePlayer> pair : TextUtils.scoreboardAroundAndTop(
                        positions,
                        position,
                        SIDEBAR_RANKING_TOP,
                        SIDEBAR_RANKING_COMPARED)) {
                    if (pair == null) {
                        content.add(TextUtils.PAD_SCOREBOARD_POSITION);
                        continue;
                    }

                    MutableComponent text = Component.empty();
                    BoatRacePlayer player2 = pair.getB();
                    int position2 = pair.getA();
                    boolean highlighted = bPlayer.equals(player2);

                    text.append(" ");
                    text.append(TextUtils.scoreboardPosition(highlighted, position2)).append(" ");

                    switch (leaderboardType) {
                        case LEADER: {
                            if (player2.equals(positions.getFirst())) {
                                break;
                            }

                            long delta = stageManager.positions.getDelta(player2, positions.getFirst());
                            text.append(TextUtils.scoreboardRelative(delta)).append(" ");
                            break;
                        }
                        case PLAYER: {
                            if (player2.equals(bPlayer)) {
                                break;
                            }

                            long delta = stageManager.positions.getDelta(bPlayer, player2);
                            text.append(TextUtils.scoreboardRelative(delta)).append(" ");
                            break;
                        }
                    }

                    text.append(TextUtils.scoreboardName(player2, stageManager.teams.getConfig(player2), highlighted,
                            position2)).append(" ");

                    if (stageManager.getRequiredPits() > 0 && !bPlayer.equals(player2)) {
                        text.append(TextUtils.scoreboardLeaderboardPits(stageManager.pits.getPits(player2)))
                                .append(" ");
                    }

                    content.add(text);
                }
            });
        }
    }

    /**
     * Cycle active leaderboard type.
     *
     * @param player The player whos leaderboard should be updated.
     */
    public LeaderboardType cycleLeaderboard(ServerPlayer player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        LeaderboardType leaderboardType = this.leaderboardType.getOrDefault(bPlayer, LeaderboardType.PLAYER);
        leaderboardType = LeaderboardType.values()[(leaderboardType.ordinal() + 1) % LeaderboardType.values().length];
        this.leaderboardType.put(bPlayer, leaderboardType);

        player.sendSystemMessage(TextUtils.chatLeaderboardType(leaderboardType));
        return leaderboardType;
    }

    public enum LeaderboardType {
        /**
         * Time to all against the leader.
         */
        LEADER,

        /**
         * Time to all against the current player.
         */
        PLAYER;
    }
}
