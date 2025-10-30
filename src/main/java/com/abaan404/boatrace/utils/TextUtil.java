package com.abaan404.boatrace.utils;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

/**
 * Common texts for widgets shared by each gamemode.
 */
public final class TextUtil {
    private TextUtil() {
    }

    public static final Text PAD_SCOREBOARD_POSITION = Text.literal("   ○ ○ ○").formatted(Formatting.DARK_GRAY);

    /**
     * Create a text for a countdown.
     *
     * @param countdown the current countdown in seconds.
     * @return A text to be displayed as a title.
     */
    public static Text titleCountdown(long countdown) {
        MutableText countdownText = Text.empty();

        if (countdown <= 0) {
            countdownText = Text.literal("Go!").formatted(Formatting.RED, Formatting.BOLD);
        } else {
            countdownText = Text.literal(String.valueOf(countdown)).formatted(Formatting.WHITE, Formatting.ITALIC);
        }

        return Text.empty()
                .append(Text.literal(">> ").formatted(Formatting.GRAY))
                .append(countdownText)
                .append(Text.literal(" <<").formatted(Formatting.GRAY));
    }

    /**
     * Create a text for player position on the leaderboard.
     *
     * @param position The player's position to display.
     * @return A text if the position was valid otherwise an empty text.
     */
    public static Text actionBarPosition(int position) {
        return TextUtil.scoreboardPosition(false, position);
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
     * Create a text to show the current delta.
     *
     * @param timer The timer in ms.
     * @param delta The delta for this split.
     * @return The timer and splits text seperated by a symbol.
     */
    public static Text actionBarDelta(long delta) {
        String deltaString = TimeUtils.formatTime(
                delta,
                EnumSet.of(TimeUtils.Selector.SECONDS, TimeUtils.Selector.MILLISECONDS),
                EnumSet.allOf(TimeUtils.Selector.class));

        // faster
        if (delta < 0) {
            return Text.empty()
                    .append(Text.literal("▲ ").formatted(Formatting.BLUE))
                    .append(Text.literal(deltaString).formatted(Formatting.BLUE))
                    .formatted(Formatting.BOLD);
        }
        // slower
        else if (delta > 0) {
            return Text.empty()
                    .append(Text.literal("▼ ").formatted(Formatting.RED))
                    .append(Text.literal(deltaString).formatted(Formatting.RED))
                    .formatted(Formatting.BOLD);
        }
        // equal
        else {
            return Text.empty()
                    .append(Text.literal("◇ ").formatted(Formatting.GRAY))
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
    public static List<Text> scoreboardMeta(BoatRaceTrack.Meta meta) {
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
     * Format laps as text.
     *
     * @param laps    The current laps.
     * @param maxLaps The total laps
     * @return The duration text.
     */
    public static Text scoreboardLaps(int laps, int maxLaps) {
        return Text.empty()
                .append(Text.literal(" Laps: ").formatted(Formatting.RED))
                .append(Text.literal(String.valueOf(laps)))
                .append(Text.literal(" / ").formatted(Formatting.ITALIC))
                .append(Text.literal(String.valueOf(maxLaps)));
    }

    /**
     * Format duration as text.
     *
     * @param duration    The elapsed time.
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
     * @param highlighted Should this be highlighted.
     * @param position    The track position.
     * @return The position text.
     */
    public static Text scoreboardPosition(boolean highlighted, int position) {
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
            } else if (highlighted) {
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
     * @param timer    The time to use.
     * @param position The track position.
     */
    public static Text scoreboardAbsolute(long timer, int position) {
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
     */
    public static Text scoreboardRelative(long delta) {
        MutableText timeText = Text.literal(TimeUtils.formatTime(
                delta,
                EnumSet.of(TimeUtils.Selector.SECONDS, TimeUtils.Selector.MILLISECONDS),
                EnumSet.allOf(TimeUtils.Selector.class)));

        if (delta > 0) {
            return Text.literal("+").append(timeText).formatted(Formatting.RED);
        } else if (delta < 0) {
            return Text.literal("-").append(timeText).formatted(Formatting.BLUE);
        } else {
            return Text.literal("=").append(timeText).formatted(Formatting.GRAY);
        }
    }

    /**
     * Format player name as text.
     *
     * @param player      The player the position belongs to.
     * @param highlighted Should this be highlighted.
     * @param position    The track position.
     */
    public static Text scoreboardName(BoatRacePlayer player, boolean highlighted, int position) {
        MutableText nameText = Text.literal(player.offlineName());

        if (position == 0) {
            return nameText.formatted(Formatting.YELLOW, Formatting.BOLD);
        } else {
            if (highlighted) {
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
        for (Pair<Integer, T> pair : TextUtil.scoreboardAround(list, 0, top - 1)) {
            around.add(pair);
        }

        // add padding if theres indices between overlaps
        if (index > 0 && index - compared > top) {
            around.add(null);
        }

        // show indices around the player
        if (index > 0 && index + compared > top - 1) {
            for (Pair<Integer, T> pair : TextUtil.scoreboardAround(list, index, compared)) {
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

    /**
     * A text to display a new best time.
     *
     * @param timer    The player's timer.
     * @param position The player's position.
     * @return A text
     */
    public static Text chatNewPersonalBest(long timer, int position) {
        return Text.empty()
                .append(Text.literal(" >> ").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("New Best Time!   ").formatted(Formatting.WHITE))
                .append(TextUtil.scoreboardPosition(true, position))
                .append(Text.literal(" ◇ ").formatted(Formatting.BOLD))
                .append(Text.literal(TimeUtils.formatTime(timer)).formatted(Formatting.BOLD));
    }

    /**
     * A text to display a new time.
     *
     * @param timer The player's timer.
     * @return A text
     */
    public static Text chatNewTime(long timer) {
        return Text.empty()
                .append(Text.literal(" >> ").formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal(TimeUtils.formatTime(timer)).formatted(Formatting.GRAY,
                        Formatting.ITALIC));
    }

    /**
     * A text to show in chat.
     *
     * @param message The message.
     * @return A text with the formatted message.
     */
    public static Text chat(MutableText message) {
        return Text.empty()
                .append(Text.literal(" >> ").formatted(Formatting.RED, Formatting.BOLD))
                .append(message.formatted(Formatting.ITALIC));
    }
}
