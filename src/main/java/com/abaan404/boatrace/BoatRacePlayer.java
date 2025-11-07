package com.abaan404.boatrace;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Uuids;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public record BoatRacePlayer(PlayerRef ref, String offlineName) {
    public static final Codec<PlayerRef> CODEC_REF = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.CODEC.fieldOf("uuid").forGetter(PlayerRef::id))
            .apply(instance, PlayerRef::new));

    public static final BoatRacePlayer DEFAULT = new BoatRacePlayer(PlayerRef.ofUnchecked(UUID.randomUUID()), "Mumbo");

    public static final Codec<BoatRacePlayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CODEC_REF.fieldOf("player").forGetter(BoatRacePlayer::ref),
            Codec.STRING.fieldOf("offlineName").forGetter(BoatRacePlayer::offlineName))
            .apply(instance, BoatRacePlayer::new));

    public static BoatRacePlayer of(PlayerEntity player) {
        return BoatRacePlayer.of(player.getGameProfile());
    }

    public static BoatRacePlayer of(GameProfile profile) {
        return new BoatRacePlayer(PlayerRef.of(profile), profile.getName());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ref == null) ? 0 : ref.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BoatRacePlayer other = (BoatRacePlayer) obj;
        if (ref == null) {
            if (other.ref != null)
                return false;
        } else if (!ref.equals(other.ref))
            return false;
        return true;
    }
}
