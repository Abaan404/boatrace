package com.abaan404.boatrace.utils;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.maps.TrackMap;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

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

    /**
     * Format duration as text.
     *
     * @param duration The elapsed time.
     * @param maxDuration The remaining time.
     * @return The duration text.
     */
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
     * Format position as text.
     *
     * @param player The player the position belongs to.
     * @param curPlayer The player that should be highlighted.
     * @param position The track position.
     * @return The position text.
     */
    public static Text scoreboardPosition(BoatRacePlayer player, BoatRacePlayer curPlayer, int position) {
        MutableText text = Text.empty();

        MutableText P = Text.literal("P");
        MutableText positionText = Text.literal(String.valueOf(position + 1));

        if (position == 0) {
            text.append(P.formatted(Formatting.RED, Formatting.BOLD));
            text.append(positionText.formatted(Formatting.BOLD, Formatting.ITALIC));
        } else {
            if (position == 1) {
                text.append(P.formatted(Formatting.GRAY, Formatting.BOLD));
                text.append(positionText.formatted(Formatting.BOLD, Formatting.ITALIC));
            } else if (position == 2) {
                text.append(P.formatted(Formatting.GOLD, Formatting.BOLD));
                text.append(positionText.formatted(Formatting.BOLD, Formatting.ITALIC));
            } else if (player.equals(curPlayer)) {
                text.append(P.formatted(Formatting.BOLD));
                text.append(positionText.formatted(Formatting.BOLD, Formatting.ITALIC));
            } else {
                text.append(P.formatted(Formatting.DARK_GRAY, Formatting.BOLD));
                text.append(positionText.formatted(Formatting.DARK_GRAY, Formatting.BOLD, Formatting.ITALIC));
            }
        }

        return text;
    }

    /**
     * Format time as an absolute time.
     *
     * @param timer The time to use.
     * @param position The track position.
     */
    public static Text scoreboardAbsolute(Long timer, int position) {
        MutableText timeText = Text.literal(TimeUtils.formatTime(timer));

        if (position == 0) {
            return timeText.formatted(Formatting.YELLOW);
        } else {
            return timeText.formatted(Formatting.WHITE);
        }
    }

    /**
     * Format time as a relative time.
     *
     * @param delta The time to use.
     * @param position The track position.
     */
    public static Text scoreboardRelative(Long delta, int position) {
        MutableText timeText = Text.literal(TimeUtils.formatTime(
                delta,
                EnumSet.of(TimeUtils.Selector.SECONDS, TimeUtils.Selector.MILLISECONDS),
                EnumSet.allOf(TimeUtils.Selector.class)));

        if (delta > 0) {
            return Text.literal("+").append(timeText).formatted(Formatting.RED);
        } else if (delta < 0) {
            return Text.literal("-").append(timeText).formatted(Formatting.BLUE);
        } else {
            return Text.empty();
        }
    }

    /**
     * Format player name as text.
     *
     * @param player The player the position belongs to.
     * @param curPlayer The player to be highlighted.
     * @param position The track position.
     */
    public static Text scoreboardName(BoatRacePlayer player, BoatRacePlayer curPlayer, int position) {
        MutableText nameText = Text.literal(player.offlineName());

        if (position == 0) {
            return nameText.formatted(Formatting.YELLOW, Formatting.BOLD);
        } else {
            if (player.equals(curPlayer)) {
                return nameText.formatted(Formatting.BOLD);
            } else {
                return nameText.formatted(Formatting.GRAY);
            }
        }
    }

    /**
     * Return a list of objects surrounding an index paired with its original index
     * positions. Null elements indicate when there were more items that didnt get
     * included in the final list.
     *
     * @param list     The list to use.
     * @param index    The index to use for comparisons.
     * @param top      The top indices to include.
     * @param compared The indices ahead and behind to use.
     * @return The computed list with null for padding.
     */
    public static <T> List<@Nullable Pair<Integer, T>> scoreboardAroundAndTop(List<T> list,
            int index, int top, int compared) {
        List<Pair<Integer, T>> around = new ObjectArrayList<>();

        // add padding if theres indices before the top for some reason
        if (top <= 0 && !list.isEmpty()) {
            around.add(null);
        }

        // add top values
        for (Pair<Integer, T> pair : WidgetTextUtil.scoreboardAround(list, 0, top - 1)) {
            around.add(pair);
        }

        // add padding if theres indices between overlaps
        if (index > 0 && index - compared > top) {
            around.add(null);
        }

        // show indices around the player
        if (index > 0 && index + compared > top - 1) {
            for (Pair<Integer, T> pair : WidgetTextUtil.scoreboardAround(list, index, compared)) {
                // skip overlaps from top
                if (pair.getLeft() > top - 1) {
                    around.add(pair);
                }
            }
        }

        // add padding at the end if needed
        int lastDisplayed = Math.max(index + compared, top - 1);
        if (list.size() - 1 > lastDisplayed) {
            around.add(null);
        }

        return around;
    }

    /**
     * Return a list of objects surrounding an index paired with its original index
     * positions.
     *
     * @param list  The list to choose from
     * @param at    The index to search from.
     * @param range The range before and after the index to fetch.
     * @return The computed list.
     */
    public static <T> List<Pair<Integer, T>> scoreboardAround(List<T> list, int at, int range) {
        List<Pair<Integer, T>> around = new ObjectArrayList<>();

        if (list == null || list.isEmpty()) {
            return around;
        }

        if (at < 0) {
            at = 0;
        }

        if (at >= list.size()) {
            at = list.size() - 1;
        }

        int from = Math.max(0, at - range);
        int to = Math.min(list.size(), at + range + 1);

        for (int i = from; i < to; i++) {
            around.add(new Pair<>(i, list.get(i)));
        }

        return around;
    }
}
