package com.abaan404.boatrace.gameplay;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;

public class Countdown {
    public final int COUNTDOWN_DIVISOR = 1000;

    private final Random random = Random.createLocal();

    private int countdown = -1;
    private int randomCountdown = -1;

    /**
     * Tick the countdown.
     *
     * @param world The world to get tick info.
     * @return The result of this tick.
     */
    public TickResult tick(ServerWorld world) {
        if (this.countdown + this.randomCountdown <= 0) {
            return TickResult.IDLE;
        }

        if (this.countdown > 0) {
            this.countdown -= world.getTickManager().getMillisPerTick();
        } else {
            this.randomCountdown -= world.getTickManager().getMillisPerTick();
        }

        if (this.countdown + this.randomCountdown <= 0) {
            return TickResult.FINISH;
        }

        return TickResult.COUNTDOWN;
    }

    /**
     * Set a linear and randomized countdown. Countdown will be held at 1 when the
     * linear countdown has finished but is still waiting on the randomized
     * countdown to finish.
     *
     * @param countdown       linear countdown in ms.
     * @param randomCountdown randomized countdown from 0 to this in ms
     */
    public void setCountdown(int countdown, int randomCountdown) {
        this.countdown = countdown;
        this.randomCountdown = this.random.nextBetween(0, randomCountdown);
    }

    /**
     * Get the current countdown in ms divided by a divisor. Freezes on 1 whenever
     * its waiting on the go counter.
     *
     * @return The countdown.
     */
    public int getCountdown() {
        if (this.countdown <= 0 && this.randomCountdown > 0) {
            // goCountdown is not visible outside this class, return a constant non zero
            return 1;
        }

        // round to the nearest second
        return Math.ceilDiv(this.countdown, COUNTDOWN_DIVISOR);
    }

    /**
     * If the timer has finished counting.
     *
     * @return If the timer has finished counting.
     */
    public boolean isCounting() {
        return this.countdown + this.randomCountdown > 0;
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
