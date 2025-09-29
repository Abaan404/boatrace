package com.abaan404.boatrace.boatrace.game.gameplay;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatCollection;
import it.unimi.dsi.fastutil.floats.FloatCollections;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.tick.TickManager;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

/**
 * Records splits and times an active run.
 */
public class SplitsManager {
    private Object2FloatMap<PlayerRef> timer = new Object2FloatOpenHashMap<>();
    private Object2ObjectMap<PlayerRef, FloatArrayList> splits = new Object2ObjectOpenHashMap<>();

    /**
     * Update the timer according to the server's tick rate.
     *
     * @param player The player to update.
     * @param tickManager The server's tick manager.
     */
    public void tick(ServerPlayerEntity player, TickManager tickManager) {
        PlayerRef ref = PlayerRef.of(player);

        float split = this.timer.getOrDefault(ref, 0.0f);
        split += tickManager.getMillisPerTick();
        this.timer.put(ref, split);
    }

    /**
     * Records a split.
     *
     * @param player The player to record.
     * @return The recorded split time.
     */
    public float recordSplit(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        float lap = this.timer.getOrDefault(ref, 0.0f);

        if (!this.splits.containsKey(ref)) {
            FloatArrayList splits = new FloatArrayList();
            splits.add(lap);
            this.splits.put(ref, splits);

            return lap;
        }

        FloatArrayList splits = this.splits.get(ref);
        splits.push(lap);

        return lap;
    }

    /**
     * Resets the internal timer and splits.
     *
     * @param player The player to reset for.
     * @return The last timer before a reset.
     */
    public float reset(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);

        float timer = this.timer.getOrDefault(ref, 0.0f);
        this.timer.removeFloat(ref);
        this.splits.remove(ref);

        return timer;
    }

    /**
     * Get the current splits for a player.
     *
     * @param player The player the get splits for.
     * @return The player's splits.
     */
    public FloatCollection getSplits(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        return FloatCollections.unmodifiable(this.splits.getOrDefault(ref, FloatArrayList.of()));
    }

    /**
     * Get the timer for a player.
     *
     * @param player The player the get timer for.
     * @return The player's time.
     */
    public float getTimer(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);

        return this.timer.getOrDefault(ref, 0.0f);
    }
}
