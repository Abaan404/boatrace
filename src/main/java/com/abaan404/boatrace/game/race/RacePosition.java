package com.abaan404.boatrace.game.race;

import java.util.List;
import java.util.SortedMap;

import com.abaan404.boatrace.BoatRace;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.game.gameplay.CheckpointsManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Holds the leaderboard for a track stored persistently.
 */
public record RacePosition(List<BoatRacePlayer> positions) {
    public static void initialize() {
    }

    public static final Codec<RacePosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BoatRacePlayer.CODEC.listOf().fieldOf("positions").forGetter(RacePosition::positions))
            .apply(instance, RacePosition::new));

    public static final AttachmentType<RacePosition> ATTACHMENT = AttachmentRegistry.create(
            Identifier.of(BoatRace.ID, "leaderboard"), builder -> builder
                    .initializer(() -> new RacePosition(List.of()))
                    .persistent(RacePosition.CODEC));

    /**
     * Update the track race positions according to the number of checkpoints crossed.
     *
     * @param world       The world the race is held in.
     * @param checkpoints The cehckpoint manager to fetch lap and checkpoints.
     * @param players     The players participanting.
     * @return The updated race positions with players sorted according to their
     *         number of laps and checkpoints.
     */
    public RacePosition update(ServerWorld world, CheckpointsManager checkpoints, List<BoatRacePlayer> players) {
        SortedMap<Integer, BoatRacePlayer> playerCheckpoints = new Int2ObjectRBTreeMap<>();

        for (BoatRacePlayer player : players) {
            int laps = checkpoints.getLaps(player);
            int checkpoint = Math.max(0, checkpoints.getCheckpointIndex(player));

            playerCheckpoints.put(laps * (checkpoint + 1), player);
        }

        return update(world, playerCheckpoints.values().stream()
                .collect(ObjectArrayList.toList()));
    }

    /**
     * Update the track race positions.
     *
     * @param world   The world the race is held in.
     * @param players The players participanting.
     * @return The updated race positions.
     */
    public RacePosition update(ServerWorld world, List<BoatRacePlayer> players) {
        return world.setAttached(RacePosition.ATTACHMENT, new RacePosition(players));
    }
}
