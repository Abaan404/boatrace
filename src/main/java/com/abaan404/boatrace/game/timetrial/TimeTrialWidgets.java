package com.abaan404.boatrace.game.timetrial;

import java.util.List;
import java.util.Map;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.utils.TextUtil;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

public final class TimeTrialWidgets {
    private final GameSpace gameSpace;
    private final GlobalWidgets widgets;
    private final BoatRaceTrack track;

    private final Map<BoatRacePlayer, SidebarWidget> sidebars = new Object2ObjectOpenHashMap<>();

    private static final int SIDEBAR_RANKING_COMPARED = 1;
    private static final int SIDEBAR_RANKING_TOP = 3;

    public TimeTrialWidgets(GameSpace gameSpace, GlobalWidgets widgets, BoatRaceTrack track) {
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
            if (position >= 0) {
                actionBarText.append(TextUtil.actionBarPosition(position)).append(" ");
            }

            if (checkpoint > 0 && pb.exists()) {
                long delta = pb.getCheckpointDelta(currentSplits, checkpoint);
                actionBarText.append(TextUtil.actionBarTimer(timer)).append(" ");
                actionBarText.append(TextUtil.actionBarDelta(delta)).append(" ");
            } else {
                actionBarText.append(TextUtil.actionBarTimer(timer)).append(" ");
            }

            actionBarText.append(TextUtil.actionBarCheckpoint(Math.max(0, checkpoint), maxCheckpoints));
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
                        TextUtil.scoreboardTitleText("TimeTrial"),
                        p -> BoatRacePlayer.of(p).equals(bPlayer));
                newSidebar.addPlayer(player);

                this.sidebars.put(bPlayer, newSidebar);
            }

            SidebarWidget sidebar = this.sidebars.get(bPlayer);

            sidebar.set(content -> {
                TextUtil.scoreboardMeta(this.track.getMeta()).forEach(content::add);

                List<PersonalBest> records = leaderboard.getLeaderboard(this.track);

                if (records.isEmpty()) {
                    content.add(Text.literal(" No records submitted.")
                            .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                    return;
                }

                int position = leaderboard.getLeaderboardPosition(this.track, bPlayer);

                for (Pair<Integer, PersonalBest> pair : TextUtil.scoreboardAroundAndTop(
                        records,
                        position,
                        SIDEBAR_RANKING_TOP,
                        SIDEBAR_RANKING_COMPARED)) {
                    if (pair == null) {
                        content.add(TextUtil.PAD_SCOREBOARD_POSITION);
                        continue;
                    }

                    MutableText text = Text.empty();
                    PersonalBest pb = pair.getRight();
                    boolean highlighted = bPlayer.equals(pb.player());

                    text.append(" ");
                    text.append(TextUtil.scoreboardPosition(highlighted, pair.getLeft())).append(" ");
                    text.append(TextUtil.scoreboardAbsolute(pb.timer(), pair.getLeft())).append(" ");
                    text.append(TextUtil.scoreboardName(pb.player(), GameTeamConfig.DEFAULT, highlighted, pair.getLeft()));

                    content.add(text);
                }
            });
        }
    }
}
