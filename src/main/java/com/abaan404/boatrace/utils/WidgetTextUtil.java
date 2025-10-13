package com.abaan404.boatrace.utils;

import java.util.EnumSet;
import java.util.List;
import java.util.SequencedMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.maps.TrackMap;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Common texts for widgets shared by each gamemode.
 */
public final class WidgetTextUtil {
    private WidgetTextUtil() {
    }

    public static final Text PAD_SCOREBOARD_POSITION = Text.literal("   ○ ○ ○").formatted(Formatting.DARK_GRAY);

    /**
     * Create a text for player position on the leaderboard.
     *
     * @param position The player's position to display.
     * @return A text if the position was valid otherwise an empty text.
     */
    public static Text actionBarPosition(int position) {
        if (position > -1) {
            return Text.literal(String.format("P%d", position + 1)).formatted(Formatting.AQUA, Formatting.BOLD);
        } else {
            return Text.empty();
        }
    }

    /**
     * Create a text to show remaining checkpoints.
     *
     * @param checkpoint     The current checkpoint
     * @param maxCheckpoints The total number of checkpoints
     * @return The text showing checkpoints.
     */
    public static Text actionBarCheckpoint(int checkpoint, int maxCheckpoints) {
        return Text.literal(String.format("(%d/%d)", checkpoint, maxCheckpoints))
                .formatted(Formatting.DARK_GRAY, Formatting.BOLD);
    }

    /**
     * Create a text to show the current timer.
     *
     * @param timer The time in ms.
     * @return The timer text.
     */
    public static Text actionBarTimer(long timer) {
        return Text.literal(TimeUtils.formatTime(timer))
                .formatted(Formatting.BOLD);
    }

    /**
     * Create a text to show the current timer along with splits.
     *
     * @param timer The timer in ms.
     * @param delta The delta for this split.
     * @return The timer and splits text seperated by a symbol.
     */
    public static Text actionBarTimerDelta(long timer, long delta) {
        String deltaString = TimeUtils.formatTime(
                delta,
                EnumSet.of(TimeUtils.Selector.SECONDS, TimeUtils.Selector.MILLISECONDS),
                EnumSet.allOf(TimeUtils.Selector.class));

        // faster
        if (delta < 0) {
            return Text.empty()
                    .append(WidgetTextUtil.actionBarTimer(timer))
                    .append(Text.literal(" ▲ ").formatted(Formatting.BLUE))
                    .append(Text.literal(deltaString).formatted(Formatting.BLUE))
                    .formatted(Formatting.BOLD);
        }
        // slower
        else if (delta > 0) {
            return Text.empty()
                    .append(WidgetTextUtil.actionBarTimer(timer))
                    .append(Text.literal(" ▼ ").formatted(Formatting.RED))
                    .append(Text.literal(deltaString).formatted(Formatting.RED))
                    .formatted(Formatting.BOLD);
        }
        // equal
        else {
            return Text.empty()
                    .append(WidgetTextUtil.actionBarTimer(timer))
                    .append(Text.literal(" ◇ ").formatted(Formatting.GRAY))
                    .append(Text.literal(deltaString).formatted(Formatting.GRAY))
                    .formatted(Formatting.BOLD);
        }
    }

    /**
     * A consistent title for each game mode.
     *
     * @param mode The current mode.
     * @return The title text.
     */
    public static Text scoreboardTitleText(String mode) {
        return Text.literal("    ")
                .append(Text.literal("Boat").formatted(Formatting.RED))
                .append(Text.literal("Race").formatted(Formatting.WHITE, Formatting.ITALIC))
                .append(Text.literal(" ◇ ").formatted(Formatting.GRAY))
                .append(Text.literal(mode).formatted(Formatting.DARK_GRAY))
                .append(Text.literal("    "))
                .formatted(Formatting.BOLD);
    }

    /**
     * Create lines of scoreboard texts for track metadata (i.e. authors, name, etc)
     *
     * @param meta The track meta.
     * @return A list of text for each line.
     */
    public static List<Text> scoreboardMeta(TrackMap.Meta meta) {
        List<Text> list = new ObjectArrayList<>();

        list.add(Text.empty());
        list.add(Text.literal(" ").append(meta.name()).formatted(Formatting.BOLD));

        if (meta.authors().isEmpty()) {
            list.add(Text.literal("    - By Unknown Author(s)")
                    .formatted(Formatting.GRAY, Formatting.ITALIC));
        } else {
            list.add(Text.literal("    - By " + meta.authors().stream()
                    .collect(Collectors.joining(", ")))
                    .formatted(Formatting.GRAY, Formatting.ITALIC));
        }

        list.add(Text.empty());

        return list;
    }

