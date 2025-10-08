package com.abaan404.boatrace.utils;

import java.util.List;
import java.util.stream.Collectors;

import com.abaan404.boatrace.leaderboard.PersonalBest;
import com.abaan404.boatrace.maps.TrackMap;

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
        return Text.literal(WidgetTextUtil.formatTime(timer, true))
                .formatted(Formatting.BOLD);
    }

    /**
     * Create a text to show the current timer along with splits.
     *
     * @param timer        The timer in ms.
     * @param currentSplit The current split in ms.
     * @param pbSplit      The best split in ms to be compared against.
     * @return The timer and splits text seperated by a symbol.
     */
    public static Text actionBarTimerDelta(long timer, long currentSplit, long pbSplit) {
        long delta = currentSplit - pbSplit;

        if (pbSplit > currentSplit) {
            return Text.empty()
                    .append(WidgetTextUtil.actionBarTimer(timer))
                    .append(Text.literal(" ▲ ").formatted(Formatting.BLUE))
                    .append(Text.literal(WidgetTextUtil.formatTime(delta)).formatted(Formatting.BLUE))
                    .formatted(Formatting.BOLD);
        }
        // slower
        else if (pbSplit < currentSplit) {
            return Text.empty()
                    .append(WidgetTextUtil.actionBarTimer(timer))
                    .append(Text.literal(" ▼ ").formatted(Formatting.RED))
                    .append(Text.literal(WidgetTextUtil.formatTime(delta)).formatted(Formatting.RED))
                    .formatted(Formatting.BOLD);
        }
        // equal
        else {
            return Text.empty()
                    .append(WidgetTextUtil.actionBarTimer(timer))
                    .append(Text.literal(" ◇ ").formatted(Formatting.GRAY))
                    .append(Text.literal(WidgetTextUtil.formatTime(delta)).formatted(Formatting.GRAY))
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
    public static List<Text> scoreboardTrackText(TrackMap.Meta meta) {
        List<Text> list = new ObjectArrayList<>();

        list.add(Text.empty());
        list.add(Text.literal(meta.name()).formatted(Formatting.BOLD));

        if (meta.authors().isEmpty()) {
            list.add(Text.literal(" - By Unknown Author(s)")
                    .formatted(Formatting.GRAY, Formatting.ITALIC));
        } else {
            list.add(Text.literal(" - By " + meta.authors().stream()
                    .collect(Collectors.joining(", ")))
                    .formatted(Formatting.GRAY, Formatting.ITALIC));
        }

        list.add(Text.empty());

        return list;
    }

    /**
     * Format a player's personal best to a line text thats ready to be shown on the
     * leaderboard.
     *
     * @param pb        The player's personal best.
     * @param position  The player's position in the leaderboard.
     * @param highlight Should this text be highlighted (bolded).
     * @return A text ready to display player's pb.
     */
    public static Text scoreboardLeaderboardText(PersonalBest pb, int position, boolean highlight) {
        MutableText text = Text.empty();

        String name = pb.offlineName();

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

            if (highlight) {
                text.append(nameText.formatted(Formatting.BOLD));
            } else {
                text.append(nameText.formatted(Formatting.GRAY));
            }
        }

        return text;
    }

    /**
     * Format the time into a string.
     *
     * @param time        The time in ms.
     * @param showMinutes If should show empty minutes (00:).
     * @return The formatted time.
     */
    public static String formatTime(long time, boolean showMinutes) {
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
    public static String formatTime(long time) {
        return formatTime(time, false);
    }
}
