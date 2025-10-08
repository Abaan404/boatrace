package com.abaan404.boatrace.leaderboard;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.abaan404.boatrace.BoatRace;
import com.abaan404.boatrace.maps.TrackMap;
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
    public List<PersonalBest> getTrackLeaderboard(TrackMap track) {
        return this.leaderboard.getOrDefault(String.valueOf(track.hashCode()), ObjectArrayList.of());
    }

    /**
     * Gets the personal best for this player on this track.
     *
     * @param track      The track.
     * @param playerUuid The player's uuid.
     * @return Their personal best.
     */
    public PersonalBest getPersonalBest(TrackMap track, UUID playerUuid) {
        return this.leaderboard
                .getOrDefault(String.valueOf(track.hashCode()), ObjectArrayList.of()).stream()
                .filter(pair -> pair.id().equals(playerUuid))
                .findFirst()
                .orElse(PersonalBest.of());
    }

    /**
     * Gets the position for this player on the track leaderboard.
     *
     * @param track      The track.
     * @param playerUuid The player's uuid.
     * @return Their position on the track. -1 if not found.
     */
    public int getTrackLeaderboardPosition(TrackMap track, UUID playerUuid) {
        List<PersonalBest> trackLeaderboard = this.getTrackLeaderboard(track);

        return IntStream.range(0, trackLeaderboard.size())
                .filter(i -> trackLeaderboard.get(i).id().equals(playerUuid))
                .findFirst()
                .orElse(-1);
    }

    /**
     * submits the personal best for this track if the run is valid.
     *
     * @param track        The track.
     * @param personalBest Personal best to submit.
     * @return A new leaderboard with the new personal best.
     */
    public Leaderboard trySubmit(ServerWorld world, TrackMap track, PersonalBest personalBest) {
        // invalid pb, reject
        if (!this.validatePersonalBest(track, personalBest)) {
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
    public Leaderboard submit(ServerWorld world, TrackMap track, PersonalBest personalBest) {
        List<PersonalBest> newTrackLeaderboard = new ObjectArrayList<>(this.getTrackLeaderboard(track));

        newTrackLeaderboard.removeIf(pb -> pb.id().equals(personalBest.id()));
        newTrackLeaderboard.add(personalBest);

        newTrackLeaderboard.sort((a, b) -> Float.compare(a.timer(), b.timer()));

        Map<String, List<PersonalBest>> newLeaderboard = new Object2ObjectOpenHashMap<>(this.leaderboard);
        newLeaderboard.put(String.valueOf(track.hashCode()), newTrackLeaderboard);

        return world.setAttached(Leaderboard.ATTACHMENT, new Leaderboard(newLeaderboard));
    }

    /**
     * Validates the run and checks if its a better run.
     *
     * @param track           The track the run belongs to.
     * @param newPersonalBest The new personal best to validate.
     * @return If the run was valid and is ready to be submitted.
     */
    private boolean validatePersonalBest(TrackMap track, PersonalBest newPersonalBest) {
        PersonalBest currentPersonalBest = this.getPersonalBest(track, newPersonalBest.id());
        if (newPersonalBest.timer() > currentPersonalBest.timer() && !Float.isNaN(newPersonalBest.timer())) {
            // not a better pb
            return false;
        }

        switch (track.getMeta().layout()) {
            case CIRCULAR:
                if (newPersonalBest.splits().size() != track.getRegions().checkpoints().size()) {
                    BoatRace.LOGGER.warn("Invalid number of splits in the personal best ({}) for track \"{}\"",
                            newPersonalBest.toString(), track.getMeta().name());
                    return false;
                }

                break;
            case LINEAR:
                if (newPersonalBest.splits().size() + 1 != track.getRegions().checkpoints().size()) {
                    BoatRace.LOGGER.warn("Invalid number of splits in the personal best ({}) for track \"{}\"",
                            newPersonalBest.toString(), track.getMeta().name());
                    return false;
                }

                break;
            default:
                break;
        }

        float prevFloat = 0.0f;
        for (float split : newPersonalBest.splits()) {
            if (split < prevFloat) {
                BoatRace.LOGGER.warn("Splits dont ascend in the personal best ({}) for track \"{}\"",
                        newPersonalBest.toString(), track.getMeta().name());
                return false;
            }

            if (split > newPersonalBest.timer()) {
                BoatRace.LOGGER.warn("A split was greater than the timer in the personal best ({}) for track \"{}\"",
                        newPersonalBest.toString(), track.getMeta().name());
                return false;
            }

            prevFloat = split;
        }

        return true;
    }
}
