package com.abaan404.boatrace.utils;

import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.game.race.RaceWidgets;
import com.abaan404.boatrace.leaderboard.PersonalBest;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;

/**
 * Common texts for widgets shared by each gamemode.
 */
public final class TextUtils {
    private TextUtils() {
    }

    public static final Component PAD_SCOREBOARD_POSITION = Component.literal("   ○ ○ ○").withStyle(ChatFormatting.DARK_GRAY);

    /**
     * Create a text for a countdown.
     *
     * @param countdown the current countdown in seconds.
     * @return A text to be displayed as a title.
     */
    public static Component titleCountdown(long countdown) {
        MutableComponent countdownText = Component.empty();

        if (countdown <= 0) {
            countdownText = Component.literal("Go!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else {
            countdownText = Component.literal(String.valueOf(countdown)).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC);
        }

        return Component.empty()
                .append(Component.literal(">> ").withStyle(ChatFormatting.GRAY))
                .append(countdownText)
                .append(Component.literal(" <<").withStyle(ChatFormatting.GRAY));
    }

    /**
     * A text to alert the player if theyre going backwards.
     *
     * @param reverse Is the direction reversed or not.
     * @return A pair of texts for a title and subtitle.
     */
    public static Tuple<Component, Component> titleAlertCheckpoint() {
        return new Tuple<>(
                Component.literal("⚠ Missed Checkpoint ⚠").withStyle(ChatFormatting.YELLOW),
                Component.literal("Respawn or go back!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    /**
     * Create a text for player position on the leaderboard.
     *
     * @param position The player's position to display.
     * @return A text if the position was valid otherwise an empty text.
     */
    public static Component actionBarPosition(int position) {
        return TextUtils.scoreboardPosition(false, position);
    }

    /**
     * Create a text to show remaining checkpoints.
     *
     * @param checkpoint     The current checkpoint
     * @param maxCheckpoints The total number of checkpoints
     * @return The text showing checkpoints.
     */
    public static Component actionBarCheckpoint(int checkpoint, int maxCheckpoints) {
        return Component.literal(String.format("(%d/%d)", checkpoint, maxCheckpoints))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD);
    }

    /**
     * Create a text to show the current timer.
     *
     * @param timer The time in ms.
     * @return The timer text.
     */
    public static Component actionBarTimer(long timer) {
        return Component.literal(TimeUtils.formatTime(timer)).withStyle(ChatFormatting.BOLD);
    }

    /**
     * Create a text to show the current delta.
     *
     * @param timer The timer in ms.
     * @param delta The delta for this split.
     * @return The timer and splits text seperated by a symbol.
     */
    public static Component actionBarDelta(long delta) {
        String deltaString = TimeUtils.formatTime(
                delta,
                EnumSet.of(TimeUtils.Selector.SECONDS, TimeUtils.Selector.MILLISECONDS),
                EnumSet.allOf(TimeUtils.Selector.class));

        // faster
        if (delta < 0) {
            return Component.empty()
                    .append(Component.literal("▲ ").withStyle(ChatFormatting.BLUE))
                    .append(Component.literal(deltaString).withStyle(ChatFormatting.BLUE))
                    .withStyle(ChatFormatting.BOLD);
        }
        // slower
        else if (delta > 0) {
            return Component.empty()
                    .append(Component.literal("▼ ").withStyle(ChatFormatting.RED))
                    .append(Component.literal(deltaString).withStyle(ChatFormatting.RED))
                    .withStyle(ChatFormatting.BOLD);
        }
        // equal
        else {
            return Component.empty()
                    .append(Component.literal("◇ ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(deltaString).withStyle(ChatFormatting.GRAY))
                    .withStyle(ChatFormatting.BOLD);
        }
    }

    /**
     * A consistent title for each game mode.
     *
     * @param mode The current mode.
     * @return The title text.
     */
    public static Component scoreboardTitleText(String mode) {
        return Component.literal("    ")
                .append(Component.literal("Boat").withStyle(ChatFormatting.RED))
                .append(Component.literal("Race").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC))
                .append(Component.literal(" ◇ ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(mode).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("    "))
                .withStyle(ChatFormatting.BOLD);
    }

    /**
     * Create lines of scoreboard texts for track metadata (i.e. authors, name, etc)
     *
     * @param meta The track meta.
     * @return A list of text for each line.
     */
    public static List<Component> scoreboardMeta(BoatRaceTrack.Meta meta) {
        List<Component> list = new ObjectArrayList<>();

        list.add(Component.literal(" ").append(meta.name()).withStyle(ChatFormatting.BOLD));

        List<String> authorLines = new ObjectArrayList<>();

        final int maxLength = 36;
        StringBuilder currentLine = new StringBuilder();

        // wrap author names
        for (int i = 0; i < meta.authors().size(); i++) {
            String next = new String();

            if (authorLines.isEmpty() && currentLine.isEmpty()) {
                next += "  - By ";
            }

            next += meta.authors().get(i) + (i != meta.authors().size() - 1 ? ", " : " ");

            if (currentLine.length() + next.length() > maxLength) {
                authorLines.add(currentLine.toString());
                currentLine = new StringBuilder("     " + next);
            } else {
                currentLine.append(next);
            }
        }

        if (currentLine.length() > 0) {
            authorLines.add(currentLine.toString());
        }

        for (String line : authorLines) {
            list.add(Component.literal(line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        return list;
    }

    /**
     * Format laps as text.
     *
     * @param laps    The current laps.
     * @param maxLaps The total laps
     * @return The duration text.
     */
    public static Component scoreboardLaps(int laps, int maxLaps) {
        return Component.empty()
                .append(Component.literal(" Laps: ").withStyle(ChatFormatting.RED))
                .append(Component.literal(String.valueOf(Math.min(laps, maxLaps))))
                .append(Component.literal(" / ").withStyle(ChatFormatting.ITALIC))
                .append(Component.literal(String.valueOf(maxLaps)));
    }

    /**
     * Format duration as text.
     *
     * @param duration    The elapsed time.
     * @param maxDuration The remaining time.
     * @return The duration text.
     */
    public static Component scoreboardDuration(long duration, long maxDuration) {
        return Component.empty()
                .append(Component.literal(" Duration: ").withStyle(ChatFormatting.RED))
                .append(Component.literal(TimeUtils.formatTime(
                        Math.min(duration, maxDuration),
                        EnumSet.complementOf(EnumSet.of(TimeUtils.Selector.HOURS)),
                        EnumSet.complementOf(EnumSet.of(TimeUtils.Selector.MILLISECONDS)))))
                .append(Component.literal(" / ").withStyle(ChatFormatting.ITALIC))
                .append(Component.literal(TimeUtils.formatTime(
                        maxDuration,
                        EnumSet.complementOf(EnumSet.of(TimeUtils.Selector.HOURS)),
                        EnumSet.complementOf(EnumSet.of(TimeUtils.Selector.MILLISECONDS)))));
    }

    /**
     * Format pits as text.
     *
     * @param pits    The player's current pits.
     * @param maxPits The total pits required.
     * @return The pits text.
     */
    public static Component scoreboardPits(int pits, int maxPits) {
        return Component.empty()
                .append(Component.literal(" Pits: ").withStyle(ChatFormatting.RED))
                .append(Component.literal(String.valueOf(Math.min(pits, maxPits))))
                .append(Component.literal(" / ").withStyle(ChatFormatting.ITALIC))
                .append(Component.literal(String.valueOf(maxPits)));
    }

    /**
     * Format pits as text for the leaderboard.
     *
     * @param pits The player's current pits.
     * @return The pits text.
     */
    public static Component scoreboardLeaderboardPits(int pits) {
        return Component.empty()
                .append("| ")
                .append(Component.literal(String.valueOf(pits)))
                .withStyle(ChatFormatting.GRAY);
    }

    /**
     * Format position as text.
     *
     * @param highlighted Should this be highlighted.
     * @param position    The track position.
     * @return The position text.
     */
    public static Component scoreboardPosition(boolean highlighted, int position) {
        MutableComponent text = Component.empty();

        MutableComponent P = Component.literal("P");
        MutableComponent positionText = Component.literal(String.valueOf(position + 1));

        if (position == 0) {
            text.append(P.withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            text.append(positionText.withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC));
        } else {
            if (position == 1) {
                text.append(P.withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
                text.append(positionText.withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC));
            } else if (position == 2) {
                text.append(P.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                text.append(positionText.withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC));
            } else if (highlighted) {
                text.append(P.withStyle(ChatFormatting.BOLD));
                text.append(positionText.withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC));
            } else {
                text.append(P.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
                text.append(positionText.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD, ChatFormatting.ITALIC));
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
    public static Component scoreboardAbsolute(long timer, int position) {
        MutableComponent timeText = Component.literal(TimeUtils.formatTime(timer));

        if (position == 0) {
            return timeText.withStyle(ChatFormatting.YELLOW);
        } else {
            return timeText.withStyle(ChatFormatting.WHITE);
        }
    }

    /**
     * Format time as a relative time.
     *
     * @param delta The time to use.
     */
    public static Component scoreboardRelative(long delta) {
        MutableComponent timeText = Component.literal(TimeUtils.formatTime(
                delta,
                EnumSet.of(TimeUtils.Selector.SECONDS, TimeUtils.Selector.MILLISECONDS),
                EnumSet.allOf(TimeUtils.Selector.class)));

        if (delta > 0) {
            return Component.literal("+").append(timeText).withStyle(ChatFormatting.RED);
        } else if (delta < 0) {
            return Component.literal("-").append(timeText).withStyle(ChatFormatting.BLUE);
        } else {
            return Component.literal("=").append(timeText).withStyle(ChatFormatting.GRAY);
        }
    }

    /**
     * Format player name as text.
     *
     * @param player      The player the position belongs to.
     * @param teamConfig  The player's team config.
     * @param highlighted Should this be highlighted.
     * @param position    The track position.
     */
    public static Component scoreboardName(BoatRacePlayer player, GameTeamConfig teamConfig, boolean highlighted,
            int position) {
        MutableComponent nameText = Component.empty()
                .append(teamConfig.prefix())
                .append(player.offlineName());

        if (position == 0) {
            return nameText.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        } else {
            if (highlighted) {
                return nameText.withStyle(ChatFormatting.BOLD);
            } else {
                return nameText.withStyle(ChatFormatting.GRAY);
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
    public static <T> List<@Nullable Tuple<Integer, T>> scoreboardAroundAndTop(List<T> list,
            int index, int top, int compared) {
        List<Tuple<Integer, T>> around = new ObjectArrayList<>();

        // add padding if theres indices before the top for some reason
        if (top <= 0 && !list.isEmpty()) {
            around.add(null);
        }

        // add top values
        for (Tuple<Integer, T> pair : TextUtils.scoreboardAround(list, 0, top - 1)) {
            around.add(pair);
        }

        // add padding if theres indices between overlaps
        if (index > 0 && index - compared > top) {
            around.add(null);
        }

        // show indices around the player
        if (index > 0 && index + compared > top - 1) {
            for (Tuple<Integer, T> pair : TextUtils.scoreboardAround(list, index, compared)) {
                // skip overlaps from top
                if (pair.getA() > top - 1) {
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
    public static <T> List<Tuple<Integer, T>> scoreboardAround(List<T> list, int at, int range) {
        List<Tuple<Integer, T>> around = new ObjectArrayList<>();

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
            around.add(new Tuple<>(i, list.get(i)));
        }

        return around;
    }

    /**
     * A text to display the lap delta for a finisher.
     *
     * @param leadingLaps The leader's laps.
     * @param currentLaps The current player's laps.
     * @return A text.
     */
    public static Component chatLapsDelta(int leadingLaps, int currentLaps) {
        int lapDelta = leadingLaps - currentLaps;
        return Component.literal(String.format("+%d Lap(s)", lapDelta)).withStyle(ChatFormatting.BOLD);
    }

    /**
     * A text to display a new best time.
     *
     * @param pb       The player's pb.
     * @param position The player's position.
     * @return A text
     */
    public static Component chatNewPersonalBest(PersonalBest pb, int position) {
        return Component.empty()
                .append(Component.literal(" >> ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("[").withStyle(ChatFormatting.RED))
                .append(Component.literal(pb.player().offlineName()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal("]  ").withStyle(ChatFormatting.RED))
                .append(TextUtils.scoreboardPosition(true, position))
                .append(Component.literal(" ◇ ").withStyle(ChatFormatting.BOLD))
                .append(Component.literal(TimeUtils.formatTime(pb.timer())).withStyle(ChatFormatting.BOLD));
    }

    /**
     * A text to display a new best time.
     *
     * @param pb The player's pb.
     * @return A text
     */
    public static Component chatNewFastestLap(PersonalBest pb) {
        return Component.empty()
                .append(Component.literal(" >> ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("[").withStyle(ChatFormatting.RED))
                .append(Component.literal(pb.player().offlineName()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal("]  ").withStyle(ChatFormatting.RED))
                .append(Component.literal(TimeUtils.formatTime(pb.timer())).withStyle(ChatFormatting.BOLD));
    }

    /**
     * A text to display a new time.
     *
     * @param timer The player's timer.
     * @return A text
     */
    public static Component chatNewTime(long timer) {
        return Component.empty()
                .append(Component.literal(" >> ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal(TimeUtils.formatTime(timer)).withStyle(ChatFormatting.GRAY,
                        ChatFormatting.ITALIC));
    }

    /**
     * A text to show leaderboard type.
     *
     * @param leaderboardType The leaderboard type.
     * @return A text with the formatted message.
     */
    public static Component chatLeaderboardType(RaceWidgets.LeaderboardType leaderboardType) {
        return Component.empty()
                .append(Component.literal(" >> ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal(leaderboardType.toString()).withStyle(ChatFormatting.ITALIC));
    }

    /**
     * A text to show winning points.
     *
     * @param points The points.
     * @return The formatted text.
     */
    public static Component chatPoints(int points) {
        return Component.literal(String.format("+%d", points)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    /**
     * A text to show track info in chat.
     *
     * @param meta The track's metadata.
     * @return A list of chat message(s).
     */
    public static List<Component> chatMeta(BoatRaceTrack.Meta meta) {
        List<Component> lines = new ObjectArrayList<>();

        MutableComponent titleText = Component.empty()
                .append(Component.literal(meta.name()).withStyle(ChatFormatting.BOLD));

        meta.url().ifPresent(url -> titleText.setStyle(Style.EMPTY
                .applyFormats(ChatFormatting.BLUE, ChatFormatting.UNDERLINE)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                .withHoverEvent(new HoverEvent.ShowText(Component.nullToEmpty("Open track's website.")))));

        Component authorText = Component.empty()
                .append("By ")
                .append(String.join(", ", meta.authors()))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

        lines.add(Component.empty()
                .append(Component.literal(" >> ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(titleText)
                .append(Component.nullToEmpty(" "))
                .append(authorText));

        meta.description().ifPresent(description -> {
            lines.add(Component.empty()
                    .append(Component.literal(" >> ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                    .append(Component.literal(description).withStyle(ChatFormatting.GRAY)));
        });

        return lines;
    }

    /**
     * Announce that players are on their final laps.
     *
     * @return The text.
     */
    public static Component chatFinalLap() {
        return Component.empty()
                .append(Component.literal(" >> ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("The leader has finished their race, this is your final lap."));
    }

    /**
     * Show the time taken for a pit stop.
     *
     * @return The text.
     */
    public static Component chatPitTime(long duration, boolean valid) {
        MutableComponent text = Component.empty()
                .append(Component.literal(" >> ").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD))
                .append(Component.literal("PitStop: "))
                .append(Component.literal(TimeUtils.formatTime(duration)).withStyle(ChatFormatting.BOLD));

        if (!valid) {
            text.append(Component.literal(" (invalidated)").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        return text;
    }
}
