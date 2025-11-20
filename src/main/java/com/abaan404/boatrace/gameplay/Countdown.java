package com.abaan404.boatrace.gameplay;

import java.util.Random;

import com.abaan404.boatrace.BoatRaceConfig;

import net.minecraft.server.world.ServerWorld;

public class Countdown {
    private final Random rand = new Random();

    private long duration = -1;
    private long random = -1;

    /**
     * Tick the countdown.
     *
     * @param world The world to get tick info.
     * @return The result of this tick.
     */
    public TickResult tick(ServerWorld world) {
        if (this.duration < 0 && this.random < 0) {
            return TickResult.IDLE;
        }

        if (this.duration >= 0) {
            this.duration -= world.getTickManager().getMillisPerTick();
        } else {
            this.random -= world.getTickManager().getMillisPerTick();
        }

        if (this.duration < 0 && this.random < 0) {
            return TickResult.FINISH;
        }

        return TickResult.COUNTDOWN;
    }

    /**
     * Set a linear and randomized countdown. Countdown will be held at 1 when the
     * linear countdown has finished but is still waiting on the randomized
     * countdown to finish.
     *
     * @param duration linear countdown in ms.
     * @param random   randomized countdown from 0 to this in ms
     */
    public void setCountdown(long duration, long random) {
        this.duration = duration;

        if (random > 0) {
            this.random = this.rand.nextLong(0, random);
        } else {
            this.random = random;
        }
    }

    /**
     * Set a linear and randomized countdown. Countdown will be held at 1 when the
     * linear countdown has finished but is still waiting on the randomized
     * countdown to finish.
     *
     * @param config A config with random and linear countdown
     */
    public void setCountdown(BoatRaceConfig.Countdown config) {
        this.setCountdown(config.duration(), config.random());
    }

    /**
     * Get the current countdown in ms. Freezes on 1 whenever its waiting on the
     * random counter.
     *
     * @return The countdown.
     */
    public long getCountdown() {
        // If duration finished but random still waiting, freeze at 1
        if (this.duration <= 0 && this.random > 0) {
            return 1;
        }

        return Math.max(this.duration, 0);
    }

    /**
     * If the timer has finished counting.
     *
     * @return If the timer has finished counting.
     */
    public boolean isCounting() {
        return this.duration >= 0 || this.random >= 0;
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
