package com.abaan404.boatrace.game.gameplay;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

/**
 * Records splits and times an active run.
 */
public class SplitsManager {
    private Set<PlayerRef> running = new ObjectOpenHashSet<>();

    private Map<PlayerRef, Long> timer = new Object2LongOpenHashMap<>();
    private Map<PlayerRef, List<Long>> splits = new Object2ObjectOpenHashMap<>();

    /**
     * Continuously run the timer for this player.
     *
     * @param player The player.
     */
    public void run(PlayerRef player) {
        this.running.add(player);

        // reset everything but the lap count.
        this.timer.put(player, 0l);
        this.splits.remove(player);
    }

    /**
     * Stop running the timer for this player.
     *
     * @param player The player.
     */
    public void stop(PlayerRef player) {
        this.running.remove(player);

        // reset everything but the lap count.
        this.timer.put(player, 0l);
        this.splits.remove(player);
    }

    /**
     * Update every player's timer according to the world's tick rate.
     *
     * @param world The world to fetch mspt from.
     */
    public void tick(ServerWorld world) {
        for (PlayerRef player : this.running) {
            long timer = this.timer.getOrDefault(player, 0l);
            timer += world.getTickManager().getMillisPerTick();
            this.timer.put(player, timer);
        }
    }

    /**
     * Records a split.
     *
     * @param player The player to record.
     * @return The recorded split time.
     */
    public long recordSplit(PlayerRef player) {
        long lap = this.timer.getOrDefault(player, 0l);

        if (!this.splits.containsKey(player)) {
            List<Long> splits = new LongArrayList();
            splits.add(lap);

            this.splits.put(player, splits);

            return lap;
        }

        List<Long> splits = this.splits.get(player);
        splits.add(lap);

        return lap;
    }

    /**
     * Resets the internal timer and splits.
     *
     * @param player The player to reset for.
     * @return The last timer before a reset.
     */
    public long reset(PlayerRef player) {
        long timer = this.timer.getOrDefault(player, 0l);
        this.timer.remove(player);
        this.splits.remove(player);

        return timer;
    }

    /**
     * Get the current splits for a player.
     *
     * @param player The player the get splits for.
     * @return The player's splits.
     */
    public List<Long> getSplits(PlayerRef player) {
        return Collections.unmodifiableList(this.splits.getOrDefault(player, LongArrayList.of()));
    }

    /**
     * Get the timer for a player.
     *
     * @param player The player the get timer for.
     * @return The player's time.
     */
    public long getTimer(PlayerRef player) {
        return this.timer.getOrDefault(player, 0l);
    }
}
