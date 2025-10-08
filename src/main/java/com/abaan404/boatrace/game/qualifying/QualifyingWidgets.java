package com.abaan404.boatrace.game.qualifying;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.maps.TrackMap;
import com.abaan404.boatrace.utils.WidgetTextUtil;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class QualifyingWidgets {
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final GlobalWidgets widgets;
    private final TrackMap track;

    private static final int SIDEBAR_RANKING_COMPARED = 1;
    private static final int SIDEBAR_RANKING_TOP = 5;

    private final Map<PlayerRef, SidebarWidget> sidebars = new Object2ObjectOpenHashMap<>();

    public QualifyingWidgets(GameSpace gameSpace, ServerWorld world, GlobalWidgets widgets, TrackMap track) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.track = track;
        this.widgets = widgets;

    }

    /**
     * Tick the UI for the player.
     *
     * @param stageManager The game's state.
     */
    public void tick(QualifyingStageManager stageManager) {
        this.tickActionBar(stageManager);
        this.tickSidebar(stageManager);
    }

    /**
     * Display splits and timers on the client's action bar.
     *
     * @param stageManager The stage manager.
     */
    private void tickActionBar(QualifyingStageManager stageManager) {
        Leaderboard leaderboard = this.world.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            if (!stageManager.isParticipant(player)) {
                continue;
            }

            PlayerRef ref = PlayerRef.of(player);
            PersonalBest pb = leaderboard.getPersonalBest(this.track, ref.id());

            List<Float> currentSplits = stageManager.splits.getSplits(ref);
            List<Float> pbSplits = pb.splits();

            long timer = (long) stageManager.splits.getTimer(ref);
            int position = leaderboard.getTrackLeaderboardPosition(this.track, ref.id());

            // player has no pb or hasnt started a run yet
            if (pbSplits.isEmpty() || currentSplits.isEmpty()) {
                player.networkHandler.sendPacket(new OverlayMessageS2CPacket(
                        WidgetTextUtil.actionBarTimerSplit(position, timer, 0, 0)));
                return;
            }

            long currentSplit;
            long pbSplit;

            try {
                int checkpointIndex = stageManager.checkpoints.getCheckpointIndex(ref);

                currentSplit = currentSplits.get(checkpointIndex).longValue();
                pbSplit = pbSplits.get(checkpointIndex).longValue();
            } catch (IndexOutOfBoundsException e) {
                player.networkHandler.sendPacket(new OverlayMessageS2CPacket(
                        WidgetTextUtil.actionBarTimerSplit(position, timer, 0, 0)));
                return;
            }

            player.networkHandler.sendPacket(new OverlayMessageS2CPacket(
                    WidgetTextUtil.actionBarTimerSplit(position, timer, currentSplit, pbSplit)));
        }
    }

    /**
     * Displays track meta and track leaderboard.
     */
    private void tickSidebar(QualifyingStageManager stageManager) {
        Leaderboard leaderboard = this.world.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            PlayerRef ref = PlayerRef.of(player);

            if (!this.sidebars.containsKey(ref)) {
                SidebarWidget newSidebar = this.widgets.addSidebar(
                        WidgetTextUtil.scoreboardTitleText("Qualifying"),
                        p -> PlayerRef.of(p).equals(ref));
                newSidebar.addPlayer(player);
                this.sidebars.put(ref, newSidebar);
            }

            SidebarWidget sidebar = this.sidebars.get(ref);

            sidebar.set(content -> {
                WidgetTextUtil.scoreboardTrackText(this.track.getMeta()).forEach(content::add);

                Text timeLeft = Text.literal(WidgetTextUtil.formatTime(stageManager.getTimeLeft(), true))
                        .formatted(Formatting.BOLD, Formatting.DARK_AQUA);

                content.add(timeLeft);

                List<PersonalBest> pbs = leaderboard.getTrackLeaderboard(this.track);
                SortedMap<Integer, PersonalBest> toDisplay = new Int2ObjectRBTreeMap<>();

                int playerIndex = -1;

                // find player and add top 3
                for (int i = 0; i < pbs.size(); i++) {
                    PersonalBest pb = pbs.get(i);

                    if (pb.id().equals(ref.id())) {
                        playerIndex = i;
                    }

                    // add top players
                    if (i < SIDEBAR_RANKING_TOP) {
                        toDisplay.put(i + 1, pb);
                    }
                }

                // player has a time
                if (playerIndex != -1) {
                    // add rankings above and below the player
                    int min = Math.max(0, playerIndex - SIDEBAR_RANKING_COMPARED);
                    int max = Math.min(pbs.size() - 1, playerIndex + SIDEBAR_RANKING_COMPARED);

                    for (int i = min; i <= max; i++) {
                        toDisplay.putIfAbsent(i + 1, pbs.get(i));
                    }
                }

                // show top ranks
                int lastDisplayedTop = 0;
                int lastDisplayedPlayer = 0;
                for (Map.Entry<Integer, PersonalBest> entry : toDisplay.entrySet()) {
                    if (entry.getKey().intValue() <= SIDEBAR_RANKING_TOP) {
                        int position = entry.getKey();
                        content.add(WidgetTextUtil.scoreboardLeaderboardText(
                                entry.getValue(),
                                entry.getKey(),
                                entry.getValue().id().equals(ref.id())));

                        lastDisplayedTop = position;
                        lastDisplayedPlayer = position;
                    }
                }

                // add padding if needed
                if (pbs.size() > SIDEBAR_RANKING_TOP && !toDisplay.containsKey(SIDEBAR_RANKING_TOP + 1)) {
                    content.add(WidgetTextUtil.PAD_SCOREBOARD_POSITION);
                }

                // show ranks around player
                for (Map.Entry<Integer, PersonalBest> entry : toDisplay.entrySet()) {
                    if (entry.getKey().intValue() > SIDEBAR_RANKING_TOP) {
                        content.add(WidgetTextUtil.scoreboardLeaderboardText(
                                entry.getValue(),
                                entry.getKey(),
                                entry.getValue().id().equals(ref.id())));
                        lastDisplayedPlayer = entry.getKey();
                    }
                }

                // add padding at the end
                if (pbs.size() > lastDisplayedPlayer && lastDisplayedPlayer != lastDisplayedTop) {
                    content.add(WidgetTextUtil.PAD_SCOREBOARD_POSITION);
                }
            });
        }
    }
}
