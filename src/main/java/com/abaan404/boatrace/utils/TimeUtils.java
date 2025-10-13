package com.abaan404.boatrace.utils;

import java.time.Duration;
import java.util.EnumSet;

public final class TimeUtils {
    private TimeUtils() {
    }

    /**
     * Format the time into a string with chosen selectors to control formatting.
     * Hours will be omitted if its zero.
     *
     * @param time     The time in ms.
     * @param showZero Show the time component even if zero.
     * @param include  include this time component.
     * @return The formatted time.
     */
    public static String formatTime(long time, EnumSet<Selector> showZero, EnumSet<Selector> include) {
        Duration duration = Duration.ofMillis(Math.abs(time));

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        long millis = duration.toMillisPart();

        StringBuilder sb = new StringBuilder();

        boolean showHours = include.contains(Selector.HOURS)
                && (hours > 0 || showZero.contains(Selector.HOURS));
        boolean showMinutes = include.contains(Selector.MINUTES)
                && (minutes > 0 || showZero.contains(Selector.MINUTES) || showHours);
        boolean showSeconds = include.contains(Selector.SECONDS)
                && (seconds > 0 || showZero.contains(Selector.SECONDS) || showMinutes);
        boolean showMillis = include.contains(Selector.MILLISECONDS);

        if (showHours) {
            sb.append(hours);
        }

        if (showMinutes) {
            if (sb.length() > 0)
                sb.append(":");
            sb.append(String.format("%02d", minutes));
        }

        if (showSeconds) {
            if (sb.length() > 0)
                sb.append(":");
            sb.append(String.format("%02d", seconds));
        }

        if (showMillis) {
            if (sb.length() > 0)
                sb.append(".");
            sb.append(String.format("%03d", millis));
        }

        if (sb.length() == 0) {
            sb.append("0");
        }

        return sb.toString();
    }

    /**
     * Format the time into a string with default formatting (MM:SS:MS with optional
     * HH).
     *
     * @param time The time in ms
     * @return The formatted time.
     */
    public static String formatTime(long time) {
        return TimeUtils.formatTime(time, EnumSet.complementOf(EnumSet.of(Selector.HOURS)), EnumSet.allOf(Selector.class));
    }

    public enum Selector {
        HOURS,
        MINUTES,
        SECONDS,
        MILLISECONDS,
    }
}
