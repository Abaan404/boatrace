package com.abaan404.boatrace.game.race;

import java.util.List;
import java.util.Map;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.maps.TrackMap;
import com.abaan404.boatrace.utils.WidgetTextUtil;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
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
    private final TrackMap track;

    private static final int SIDEBAR_RANKING_COMPARED = 2;
    private static final int SIDEBAR_RANKING_TOP = 5;

    private final Map<BoatRacePlayer, SidebarWidget> sidebars = new Object2ObjectOpenHashMap<>();

    public RaceWidgets(GameSpace gameSpace, GlobalWidgets widgets, TrackMap track) {
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
            int position = stageManager.laps.getPosition(bPlayer);
            int checkpoint = stageManager.checkpoints.getCheckpointIndex(bPlayer);

            MutableText actionBarText = Text.empty();

            actionBarText.append(WidgetTextUtil.actionBarPosition(position)).append(" ");
            actionBarText.append(WidgetTextUtil.actionBarTimer(timer)).append(" ");

            if (position > 0) {
                long delta = stageManager.laps.getSavedDeltaToAhead(bPlayer);
                actionBarText.append(WidgetTextUtil.actionBarDelta(delta)).append(" ");
            }

            actionBarText.append(WidgetTextUtil.actionBarCheckpoint(Math.max(0, checkpoint), maxCheckpoints));

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
                        WidgetTextUtil.scoreboardTitleText("Race"),
                        p -> BoatRacePlayer.of(p).equals(bPlayer));
                newSidebar.addPlayer(player);
                this.sidebars.put(bPlayer, newSidebar);
            }

            SidebarWidget sidebar = this.sidebars.get(bPlayer);

            sidebar.set(content -> {
                WidgetTextUtil.scoreboardMeta(this.track.getMeta()).forEach(content::add);

                content.add(WidgetTextUtil.scoreboardLaps(
                        stageManager.laps.getLeadingLaps(),
                        stageManager.getRaceConfig().maxLaps()));

                content.add(WidgetTextUtil.scoreboardDuration(
                        stageManager.getDurationTimer(),
                        stageManager.getRaceConfig().maxDuration()));
                content.add(Text.empty());

                List<BoatRacePlayer> players = stageManager.laps.getPositions();

                if (players.isEmpty()) {
                    content.add(Text.literal(" No times submitted.")
                            .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                    return;
                }

                int position = stageManager.laps.getPosition(bPlayer);
                Map<BoatRacePlayer, Long> splits = stageManager.laps.getDeltas(bPlayer);

                for (Pair<Integer, BoatRacePlayer> pair : WidgetTextUtil.scoreboardAroundAndTop(
                        players,
                        position,
                        SIDEBAR_RANKING_TOP,
                        SIDEBAR_RANKING_COMPARED)) {
                    if (pair == null) {
                        content.add(WidgetTextUtil.PAD_SCOREBOARD_POSITION);
                        continue;
                    }

                    MutableText text = Text.empty();
                    BoatRacePlayer player2 = pair.getRight();
                    boolean highlighted = bPlayer.equals(player2);

                    text.append(" ");
                    text.append(WidgetTextUtil.scoreboardPosition(highlighted, pair.getLeft())).append(" ");

                    if (!bPlayer.equals(pair.getRight())) {
                        text.append(WidgetTextUtil.scoreboardRelative(splits.get(player2), pair.getLeft())).append(" ");
                    }

                    text.append(WidgetTextUtil.scoreboardName(player2, highlighted, pair.getLeft()));

                    content.add(text);
                }
            });
        }
    }
}
