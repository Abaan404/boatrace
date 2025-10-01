package com.abaan404.boatrace.boatrace.game.timetrial;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import com.abaan404.boatrace.boatrace.BoatRace;
import com.abaan404.boatrace.boatrace.game.maps.TrackMap;
import com.abaan404.boatrace.boatrace.game.timetrial.TimeTrialLeaderboard.PersonalBest;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
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
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public final class TimeTrialWidgets {
    private final GameSpace gameSpace;
    private final GlobalWidgets widgets;
    private final TrackMap track;

    private final Map<PlayerRef, SidebarWidget> sidebars = new Object2ObjectOpenHashMap<>();

    private static final int SIDEBAR_RANKING_COMPARED = 1;
    private static final int SIDEBAR_RANKING_TOP = 5;

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
        // use the overworld to store persistent data
        ServerWorld overworld = this.gameSpace.getServer().getWorld(World.OVERWORLD);
        TimeTrialLeaderboard leaderboard = overworld.getAttachedOrCreate(BoatRace.LEADERBOARD_ATTACHMENT);

        this.tickActionBar(leaderboard, stageManager);
        this.tickSidebar(leaderboard);
    }

    /**
     * Display splits and timers
     *
     * @param leaderboard The leaderboard for pbs
     * @param stageManager The stage manager
     */
    private void tickActionBar(TimeTrialLeaderboard leaderboard, TimeTrialStageManager stageManager) {
        this.gameSpace.getPlayers().forEach(player -> {
            PlayerRef ref = PlayerRef.of(player);
            PersonalBest pb = leaderboard.getPersonalBest(this.track, ref.id());

            MutableText actionBarText = Text.literal(formatTime((long) stageManager.getTimer(ref), true))
                    .formatted(Formatting.BOLD);

            List<Float> currentSplits = stageManager.getSplits(ref);
            List<Float> pbSplits = pb.splits();

            // player has no pb or hasnt started a run yet
            if (pbSplits.isEmpty() || currentSplits.isEmpty()) {
                actionBarText
                        .append(Text.literal(" ◇ ").formatted(Formatting.GRAY))
                        .append(Text.literal(formatTime(0l)).formatted(Formatting.GRAY));

                player.networkHandler.sendPacket(new OverlayMessageS2CPacket(actionBarText));
                return;
            }

            // something has gone wrong, exit
            if (pb.splits().size() != this.track.getRegions().checkpoints().size()) {
                return;
            }

            long currentSplit;
            long pbSplit;

            try {
                int checkpointIndex = stageManager.getCheckpointIndex(ref);

                currentSplit = currentSplits.get(checkpointIndex).longValue();
                pbSplit = pbSplits.get(checkpointIndex).longValue();
            } catch (IndexOutOfBoundsException e) {
                // this should never happen.
                BoatRace.LOGGER.warn("dev is bald.");
                return;
            }

            long delta = currentSplit - pbSplit;

            // faster
            if (pbSplit > currentSplit) {
                actionBarText
                        .append(Text.literal(" ▲ ").formatted(Formatting.BLUE))
                        .append(Text.literal(formatTime(delta)).formatted(Formatting.BLUE));
            }
            // slower
            else if (pbSplit < currentSplit) {
                actionBarText
                        .append(Text.literal(" ▼ ").formatted(Formatting.RED))
                        .append(Text.literal(formatTime(delta)).formatted(Formatting.RED));
            }
            // equal
            else {
                actionBarText
                        .append(Text.literal(" ◇ ").formatted(Formatting.GRAY))
                        .append(Text.literal(formatTime(delta)).formatted(Formatting.GRAY));
            }

            player.networkHandler.sendPacket(new OverlayMessageS2CPacket(actionBarText));
        });
    }

    /**
     * Displays track meta and leaderboard
     *
     * @param leaderboards The leaderboards
     */
    private void tickSidebar(TimeTrialLeaderboard leaderboards) {
        this.gameSpace.getPlayers().forEach(player -> {
            PlayerRef ref = PlayerRef.of(player);

            if (!this.sidebars.containsKey(ref)) {
                Text title = Text.literal("Boat").formatted(Formatting.RED, Formatting.BOLD)
                        .append(Text.literal("Race").formatted(Formatting.WHITE, Formatting.ITALIC))
                        .append(Text.literal(" ◇ ").formatted(Formatting.GRAY))
                        .append(Text.literal("TimeTrial").formatted(Formatting.DARK_GRAY));

                SidebarWidget newSidebar = this.widgets.addSidebar(
                        Text.literal("    ").append(title).append(Text.literal("    ")),
                        p -> PlayerRef.of(p).equals(ref));
                newSidebar.addPlayer(player);

                this.sidebars.put(ref, newSidebar);
            }

            SidebarWidget sidebar = this.sidebars.get(ref);

            sidebar.set(content -> {
                Text name = Text.literal(this.track.getMetaData().name()).formatted(Formatting.BOLD);

                Text authors = Text.literal(" - By " + this.track.getMetaData().authors().stream()
                        .collect(Collectors.joining(", ")))
                        .formatted(Formatting.GRAY, Formatting.ITALIC);

                content.add(Text.literal(""));

                content.add(name);
                content.add(authors);

                content.add(Text.literal(""));

                List<PersonalBest> leaderboard = leaderboards.getTrackLeaderboard(this.track);
                SortedMap<Integer, PersonalBest> toDisplay = new Int2ObjectRBTreeMap<>();

                if (leaderboard.isEmpty()) {
                    content.add(
                            Text.literal(" No records submitted.").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                }

                int playerIndex = -1;

                // find player and add top 3
                for (int i = 0; i < leaderboard.size(); i++) {
                    PersonalBest pb = leaderboard.get(i);

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
                    int max = Math.min(leaderboard.size() - 1, playerIndex + SIDEBAR_RANKING_COMPARED);

                    for (int i = min; i <= max; i++) {
                        toDisplay.putIfAbsent(i + 1, leaderboard.get(i));
                    }
                }

                // show top ranks
                int lastDisplayedTop = 0;
                int lastDisplayedPlayer = 0;
                for (Map.Entry<Integer, PersonalBest> entry : toDisplay.entrySet()) {
                    if (entry.getKey().intValue() <= SIDEBAR_RANKING_TOP) {
                        content.add(this.toScoreboardLeaderboardText(entry.getKey(), entry.getValue(), ref));
                        lastDisplayedTop = entry.getKey();
                        lastDisplayedPlayer = entry.getKey();
                    }
                }

                // add padding if needed
                if (leaderboard.size() > SIDEBAR_RANKING_TOP && !toDisplay.containsKey(SIDEBAR_RANKING_TOP + 1)) {
                    content.add(Text.literal("   ○ ○ ○").formatted(Formatting.DARK_GRAY));
                }

                // show ranks around player
                for (Map.Entry<Integer, PersonalBest> entry : toDisplay.entrySet()) {
                    if (entry.getKey().intValue() > SIDEBAR_RANKING_TOP) {
                        content.add(this.toScoreboardLeaderboardText(entry.getKey(), entry.getValue(), ref));
                        lastDisplayedPlayer = entry.getKey();
                    }
                }

                // add padding at the end
                if (leaderboard.size() > lastDisplayedPlayer && lastDisplayedPlayer != lastDisplayedTop) {
                    content.add(Text.literal("   ○ ○ ○").formatted(Formatting.DARK_GRAY));
                }
            });
        });
    }

    private static String formatTime(long time, boolean showMinutes) {
        long seconds = Math.abs(time) / 1000;

        long hoursStr = seconds / 3600;
        long minutesStr = (seconds % 3600) / 60;
        long secondsStr = seconds % 60;
        long millisStr = Math.abs(time) % 1000;

        if (hoursStr > 0) {
            return String.format("%d:%02d:%02d:%03d", hoursStr, minutesStr, secondsStr, millisStr);
        } else if (minutesStr > 0 || showMinutes) {
            return String.format("%02d:%02d:%03d", minutesStr, secondsStr, millisStr);
        } else {
            return String.format("%02d:%03d", secondsStr, millisStr);
        }
    }

    private static String formatTime(long time) {
        return TimeTrialWidgets.formatTime(time, false);
    }

    private Text toScoreboardLeaderboardText(int position, PersonalBest pb, PlayerRef player) {
        MutableText text = Text.literal("");

        ServerPlayerEntity pbPlayer = PlayerRef.ofUnchecked(pb.id()).getEntity(this.gameSpace);
        String name;

        if (pbPlayer != null) {
            name = pbPlayer.getNameForScoreboard();
        } else {
            name = pb.offlineName();
        }

        MutableText positionText = Text.literal(String.format("(%s) ", position));
        MutableText timeText = Text.literal(String.format("%s ", formatTime((long) pb.timer(), true)));
        MutableText nameText = Text.literal(String.format("%s", name));

        if (position == 1) {
            text.append(positionText.formatted(Formatting.YELLOW));
            text.append(timeText.formatted(Formatting.GOLD));
            text.append(nameText.formatted(Formatting.GOLD, Formatting.BOLD));
        } else {
            if (position == 2) {
                text.append(positionText.formatted(Formatting.WHITE));
            } else {
                text.append(positionText.formatted(Formatting.GRAY));
            }

            text.append(timeText.formatted(Formatting.WHITE));

            if (player.id().equals(pb.id())) {
                text.append(nameText.formatted(Formatting.BOLD));
            } else {
                text.append(nameText.formatted(Formatting.GRAY));
            }
        }

        return text;
    }
}
