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
        TimeTrialLeaderboard leaderboard = overworld.getAttachedOrCreate(TimeTrialLeaderboard.ATTACHMENT);

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            if (stageManager.isSpectator(player)) {
                Text freeRoamText = Text.literal("Free Roaming").formatted(Formatting.GRAY, Formatting.ITALIC,
                        Formatting.BOLD);
                player.networkHandler.sendPacket(new OverlayMessageS2CPacket(freeRoamText));
                continue;
            }

            PlayerRef ref = PlayerRef.of(player);
            PersonalBest pb = leaderboard.getPersonalBest(this.track, ref.id());

            MutableText actionBarText = Text.literal(formatTime((long) stageManager.splits.getTimer(ref), true))
                    .formatted(Formatting.BOLD);

            List<Float> currentSplits = stageManager.splits.getSplits(ref);
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
                int checkpointIndex = stageManager.checkpoints.getCheckpointIndex(ref);

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
        }
    }

    /**
     * Displays track meta and track leaderboard.
     */
    private void tickSidebar() {
        ServerWorld overworld = this.gameSpace.getServer().getWorld(World.OVERWORLD);
        TimeTrialLeaderboard leaderboard = overworld.getAttachedOrCreate(TimeTrialLeaderboard.ATTACHMENT);

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
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

                List<PersonalBest> pbs = leaderboard.getTrackLeaderboard(this.track);
                SortedMap<Integer, PersonalBest> toDisplay = new Int2ObjectRBTreeMap<>();

                if (pbs.isEmpty()) {
                    content.add(
                            Text.literal(" No records submitted.").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
                }

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
                        content.add(this.toScoreboardLeaderboardText(entry.getKey(), entry.getValue(), ref));
                        lastDisplayedTop = entry.getKey();
                        lastDisplayedPlayer = entry.getKey();
                    }
                }

                // add padding if needed
                if (pbs.size() > SIDEBAR_RANKING_TOP && !toDisplay.containsKey(SIDEBAR_RANKING_TOP + 1)) {
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
                if (pbs.size() > lastDisplayedPlayer && lastDisplayedPlayer != lastDisplayedTop) {
                    content.add(Text.literal("   ○ ○ ○").formatted(Formatting.DARK_GRAY));
                }
            });
        }
    }

    /**
     * Format the time into a string.
     *
     * @param time        The time in ms.
     * @param showMinutes If should show empty minutes (00:).
     * @return The formatted time.
     */
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

    /**
     * Format the time into a string.
     *
     * @param time The time in ms
     * @return The formatted time.
     */
    private static String formatTime(long time) {
        return TimeTrialWidgets.formatTime(time, false);
    }

    /**
     * Format a player's personal best to a line text thats ready to be shown on the leaderboard.
     *
     * @param position The player's position in the leaderboard.
     * @param pb The player's personal best.
     * @param player The player.
     * @return A text ready to be displayed on the leaderboard.
     */
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