    public static Text scoreboardDuration(long duration, long maxDuration) {
        return Text.empty()
                .append(Text.literal(" Duration: ").formatted(Formatting.RED))
                .append(Text.literal(TimeUtils.formatTime(
                        duration,
                        EnumSet.complementOf(EnumSet.of(TimeUtils.Selector.HOURS)),
                        EnumSet.complementOf(EnumSet.of(TimeUtils.Selector.MILLISECONDS)))))
                .append(Text.literal(" / ").formatted(Formatting.ITALIC))
                .append(Text.literal(TimeUtils.formatTime(
                        maxDuration,
                        EnumSet.complementOf(EnumSet.of(TimeUtils.Selector.HOURS)),
                        EnumSet.complementOf(EnumSet.of(TimeUtils.Selector.MILLISECONDS)))));

    }

    /**
     * Format a player's personal best to a line text thats ready to be shown on the
     * leaderboard.
     *
     * @param pb        The player's personal best.
     * @param position  The player's position in the leaderboard.
     * @param curPlayer Highlight the leaderboard if the player is the pb player.
     * @return A text ready to display player's pb.
     */
    public static Text scoreboardLeaderboardText(PersonalBest pb, int position, BoatRacePlayer curPlayer) {
        MutableText text = Text.empty();

        MutableText P = Text.literal(" P");
        MutableText positionText = Text.literal(String.format("%s ", position));
        MutableText timeText = Text.literal(String.format("%s ", TimeUtils.formatTime(pb.timer())));
        MutableText nameText = Text.literal(String.format("%s", pb.player().offlineName()));

        if (position == 1) {
            text.append(P.formatted(Formatting.RED, Formatting.BOLD));
            text.append(positionText.formatted(Formatting.BOLD, Formatting.ITALIC));
            text.append(timeText.formatted(Formatting.YELLOW));
            text.append(nameText.formatted(Formatting.YELLOW, Formatting.BOLD));
        } else {
            if (position == 2) {
                text.append(P.formatted(Formatting.GRAY, Formatting.BOLD));
                text.append(positionText.formatted(Formatting.BOLD, Formatting.ITALIC));
            } else if (position == 3) {
                text.append(P.formatted(Formatting.GOLD, Formatting.BOLD));
                text.append(positionText.formatted(Formatting.BOLD, Formatting.ITALIC));
            } else if (pb.player().equals(curPlayer)) {
                text.append(P.formatted(Formatting.BOLD));
                text.append(positionText.formatted(Formatting.BOLD, Formatting.ITALIC));
            } else {
                text.append(P.formatted(Formatting.DARK_GRAY, Formatting.BOLD));
                text.append(positionText.formatted(Formatting.DARK_GRAY, Formatting.BOLD, Formatting.ITALIC));
            }

            text.append(timeText.formatted(Formatting.WHITE));

            if (pb.player().equals(curPlayer)) {
                text.append(nameText.formatted(Formatting.BOLD));
            } else {
                text.append(nameText.formatted(Formatting.GRAY));
            }
        }

        return text;
    }

    /**
     * Return a map of personal bests surrounding a list with its positions
     *
     * @param pbs   Personal bests to choose from
     * @param at    The index to start from.
     * @param range The range before and after the start index to fetch.
     */
    public static SequencedMap<Integer, PersonalBest> scoreboardPersonalBestsAround(List<PersonalBest> pbs, int at,
            int range) {
        if (pbs == null || pbs.isEmpty()) {
            return new Int2ObjectLinkedOpenHashMap<>();
        }

        if (at < 0) {
            at = 0;
        }

        if (at >= pbs.size()) {
            at = pbs.size() - 1;
        }

        int from = Math.max(0, at - range);
        int to = Math.min(pbs.size(), at + range + 1);

        return IntStream.range(from, to)
                .boxed()
                .collect(Collectors.toMap(
                        i -> i + 1,
                        i -> pbs.get(i),
                        (a, b) -> a,
                        Int2ObjectLinkedOpenHashMap::new));
    }
}
