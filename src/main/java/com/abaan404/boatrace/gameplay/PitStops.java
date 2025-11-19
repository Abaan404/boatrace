package com.abaan404.boatrace.gameplay;

import java.util.Map;
import java.util.Set;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.screen.PitBoxGui;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.network.ServerPlayerEntity;

public class PitStops {
    private Set<BoatRacePlayer> inPit = new ObjectOpenHashSet<>();
    private Map<BoatRacePlayer, Integer> pitCount = new Object2IntOpenHashMap<>();

    /**
     * Start a pitstop.
     *
     * @param player The player.
     */
    public void startPit(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        if (!this.inPit.contains(bPlayer)) {
            PitBoxGui gui = new PitBoxGui(player);
            gui.open();

            this.inPit.add(bPlayer);
        }
    }

    /**
     * Stop a pitstop. Will not update this player's pit count.
     *
     * @param player The player.
     */
    public void stopPit(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);
        this.inPit.remove(bPlayer);
    }

    /**
     * Finish a pitstop and increment the pit counter.
     *
     * @param player The player.
     */
    public boolean finishPit(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        if (!this.inPit.contains(bPlayer)) {
            return false;
        }

        this.pitCount.put(bPlayer, this.pitCount.getOrDefault(bPlayer, 0) + 1);
        this.inPit.remove(bPlayer);

        return true;
    }

    /**
     * Reset state for this player.
     *
     * @param player The player.
     */
    public void reset(BoatRacePlayer player) {
        this.inPit.remove(player);
        this.pitCount.remove(player);
    }

    /**
     * Get the number of completed pits for this player.
     *
     * @param player The player.
     * @return Their pits.
     */
    public int getPitCount(BoatRacePlayer player) {
        return this.pitCount.getOrDefault(player, 0);
    }
}
