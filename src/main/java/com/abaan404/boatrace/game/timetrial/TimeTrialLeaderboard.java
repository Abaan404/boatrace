package com.abaan404.boatrace.game.timetrial;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.abaan404.boatrace.BoatRace;
import com.abaan404.boatrace.game.maps.TrackMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

/**
 * Holds the leaderboard for a track stored persistently.
 */
public record TimeTrialLeaderboard(Map<String, List<PersonalBest>> leaderboard) {
    public static void initialize() {
    }

    public static final Codec<TimeTrialLeaderboard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Codec.STRING,
                    PersonalBest.CODEC.listOf())
                    .fieldOf("leaderboard").forGetter(TimeTrialLeaderboard::leaderboard))
            .apply(instance, TimeTrialLeaderboard::new));

    public static final AttachmentType<TimeTrialLeaderboard> ATTACHMENT = AttachmentRegistry.create(
            Identifier.of(BoatRace.ID, "time_trial_leaderboard"), builder -> builder
                    .initializer(() -> new TimeTrialLeaderboard(Map.of()))
                    .persistent(TimeTrialLeaderboard.CODEC));

    /**
     * Get the track's leaderboard sorted by time.
     *
     * @param track The track.
     * @return A map of personal bests.
     */
    public List<PersonalBest> getTrackLeaderboard(TrackMap track) {
        return this.leaderboard.getOrDefault(String.valueOf(track.hashCode()), List.of());
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
                .getOrDefault(String.valueOf(track.hashCode()), List.of()).stream()
                .filter(pair -> pair.id().equals(playerUuid))
                .findFirst()
                .orElse(PersonalBest.EMPTY);
    }

    /**
     * Sets the personal best for this player on this track.
     *
     * @param track        The track.
     * @param personalBest Personal best to create or overwrite.
     * @return A new leaderboard with the new personal best.
     */
    public TimeTrialLeaderboard setPersonalBest(TrackMap track, PersonalBest personalBest) {
        // invalid number of splits, reject
        if (personalBest.splits().size() != track.getRegions().checkpoints().size()) {
            return this;
        }

        List<PersonalBest> newTrackLeaderboard = new ObjectArrayList<>(this.getTrackLeaderboard(track));

        newTrackLeaderboard.removeIf(pb -> pb.id().equals(personalBest.id()));
        newTrackLeaderboard.add(personalBest);

        newTrackLeaderboard.sort((a, b) -> Float.compare(a.timer(), b.timer()));

        Map<String, List<PersonalBest>> newLeaderboard = new Object2ObjectOpenHashMap<>(this.leaderboard);
        newLeaderboard.put(String.valueOf(track.hashCode()), newTrackLeaderboard);

        return new TimeTrialLeaderboard(newLeaderboard);
    }

    /**
     * Stores the player's time and splits and their info.
     */
    public record PersonalBest(String offlineName, UUID id, float timer, List<Float> splits) {
        public static final PersonalBest EMPTY = new PersonalBest("Herobrine", UUID.randomUUID(), Float.NaN, List.of());

        public static final Codec<PersonalBest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("offlineName").forGetter(PersonalBest::offlineName),
                Uuids.CODEC.fieldOf("uuid").forGetter(PersonalBest::id),
                Codec.FLOAT.fieldOf("timer").forGetter(PersonalBest::timer),
                Codec.FLOAT.listOf().fieldOf("splits").forGetter(PersonalBest::splits))
                .apply(instance, PersonalBest::new));
    }
}
