package com.abaan404.boatrace.compat.openboatutils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringIdentifiable;

public record OBUTrackConfig(
        Optional<BlockSettings> settings,
        Optional<List<Mode>> mode,
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
        Optional<Boolean> tenStepInterpolation) {

    public static final Codec<OBUTrackConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockSettings.CODEC.optionalFieldOf("settings").forGetter(OBUTrackConfig::settings),
            Mode.CODEC.listOf().optionalFieldOf("mode").forGetter(OBUTrackConfig::mode),
            Codec.FLOAT.optionalFieldOf("swim_force").forGetter(OBUTrackConfig::swimForce),
            Codec.FLOAT.optionalFieldOf("step_height").forGetter(OBUTrackConfig::stepHeight),
            Codec.DOUBLE.optionalFieldOf("gravity").forGetter(OBUTrackConfig::gravity),
            Codec.INT.optionalFieldOf("coyote_time").forGetter(OBUTrackConfig::coyoteTime),
            Codec.BOOL.optionalFieldOf("fall_damage").forGetter(OBUTrackConfig::fallDamage),
            Codec.BOOL.optionalFieldOf("air_control").forGetter(OBUTrackConfig::airControl),
            Codec.BOOL.optionalFieldOf("water_elevation").forGetter(OBUTrackConfig::waterElevation),
            Codec.BOOL.optionalFieldOf("acceleration_stacking").forGetter(OBUTrackConfig::accelerationStacking),
            Codec.BOOL.optionalFieldOf("underwater_control").forGetter(OBUTrackConfig::underwaterControl),
            Codec.BOOL.optionalFieldOf("surface_water_control").forGetter(OBUTrackConfig::surfaceWaterControl),
            Codec.BOOL.optionalFieldOf("water_jumping").forGetter(OBUTrackConfig::waterJumping),
            Codec.BOOL.optionalFieldOf("air_stepping").forGetter(OBUTrackConfig::airStepping),
            Codec.BOOL.optionalFieldOf("ten_step_interpolation").forGetter(OBUTrackConfig::tenStepInterpolation))
            .apply(instance, OBUTrackConfig::new));

    public record BlockSettings(
            Map<BlockSettingType, Float> defaults,
            Map<BlockSettingType, List<BlockSetting>> settings) {

        public static final Codec<BlockSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(BlockSettingType.CODEC, Codec.FLOAT).optionalFieldOf("defaults", Map.of())
                        .forGetter(BlockSettings::defaults),
                Codec.unboundedMap(BlockSettingType.CODEC, BlockSetting.CODEC.listOf())
                        .optionalFieldOf("settings", Map.of())
                        .forGetter(BlockSettings::settings))
                .apply(instance, BlockSettings::new));

        public record BlockSetting(List<String> blocks, float value) {
            public static final Codec<BlockSetting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().fieldOf("blocks").forGetter(BlockSetting::blocks),
                    Codec.FLOAT.fieldOf("value").forGetter(BlockSetting::value))
                    .apply(instance, BlockSetting::new));
        }
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
