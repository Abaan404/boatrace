package com.abaan404.boatrace.game.timetrial;

import java.util.List;
import java.util.Map;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.maps.TrackMap;
import com.abaan404.boatrace.utils.WidgetTextUtil;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

public final class TimeTrialWidgets {
    private final GameSpace gameSpace;
    private final GlobalWidgets widgets;
    private final TrackMap track;

    private final Map<BoatRacePlayer, SidebarWidget> sidebars = new Object2ObjectOpenHashMap<>();

    private static final int SIDEBAR_RANKING_COMPARED = 1;
    private static final int SIDEBAR_RANKING_TOP = 2;

    public TimeTrialWidgets(GameSpace gameSpace, GlobalWidgets widgets, TrackMap track) {
        this.gameSpace = gameSpace;
        this.track = track;
        this.widgets = widgets;
    }

    /**
     * Tick the UI for the player.
     *
     * @param stageManager The game's state.
     */
    public void tick(TimeTrialStageManager stageManager) {
        this.tickActionBar(stageManager);
        this.tickSidebar();
    }

    /**
     * Display splits and timers on the client's action bar.
     *
     * @param stageManager The stage manager.
     */
    private void tickActionBar(TimeTrialStageManager stageManager) {
        ServerWorld overworld = this.gameSpace.getServer().getWorld(World.OVERWORLD);
        Leaderboard leaderboard = overworld.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        int maxCheckpoints = switch (this.track.getMeta().layout()) {
            // dont count start
            case CIRCULAR -> this.track.getRegions().checkpoints().size() - 1;
            // dont count start and end
            case LINEAR -> this.track.getRegions().checkpoints().size() - 2;
        };

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            if (!stageManager.isParticipant(player)) {
                Text freeRoamText = Text.literal("Free Roaming").formatted(Formatting.GRAY, Formatting.ITALIC,
                        Formatting.BOLD);
                player.networkHandler.sendPacket(new OverlayMessageS2CPacket(freeRoamText));
                continue;
            }

            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
            PersonalBest pb = leaderboard.getPersonalBest(this.track, bPlayer);

            List<Long> currentSplits = stageManager.splits.getSplits(bPlayer);

            long timer = stageManager.splits.getTimer(bPlayer);
            int position = leaderboard.getLeaderboardPosition(this.track, bPlayer);
            int checkpoint = stageManager.checkpoints.getCheckpointIndex(bPlayer);

            MutableText actionBarText = Text.empty();

            // player has a position
            if (position > 0) {
                actionBarText.append(WidgetTextUtil.actionBarPosition(position)).append(" ");
            }

            if (checkpoint > 0 && pb.exists()) {
                long delta = pb.getCheckpointDelta(currentSplits, checkpoint);
                actionBarText.append(WidgetTextUtil.actionBarTimerDelta(timer, delta)).append(" ");
            } else {
                actionBarText.append(WidgetTextUtil.actionBarTimer(timer)).append(" ");
            }

            actionBarText.append(WidgetTextUtil.actionBarCheckpoint(Math.max(0, checkpoint), maxCheckpoints));
            player.networkHandler.sendPacket(new OverlayMessageS2CPacket(actionBarText));
        }
    }

    /**
     * Displays track meta and track leaderboard.
     */
    private void tickSidebar() {
        ServerWorld overworld = this.gameSpace.getServer().getWorld(World.OVERWORLD);
        Leaderboard leaderboard = overworld.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            if (!this.sidebars.containsKey(bPlayer)) {
                SidebarWidget newSidebar = this.widgets.addSidebar(
                        WidgetTextUtil.scoreboardTitleText("TimeTrial"),
                        p -> BoatRacePlayer.of(p).equals(bPlayer));
                newSidebar.addPlayer(player);

                this.sidebars.put(bPlayer, newSidebar);
            }

            SidebarWidget sidebar = this.sidebars.get(bPlayer);

            sidebar.set(content -> {
                WidgetTextUtil.scoreboardMeta(this.track.getMeta()).forEach(content::add);

                List<PersonalBest> pbs = leaderboard.getLeaderboard(this.track);

                if (pbs.isEmpty()) {
                    content.add(Text.literal(" No records submitted.")
                            .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                    return;
                }

                int position = leaderboard.getLeaderboardPosition(this.track, bPlayer);

                // add top players
                WidgetTextUtil.scoreboardPersonalBestsAround(pbs, 0, SIDEBAR_RANKING_TOP)
                        .entrySet().stream()
                        .map(entry -> WidgetTextUtil
                                .scoreboardLeaderboardText(entry.getValue(), entry.getKey(), bPlayer))
                        .forEach(content::add);

                // add padding if theres positions between overlaps
                if (position > 0 && position - SIDEBAR_RANKING_COMPARED > SIDEBAR_RANKING_TOP + 1) {
                    content.add(WidgetTextUtil.PAD_SCOREBOARD_POSITION);
                }

                // show positions around the player
                if (position > 0 && position + SIDEBAR_RANKING_COMPARED > SIDEBAR_RANKING_TOP) {
                    WidgetTextUtil.scoreboardPersonalBestsAround(pbs, position, SIDEBAR_RANKING_COMPARED)
                            .entrySet().stream()
                            .filter(entry -> entry.getKey() > SIDEBAR_RANKING_TOP + 1) // remove overlaps
                            .map(entry -> WidgetTextUtil
                                    .scoreboardLeaderboardText(entry.getValue(), entry.getKey(), bPlayer))
                            .forEach(content::add);
                }

                // add padding at the end if needed
                int lastDisplayed = Math.max(position + SIDEBAR_RANKING_COMPARED, SIDEBAR_RANKING_TOP);
                if (pbs.size() - 1 > lastDisplayed) {
                    content.add(WidgetTextUtil.PAD_SCOREBOARD_POSITION);
                }
            });
        }
    }
}
