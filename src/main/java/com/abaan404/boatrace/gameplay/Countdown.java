package com.abaan404.boatrace.gameplay;

import net.minecraft.server.world.ServerWorld;

public class Countdown {
    public final long COUNTDOWN_DIVISOR = 1000;

    private long countdown;
    private long goCountdown;

    public Countdown(long countdown, long goCountdown) {
        this.countdown = countdown;
        this.goCountdown = goCountdown;
    }

    /**
     * Tick the countdown.
     *
     * @param world The world to get tick info.
     * @return The result of this tick.
     */
    public TickResult tick(ServerWorld world) {
        if (this.countdown + this.goCountdown <= 0) {
            return TickResult.IDLE;
        }

        if (this.countdown > 0) {
            this.countdown -= world.getTickManager().getMillisPerTick();
        } else {
            this.goCountdown -= world.getTickManager().getMillisPerTick();

            if (this.countdown + this.goCountdown <= 0) {
                return TickResult.FINISH;
            }

        }

        return TickResult.COUNTDOWN;
    }

    /**
     * Get the current countdown in ms divided by a divisor. Freezes on 1 whenever
     * its waiting on the go counter.
     *
     * @return The countdown.
     */
    public long getCountdown() {
        if (this.countdown <= 0 && this.goCountdown > 0) {
            // goCountdown is not visible outside this class, return a constant non zero
            return 1;
        }

        // round to the nearest second
        return Math.ceilDiv(this.countdown, COUNTDOWN_DIVISOR);
    }

    public enum TickResult {
        /**
         * The timer is counting down.
         */
        COUNTDOWN,

        /**
         * The timer has finished counting down.
         */
        FINISH,

        /**
         * Doing nothing.
         */
        IDLE,
    }
}
