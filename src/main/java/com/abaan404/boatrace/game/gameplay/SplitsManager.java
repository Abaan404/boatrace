package com.abaan404.boatrace.game.gameplay;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

/**
 * Records splits and times an active run.
 */
public class SplitsManager {
    private Set<PlayerRef> running = new ObjectOpenHashSet<>();

    private Map<PlayerRef, Float> timer = new Object2FloatOpenHashMap<>();
    private Map<PlayerRef, List<Float>> splits = new Object2ObjectOpenHashMap<>();

    /**
     * Continuously run the timer for this player.
     *
     * @param player The player.
     */
    public void run(PlayerRef player) {
        this.running.add(player);
    }

    /**
     * Stop running the timer for this player.
     *
     * @param player The player.
     */
    public void stop(PlayerRef player) {
        this.running.remove(player);
    }

    /**
     * Update the timer according to the world's tick rate.
     *
     * @param player The player to update.
     * @param world  The world to fetch mspt from.
     */
    public void tick(PlayerRef player, ServerWorld world) {
        if (!this.running.contains(player)) {
            return;
        }

        float split = this.timer.getOrDefault(player, 0.0f);
        split += world.getTickManager().getMillisPerTick();
        this.timer.put(player, split);
    }

    /**
     * Records a split.
     *
     * @param player The player to record.
     * @return The recorded split time.
     */
    public float recordSplit(PlayerRef player) {
        float lap = this.timer.getOrDefault(player, 0.0f);

        if (!this.splits.containsKey(player)) {
            List<Float> splits = new FloatArrayList();
            splits.add(lap);

            this.splits.put(player, splits);

            return lap;
        }

        List<Float> splits = this.splits.get(player);
        splits.add(lap);

        return lap;
    }

    /**
     * Resets the internal timer and splits.
     *
     * @param player The player to reset for.
     * @return The last timer before a reset.
     */
    public float reset(PlayerRef player) {
        float timer = this.timer.getOrDefault(player, 0.0f);
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
    public List<Float> getSplits(PlayerRef player) {
        return Collections.unmodifiableList(this.splits.getOrDefault(player, List.of()));
    }

    /**
     * Get the timer for a player.
     *
     * @param player The player the get timer for.
     * @return The player's time.
     */
    public float getTimer(PlayerRef player) {
        return this.timer.getOrDefault(player, 0.0f);
    }
}
