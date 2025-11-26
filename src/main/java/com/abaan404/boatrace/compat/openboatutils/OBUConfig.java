package com.abaan404.boatrace.compat.openboatutils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringIdentifiable;

public record OBUConfig(
        Optional<BlockSettings> settings,
        Optional<List<Mode>> mode,
        Optional<Collision> collision,
        Optional<Float> swimForce,
        Optional<Float> stepHeight,
        Optional<Double> gravity,
        Optional<Integer> coyoteTime,
        Optional<Boolean> fallDamage,
        Optional<Boolean> airControl,
        Optional<Boolean> waterElevation,
        Optional<Boolean> accelerationStacking,
        Optional<Boolean> underwaterControl,
        Optional<Boolean> surfaceWaterControl,
        Optional<Boolean> waterJumping,
        Optional<Boolean> airStepping,
        Optional<Boolean> tenStespInterpolation) {

    public static final Codec<OBUConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockSettings.CODEC.optionalFieldOf("settings").forGetter(OBUConfig::settings),
            Mode.CODEC.listOf().optionalFieldOf("mode").forGetter(OBUConfig::mode),
            Collision.CODEC.optionalFieldOf("collision").forGetter(OBUConfig::collision),
            Codec.FLOAT.optionalFieldOf("swim_force").forGetter(OBUConfig::swimForce),
            Codec.FLOAT.optionalFieldOf("step_height").forGetter(OBUConfig::stepHeight),
            Codec.DOUBLE.optionalFieldOf("gravity").forGetter(OBUConfig::gravity),
            Codec.INT.optionalFieldOf("coyote_time").forGetter(OBUConfig::coyoteTime),
            Codec.BOOL.optionalFieldOf("fall_damage").forGetter(OBUConfig::fallDamage),
            Codec.BOOL.optionalFieldOf("air_control").forGetter(OBUConfig::airControl),
            Codec.BOOL.optionalFieldOf("water_elevation").forGetter(OBUConfig::waterElevation),
            Codec.BOOL.optionalFieldOf("acceleration_stacking").forGetter(OBUConfig::accelerationStacking),
            Codec.BOOL.optionalFieldOf("underwater_control").forGetter(OBUConfig::underwaterControl),
            Codec.BOOL.optionalFieldOf("surface_water_control").forGetter(OBUConfig::surfaceWaterControl),
            Codec.BOOL.optionalFieldOf("water_jumping").forGetter(OBUConfig::waterJumping),
            Codec.BOOL.optionalFieldOf("air_stepping").forGetter(OBUConfig::airStepping),
            Codec.BOOL.optionalFieldOf("ten_steps_interpolation").forGetter(OBUConfig::tenStespInterpolation))
            .apply(instance, OBUConfig::new));

    public record BlockSettings(
            Map<BlockSettingType, Float> defaults,
            Map<BlockSettingType, List<BlockSetting>> settings) {

        public static final Codec<BlockSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(BlockSettingType.CODEC, Codec.FLOAT).fieldOf("defaults")
                        .forGetter(BlockSettings::defaults),
                Codec.unboundedMap(BlockSettingType.CODEC, BlockSetting.CODEC.listOf()).fieldOf("settings")
                        .forGetter(BlockSettings::settings))
                .apply(instance, BlockSettings::new));

        public record BlockSetting(List<String> blocks, float value) {
            public static final Codec<BlockSetting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().fieldOf("blocks").forGetter(BlockSetting::blocks),
                    Codec.FLOAT.fieldOf("value").forGetter(BlockSetting::value))
                    .apply(instance, BlockSetting::new));
        }
    }

    public record Collision(
            Optional<List<String>> filter,
            Optional<CollisionMode> mode,
            Optional<Byte> resolution) {

        public static final Codec<Collision> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("filter").forGetter(Collision::filter),
                CollisionMode.CODEC.optionalFieldOf("mode").forGetter(Collision::mode),
                Codec.BYTE.optionalFieldOf("resolution").forGetter(Collision::resolution))
                .apply(instance, Collision::new));
    }

    enum Mode implements StringIdentifiable {
        RALLY("rally"),
        RALLY_BLUE("rally_blue"),
        BA_NOFD("ba_nofd"),
        PARKOUR("parkour"),
        BA_BLUE_NOFD("ba_blue_nofd"),
        PARKOUR_BLUE("parkour_blue"),
        BA("ba"),
        BA_BLUE("ba_blue");

        public static final Codec<Mode> CODEC = StringIdentifiable.createCodec(Mode::values);

        private final String setting;

        private Mode(String setting) {
            this.setting = setting;
        }

        @Override
        public String asString() {
            return this.setting;
        }
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

    enum BlockSettingType implements StringIdentifiable {
        SLIPPERINESS("slipperiness"),
        JUMP_FORCE("jump_force"),
        FORWARDS_ACCELERATION("forwards_acceleration"),
        BACKWARDS_ACCELERATION("backwards_acceleration"),
        YAW_ACCELERATION("yaw_acceleration"),
        TURN_FORWARDS_ACCELERATION("turn_forwards_acceleration");

        public static final Codec<BlockSettingType> CODEC = StringIdentifiable
                .createCodec(BlockSettingType::values);

        private final String setting;

        private BlockSettingType(String setting) {
            this.setting = setting;
        }

        @Override
        public String asString() {
            return this.setting;
        }
    }
}
