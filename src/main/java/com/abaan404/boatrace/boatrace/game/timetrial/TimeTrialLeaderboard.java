package com.abaan404.boatrace.boatrace.game.timetrial;

import com.abaan404.boatrace.boatrace.game.maps.TrackMap;

import it.unimi.dsi.fastutil.floats.Float2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectSortedMap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.PersistentState;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

/**
 * Holds the leaderboard for a track stored on disk.
 */
public class TimeTrialLeaderboard extends PersistentState {
    private final Float2ObjectSortedMap<PlayerRef> leaderboard;

    public TimeTrialLeaderboard(TrackMap map) {
        this.leaderboard = new Float2ObjectRBTreeMap<>();
        Random random = Random.create();

        this.leaderboard.put(60.0f, new PlayerRef(MathHelper.randomUuid(random)));
        this.leaderboard.put(120.0f, new PlayerRef(MathHelper.randomUuid(random)));
        this.leaderboard.put(3600.0f, new PlayerRef(MathHelper.randomUuid(random)));
        this.leaderboard.put(10.0f, new PlayerRef(MathHelper.randomUuid(random)));
    }

    /**
     * Submit a time.
     *
     * @param player  The player.
     * @param lapTime Their time.
     * @return success
     */
    public boolean submit(ServerPlayerEntity player, float lapTime) {
        PlayerRef ref = PlayerRef.of(player);

        float existingTime = Float.MAX_VALUE;
        for (var entry : this.leaderboard.float2ObjectEntrySet()) {
            if (entry.getValue().equals(ref)) {
                existingTime = entry.getFloatKey();
                break;
            }
        }

        if (lapTime >= existingTime) {
            return false;
        }

        this.leaderboard.remove(existingTime);
        this.leaderboard.put(lapTime, ref);

        return true;
    }

    /**
     * Get the leaderboard.
     *
     * @return the leaderboard.
     */
    public Float2ObjectSortedMap<PlayerRef> getLeaderboards() {
        return this.leaderboard;
    }
}
