package com.abaan404.boatrace.compat.openboatutils;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringIdentifiable;

public record OBUGameConfig(
        Optional<Collision> collision) {

    public static final Codec<OBUGameConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Collision.CODEC.optionalFieldOf("collision").forGetter(OBUGameConfig::collision))
            .apply(instance, OBUGameConfig::new));

    public record Collision(
            List<String> filter,
            CollisionMode mode,
            Byte resolution) {

        public static final Codec<Collision> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("filter", List.of()).forGetter(Collision::filter),
                CollisionMode.CODEC.optionalFieldOf("mode", CollisionMode.VANILLA).forGetter(Collision::mode),
                Codec.BYTE.optionalFieldOf("resolution", (byte) 1).forGetter(Collision::resolution))
                .apply(instance, Collision::new));
    }

    enum CollisionMode implements StringIdentifiable {
        VANILLA("vanilla"),
        NO_COLLISION_WITH_BOATS_AND_PLAYERS("no_collision_with_boats_and_players"),
        NO_COLLISION_WITH_ANY_ENTITIES("no_collision_with_any_entities"),
        FILTERED_COLLISION("filtered_collision"),
        NO_COLLISION_WITH_BOATS_AND_PLAYERS_PLUS_FILTERED_COLLISION(
                "no_collision_with_boats_and_players_plus_filtered_collision");

        public static final Codec<CollisionMode> CODEC = StringIdentifiable.createCodec(CollisionMode::values);

        private final String setting;

        private CollisionMode(String setting) {
            this.setting = setting;
        }

        @Override
        public String asString() {
            return this.setting;
        }
    }
}
