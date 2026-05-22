package com.abaan404.boatrace.game.timetrial;

import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.leaderboard.Leaderboard;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.utils.TextUtils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
        ServerLevel overworld = this.gameSpace.getServer().getLevel(Level.OVERWORLD);
        Leaderboard leaderboard = overworld.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        int maxCheckpoints = switch (this.track.getAttributes().layout()) {
            // dont count start
            case CIRCULAR -> this.track.getRegions().checkpoints().size() - 1;
            // dont count start and end
            case LINEAR -> this.track.getRegions().checkpoints().size() - 2;
        };

        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
            if (!stageManager.isParticipant(bPlayer)) {
                Component freeRoamText = Component.literal("Free Roaming").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC,
                        ChatFormatting.BOLD);
                player.connection.send(new ClientboundSetActionBarTextPacket(freeRoamText));
                continue;
            }

            PersonalBest pb = leaderboard.getPersonalBest(this.track, bPlayer);

            List<Long> currentSplits = stageManager.splits.getSplits(bPlayer);

            long timer = stageManager.splits.getTimer(bPlayer);
            int position = leaderboard.getLeaderboardPosition(this.track, bPlayer);
            int checkpoint = stageManager.checkpoints.getCheckpointIndex(bPlayer);

            MutableComponent actionBarText = Component.empty();

            // player has a position
            if (position >= 0) {
                actionBarText.append(TextUtils.actionBarPosition(position)).append(" ");
            }

            if (checkpoint > 0 && pb.exists()) {
                long delta = pb.getCheckpointDelta(currentSplits, checkpoint);
                actionBarText.append(TextUtils.actionBarTimer(timer)).append(" ");
                actionBarText.append(TextUtils.actionBarDelta(delta)).append(" ");
            } else {
                actionBarText.append(TextUtils.actionBarTimer(timer)).append(" ");
            }

            actionBarText.append(TextUtils.actionBarCheckpoint(Math.max(0, checkpoint), maxCheckpoints));
            player.connection.send(new ClientboundSetActionBarTextPacket(actionBarText));
        }
    }

    /**
     * Displays track meta and track leaderboard.
     */
    private void tickSidebar() {
        ServerLevel overworld = this.gameSpace.getServer().getLevel(Level.OVERWORLD);
        Leaderboard leaderboard = overworld.getAttachedOrCreate(Leaderboard.ATTACHMENT);

        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

            if (!this.sidebars.containsKey(bPlayer)) {
                SidebarWidget newSidebar = this.widgets.addSidebar(
                        TextUtils.scoreboardTitleText("TimeTrial"),
                        p -> BoatRacePlayer.of(p).equals(bPlayer));
                newSidebar.addPlayer(player);

                this.sidebars.put(bPlayer, newSidebar);
            }

            SidebarWidget sidebar = this.sidebars.get(bPlayer);

            sidebar.set(content -> {
                content.add(Component.empty());
                TextUtils.scoreboardMeta(this.track.getMeta()).forEach(content::add);
                content.add(Component.empty());

                List<PersonalBest> records = leaderboard.getLeaderboard(this.track);

                if (records.isEmpty()) {
                    content.add(Component.literal(" No records submitted.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    return;
                }

                int position = leaderboard.getLeaderboardPosition(this.track, bPlayer);

                for (Tuple<Integer, PersonalBest> pair : TextUtils.scoreboardAroundAndTop(
                        records,
                        position,
                        SIDEBAR_RANKING_TOP,
                        SIDEBAR_RANKING_COMPARED)) {
                    if (pair == null) {
                        content.add(TextUtils.PAD_SCOREBOARD_POSITION);
                        continue;
                    }

                    MutableComponent text = Component.empty();
                    PersonalBest pb = pair.getB();
                    boolean highlighted = bPlayer.equals(pb.player());

                    text.append(" ");
                    text.append(TextUtils.scoreboardPosition(highlighted, pair.getA())).append(" ");
                    text.append(TextUtils.scoreboardAbsolute(pb.timer(), pair.getA())).append(" ");
                    text.append(TextUtils.scoreboardName(pb.player(), GameTeamConfig.DEFAULT, highlighted, pair.getA()));

                    content.add(text);
                }
            });
        }
    }
}
