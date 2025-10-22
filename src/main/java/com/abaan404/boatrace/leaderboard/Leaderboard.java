package com.abaan404.boatrace.leaderboard;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.abaan404.boatrace.BoatRace;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Holds the leaderboard for a track stored persistently.
 */
public record Leaderboard(Map<String, List<PersonalBest>> leaderboard) {
    public static void initialize() {
    }

    public static final Codec<Leaderboard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Codec.STRING,
                    PersonalBest.CODEC.listOf())
                    .fieldOf("leaderboard").forGetter(Leaderboard::leaderboard))
            .apply(instance, Leaderboard::new));

    public static final AttachmentType<Leaderboard> ATTACHMENT = AttachmentRegistry.create(
            Identifier.of(BoatRace.ID, "leaderboard"), builder -> builder
                    .initializer(() -> new Leaderboard(Map.of()))
                    .persistent(Leaderboard.CODEC));

    /**
     * Get the track's leaderboard sorted by time.
     *
     * @param track The track.
     * @return A map of personal bests.
     */
    public List<PersonalBest> getLeaderboard(BoatRaceTrack track) {
        return this.leaderboard.getOrDefault(String.valueOf(track.hashCode()), ObjectArrayList.of());
    }

    /**
     * Gets the personal best for this player on this track.
     *
     * @param track  The track.
     * @param player The player.
     * @return Their personal best.
     */
    public PersonalBest getPersonalBest(BoatRaceTrack track, BoatRacePlayer player) {
        return this.leaderboard
                .getOrDefault(String.valueOf(track.hashCode()), ObjectArrayList.of()).stream()
                .filter(pb -> pb.player().equals(player))
                .findFirst()
                .orElse(PersonalBest.of());
    }

    /**
     * Gets the position for this player on the track leaderboard.
     *
     * @param track  The track.
     * @param player The player.
     * @return Their position on the track. -1 if not found.
     */
    public int getLeaderboardPosition(BoatRaceTrack track, BoatRacePlayer player) {
        List<PersonalBest> trackLeaderboard = this.getLeaderboard(track);

        return IntStream.range(0, trackLeaderboard.size())
                .filter(i -> trackLeaderboard.get(i).player().equals(player))
                .findFirst()
                .orElse(-1);
    }

    /**
     * submits the personal best for this track if the run is valid.
     *
     * @param world        The world the leaderboard should be stored.
     * @param track        The track.
     * @param personalBest Personal best to submit.
     * @return A new leaderboard with the new personal best.
     */
    public Leaderboard trySubmit(ServerWorld world, BoatRaceTrack track, PersonalBest personalBest) {
        PersonalBest currentPersonalBest = this.getPersonalBest(track, personalBest.player());
        if (personalBest.timer() > currentPersonalBest.timer()) {
            // not a better pb
            return this;
        }

        // invalid pb, reject
        if (!Leaderboard.validate(track, personalBest)) {
            return this;
        }

        return this.submit(world, track, personalBest);
    }

    /**
     * submits the personal best for this track.
     *
     * @param track        The track.
     * @param personalBest Personal best to submit.
     * @return A new leaderboard with the new personal best.
     */
    public Leaderboard submit(ServerWorld world, BoatRaceTrack track, PersonalBest personalBest) {
        List<PersonalBest> newTrackLeaderboard = new ObjectArrayList<>(this.getLeaderboard(track));

        newTrackLeaderboard.removeIf(pb -> pb.player().equals(personalBest.player()));
        newTrackLeaderboard.add(personalBest);

        newTrackLeaderboard.sort((a, b) -> Long.compare(a.timer(), b.timer()));

        Map<String, List<PersonalBest>> newLeaderboardMap = new Object2ObjectOpenHashMap<>(this.leaderboard);
        newLeaderboardMap.put(String.valueOf(track.hashCode()), newTrackLeaderboard);

        Leaderboard newLeaderboard = new Leaderboard(newLeaderboardMap);
        world.setAttached(Leaderboard.ATTACHMENT, newLeaderboard);
        return newLeaderboard;
    }

    /**
     * Validates the run and checks if its a better run.
     *
     * @param track        The track the run belongs to.
     * @param personalBest The personal best to validate.
     * @return If the run was valid and is ready to be submitted.
     */
    public static boolean validate(BoatRaceTrack track, PersonalBest personalBest) {
        // NOTE: detect track limits using regions?

        switch (track.getMeta().layout()) {
            case CIRCULAR:
                // first checkpoint and last checkpoints are counted splits.
                if (personalBest.splits().size() != track.getRegions().checkpoints().size() + 1) {
                    BoatRace.LOGGER.warn("Invalid number of splits in the personal best ({}) for track \"{}\"",
                            personalBest.toString(), track.getMeta().name());
                    return false;
                }

                break;
            case LINEAR:
                if (personalBest.splits().size() != track.getRegions().checkpoints().size()) {
                    BoatRace.LOGGER.warn("Invalid number of splits in the personal best ({}) for track \"{}\"",
                            personalBest.toString(), track.getMeta().name());
                    return false;
                }

                break;
            default:
                break;
        }

        long prevSplit = 0l;
        for (long split : personalBest.splits()) {
            if (split < prevSplit) {
                BoatRace.LOGGER.warn("Splits dont ascend in the personal best ({}) for track \"{}\"",
                        personalBest.toString(), track.getMeta().name());
                return false;
            }

            prevSplit = split;
        }

        return true;
    }
}
