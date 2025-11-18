package com.abaan404.boatrace.gameplay;

import java.util.Map;
import java.util.Set;

import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.screen.PitBoxGui;
import com.abaan404.boatrace.utils.TextUtils;

import eu.pb4.sgui.virtual.inventory.VirtualScreenHandler;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class PitStops {
    private final BoatRaceTrack track;

    private Map<BoatRacePlayer, Boolean> pitFinished = new Object2BooleanOpenHashMap<>();
    private Map<BoatRacePlayer, Long> pitDuration = new Object2LongOpenHashMap<>();
    private Set<BoatRacePlayer> pitGuiSent = new ObjectOpenHashSet<>();

    private Map<BoatRacePlayer, Vec3d> prevPositions = new Object2ObjectOpenHashMap<>();
    private Map<BoatRacePlayer, Integer> pitCount = new Object2IntOpenHashMap<>();

    public PitStops(BoatRaceTrack track) {
        this.track = track;
    }

    /**
     * Mark a player has having completed a pitstop.
     *
     * @param player The player.
     */
    public void pitFinished(BoatRacePlayer player, long duration) {
        this.pitFinished.putIfAbsent(player, false);
    }

    /**
     * Tick pit stops for this player.
     *
     * @param player The player.
     */
    public void tick(ServerPlayerEntity player) {
        BoatRacePlayer bPlayer = BoatRacePlayer.of(player);

        Vec3d pos = player.getEyePos();
        Vec3d prevPos = this.prevPositions.getOrDefault(bPlayer, pos);
        this.prevPositions.put(bPlayer, pos);

        BoatRaceTrack.Regions regions = this.track.getRegions();

        boolean pitGuiOpen = player.currentScreenHandler instanceof VirtualScreenHandler virt
                && virt.getGui() instanceof PitBoxGui;

        // not in pitlane
        if (!regions.pitLane().intersect(pos, prevPos)) {
            this.pitGuiSent.remove(bPlayer);
            this.pitFinished.remove(bPlayer);
        }

        // otherwise, only open if a window hasn't already been sent
        else if (!pitGuiOpen && !this.pitGuiSent.contains(bPlayer)) {
            PitBoxGui gui = new PitBoxGui(player);
            if (gui.open()) {
                this.pitGuiSent.add(bPlayer);
            }
        }

        if (this.pitFinished.getOrDefault(bPlayer, true)) {
            if (this.pitDuration.containsKey(bPlayer)) {
                player.sendMessage(TextUtils.chatPitTime(this.pitDuration.get(bPlayer)));
            }

            this.pitFinished.put(bPlayer, true);
            this.pitCount.put(bPlayer, this.pitCount.getOrDefault(bPlayer, 0));
        }
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
