package com.abaan404.boatrace.game.race;

import java.util.List;
import java.util.Map;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.utils.TextUtil;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
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

        long countdown = stageManager.countdown.getCountdown();
        if (countdown <= 0) {
            this.shownGo = true;
        }

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 30, 20));
            player.networkHandler.sendPacket(new TitleS2CPacket(TextUtil.titleCountdown(countdown)));
        }
    }

    /**
     * Display splits and timers on the client's action bar.
     *
     * @param stageManager The stage manager.
     */
    private void tickActionBar(RaceStageManager stageManager) {
        int maxCheckpoints = switch (this.track.getMeta().layout()) {
            // dont count start
            case CIRCULAR -> this.track.getRegions().checkpoints().size() - 1;
            // dont count start and end
            case LINEAR -> this.track.getRegions().checkpoints().size() - 2;
        };

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            if (!stageManager.isParticipant(player)) {
                continue;
            }

            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            long timer = stageManager.splits.getTimer(bPlayer);
            int position = stageManager.positions.getPosition(bPlayer);
            int checkpoint = stageManager.checkpoints.getCheckpointIndex(bPlayer);

            MutableText actionBarText = Text.empty();

            actionBarText.append(TextUtil.actionBarPosition(position)).append(" ");
            actionBarText.append(TextUtil.actionBarTimer(timer)).append(" ");

            if (position > 0) {
                BoatRacePlayer playerAhead = stageManager.positions.getPositions().get(position - 1);
                long delta = stageManager.positions.getDeltaCheckpoint(bPlayer, playerAhead);

                actionBarText.append(TextUtil.actionBarDelta(delta)).append(" ");
            }

            actionBarText.append(TextUtil.actionBarCheckpoint(Math.max(0, checkpoint), maxCheckpoints));

            player.networkHandler.sendPacket(new OverlayMessageS2CPacket(actionBarText));
        }
    }

    /**
     * Displays track meta and track leaderboard.
     */
    private void tickSidebar(RaceStageManager stageManager) {
        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            if (!this.sidebars.containsKey(bPlayer)) {
                SidebarWidget newSidebar = this.widgets.addSidebar(
                        TextUtil.scoreboardTitleText("Race"),
                        p -> BoatRacePlayer.of(p).equals(bPlayer));
                newSidebar.addPlayer(player);
                this.sidebars.put(bPlayer, newSidebar);
            }

            SidebarWidget sidebar = this.sidebars.get(bPlayer);

            sidebar.set(content -> {
                TextUtil.scoreboardMeta(this.track.getMeta()).forEach(content::add);

                content.add(TextUtil.scoreboardLaps(
                        stageManager.getLeadingLaps(),
                        stageManager.getConfig().maxLaps()));

                content.add(TextUtil.scoreboardDuration(
                        stageManager.getDurationTimer(),
                        stageManager.getConfig().maxDuration()));
                content.add(Text.empty());

                List<BoatRacePlayer> positions = stageManager.positions.getPositions();

                if (positions.isEmpty()) {
                    content.add(Text.literal(" No times submitted.")
                            .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                    return;
                }

                int position = stageManager.positions.getPosition(bPlayer);
                LeaderboardType leaderboardType = this.leaderboardType.getOrDefault(bPlayer, LeaderboardType.PLAYER);

                for (Pair<Integer, BoatRacePlayer> pair : TextUtil.scoreboardAroundAndTop(
                        positions,
                        position,
                        SIDEBAR_RANKING_TOP,
                        SIDEBAR_RANKING_COMPARED)) {
                    if (pair == null) {
                        content.add(TextUtil.PAD_SCOREBOARD_POSITION);
                        continue;
                    }

                    MutableText text = Text.empty();
                    BoatRacePlayer player2 = pair.getRight();
                    int position2 = pair.getLeft();
                    boolean highlighted = bPlayer.equals(player2);

                    text.append(" ");
                    text.append(TextUtil.scoreboardPosition(highlighted, position2)).append(" ");

                    switch (leaderboardType) {
                        case LEADER: {
                            if (player2.equals(positions.getFirst())) {
                                break;
                            }

                            long delta = stageManager.positions.getDelta(player2, positions.getFirst());
                            text.append(TextUtil.scoreboardRelative(delta)).append(" ");
                            break;
                        }
                        case PLAYER: {
                            if (player2.equals(bPlayer)) {
                                break;
                            }

                            long delta = stageManager.positions.getDelta(bPlayer, player2);
                            text.append(TextUtil.scoreboardRelative(delta)).append(" ");
                            break;
                        }
                    }

                    text.append(TextUtil.scoreboardName(player2, stageManager.teams.getConfig(player2), highlighted, position2));

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
    public void cycleLeaderboard(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        LeaderboardType leaderboardType = this.leaderboardType.getOrDefault(bPlayer, LeaderboardType.PLAYER);
        leaderboardType = leaderboardType.cycle();
        this.leaderboardType.put(bPlayer, leaderboardType);

        player.sendMessage(TextUtil.chatLeaderboardType(leaderboardType));
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

        public LeaderboardType cycle() {
            if (this == PLAYER) {
                return LEADER;
            }

            // Otherwise, get the next enum by ordinal position
            return LeaderboardType.values()[this.ordinal() + 1];
        }
    }
}
