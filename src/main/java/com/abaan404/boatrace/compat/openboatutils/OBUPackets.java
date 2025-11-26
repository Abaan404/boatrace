package com.abaan404.boatrace.compat.openboatutils;

import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Reference
// https://github.com/o7Moon/OpenBoatUtils/wiki/Packets/3e35b06a14fb6f989e5a773ded651e568fa42ee3
// https://github.com/o7Moon/OpenBoatUtils/wiki/Modes/898790e240e00bd1ff0fb5e1b0a951085ef7e7cf
public final class OBUPackets {
    public static final Identifier CHANNEL = Identifier.of("openboatutils", "settings");

    private OBUPackets() {
    }

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(
                ResetS2CPayload.ID,
                ResetS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetStepHeightS2CPayload.ID,
                SetStepHeightS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetDefaultSlipperinessS2CPayload.ID,
                SetDefaultSlipperinessS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetBlockSlipperinessS2CPayload.ID,
                SetBlockSlipperinessS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetBoatFallDamageS2CPayload.ID,
                SetBoatFallDamageS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetBoatWaterElevationS2CPayload.ID,
                SetBoatWaterElevationS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetBoatBoatAirControlS2CPayload.ID,
                SetBoatBoatAirControlS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetBoatJumpForceS2CPayload.ID,
                SetBoatJumpForceS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetModeS2CPayload.ID,
                SetModeS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetGravityS2CPayload.ID,
                SetGravityS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetYawAccelerationS2CPayload.ID,
                SetYawAccelerationS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetForwardAccelerationS2CPayload.ID,
                SetForwardAccelerationS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetBackwardAccelerationS2CPayload.ID,
                SetBackwardAccelerationS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetTurningForwardAccelerationS2CPayload.ID,
                SetTurningForwardAccelerationS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                AllowAccelerationStackingS2CPayload.ID,
                AllowAccelerationStackingS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                ResendVersionS2CPayload.ID,
                ResendVersionS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetUnderwaterControlS2CPayload.ID,
                SetUnderwaterControlS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetSurfaceWaterControlS2CPayload.ID,
                SetSurfaceWaterControlS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetExclusiveModeS2CPayload.ID,
                SetExclusiveModeS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetCoyoteTimeS2CPayload.ID,
                SetCoyoteTimeS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetWaterJumpingS2CPayload.ID,
                SetWaterJumpingS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetSwimForceS2CPayload.ID,
                SetSwimForceS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                ClearBlocksSlipperinessS2CPayload.ID,
                ClearBlocksSlipperinessS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                ClearAllSlipperinessS2CPayload.ID,
                ClearAllSlipperinessS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                ApplyModeSeriesS2CPayload.ID,
                ApplyModeSeriesS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                ApplyExclusiveModeSeriesS2CPayload.ID,
                ApplyExclusiveModeSeriesS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetPerBlockSettingS2CPayload.ID,
                SetPerBlockSettingS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetCollisionModeS2CPayload.ID,
                SetCollisionModeS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetAirSteppingS2CPayload.ID,
                SetAirSteppingS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetTenStepInterpolationS2CPayload.ID,
                SetTenStepInterpolationS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SetCollisionResolutionS2CPayload.ID,
                SetCollisionResolutionS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                AddToCollisionFilterS2CPayload.ID,
                AddToCollisionFilterS2CPayload.CODEC);
    }

    enum Mode {
        BROKEN_SLIME_RALLY(0),
        BROKEN_SLIME_RALLY_BLUE(1),
        BROKEN_SLIME_BA_NOFD(2),
        BROKEN_SLIME_PARKOUR(3),
        BROKEN_SLIME_BA_BLUE_NOFD(4),
        BROKEN_SLIME_PARKOUR_BLUE(5),
        BROKEN_SLIME_BA(6),
        BROKEN_SLIME_BA_BLUE(7),
        RALLY(8),
        RALLY_BLUE(9),
        BA_NOFD(10),
        PARKOUR(11),
        BA_BLUE_NOFD(12),
        PARKOUR_BLUE(13),
        BA(14),
        BA_BLUE(15),

        // extra
        UNKNOWN(16);

        private final short modeId;

        private Mode(int modeId) {
            this.modeId = (short) modeId;
        }

        public short id() {
            return this.modeId;
        }

        static public Mode fromId(int modeId) {
            if (modeId < 0 || modeId >= Mode.values().length) {
                return Mode.UNKNOWN;
            }

            return Mode.values()[modeId];
        }
    }

    enum BlockSetting {
        JUMP_FORCE(0),
        FORWARDS_ACCEL(1),
        BACKWARDS_ACCEL(2),
        YAW_ACCEL(3),
        TURN_FORWARDS_ACCEL(4),

        // extra
        UNKNOWN(5);

        private final short value;

        private BlockSetting(int setting) {
            this.value = (short) setting;
        }

        public short value() {
            return this.value;
        }

        static public BlockSetting fromValue(int value) {
            if (value < 0 || value >= BlockSetting.values().length) {
                return BlockSetting.UNKNOWN;
            }

            return BlockSetting.values()[value];
        }
    }

    enum CollisionMode {
        VANILLA(0),
        NO_COLLISION_WITH_BOATS_AND_PLAYERS(1),
        NO_COLLISION_WITH_ANY_ENTITIES(2),
        FILTERED_COLLISION(3),
        NO_COLLISION_WITH_BOATS_AND_PLAYERS_PLUS_FILTERED_COLLISION(3);

        private final short value;

        private CollisionMode(int setting) {
            this.value = (short) setting;
        }

        public short value() {
            return this.value;
        }

        static public CollisionMode fromValue(int value) {
            if (value < 0 || value >= CollisionMode.values().length) {
                return CollisionMode.VANILLA;
            }

            return CollisionMode.values()[value];
        }
    }

    public record ResetS2CPayload() implements CustomPayload {
        public static final CustomPayload.Id<ResetS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, ResetS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 0,
                ResetS2CPayload::new);

        private ResetS2CPayload(short id) {
            this();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetStepHeightS2CPayload(float stepHeight) implements CustomPayload {
        public static final CustomPayload.Id<SetStepHeightS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetStepHeightS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 1,
                PacketCodecs.FLOAT, SetStepHeightS2CPayload::stepHeight,
                SetStepHeightS2CPayload::new);

        private SetStepHeightS2CPayload(short id, float stepHeight) {
            this(stepHeight);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetDefaultSlipperinessS2CPayload(float defaultSlipperiness) implements CustomPayload {
        public static final CustomPayload.Id<SetDefaultSlipperinessS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetDefaultSlipperinessS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 2,
                PacketCodecs.FLOAT, SetDefaultSlipperinessS2CPayload::defaultSlipperiness,
                SetDefaultSlipperinessS2CPayload::new);

        private SetDefaultSlipperinessS2CPayload(short id, float defaultSlipperiness) {
            this(defaultSlipperiness);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetBlockSlipperinessS2CPayload(float slipperiness, List<String> blocks) implements CustomPayload {
        public static final CustomPayload.Id<SetBlockSlipperinessS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetBlockSlipperinessS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 3,
                PacketCodecs.FLOAT, SetBlockSlipperinessS2CPayload::slipperiness,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), SetBlockSlipperinessS2CPayload::blocks,
                SetBlockSlipperinessS2CPayload::new);

        private SetBlockSlipperinessS2CPayload(short id, float slipperiness, List<String> blocks) {
            this(slipperiness, blocks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetBoatFallDamageS2CPayload(boolean fallDamage) implements CustomPayload {
        public static final CustomPayload.Id<SetBoatFallDamageS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetBoatFallDamageS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 4,
                PacketCodecs.BOOLEAN, SetBoatFallDamageS2CPayload::fallDamage,
                SetBoatFallDamageS2CPayload::new);

        private SetBoatFallDamageS2CPayload(short id, boolean fallDamage) {
            this(fallDamage);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetBoatWaterElevationS2CPayload(boolean waterElevation) implements CustomPayload {
        public static final CustomPayload.Id<SetBoatWaterElevationS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetBoatWaterElevationS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 5,
                PacketCodecs.BOOLEAN, SetBoatWaterElevationS2CPayload::waterElevation,
                SetBoatWaterElevationS2CPayload::new);

        private SetBoatWaterElevationS2CPayload(short id, boolean waterElevation) {
            this(waterElevation);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetBoatBoatAirControlS2CPayload(boolean airControl) implements CustomPayload {
        public static final CustomPayload.Id<SetBoatBoatAirControlS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetBoatBoatAirControlS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 6,
                PacketCodecs.BOOLEAN, SetBoatBoatAirControlS2CPayload::airControl,
                SetBoatBoatAirControlS2CPayload::new);

        private SetBoatBoatAirControlS2CPayload(short id, boolean waterElevation) {
            this(waterElevation);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetBoatJumpForceS2CPayload(float jumpForce) implements CustomPayload {
        public static final CustomPayload.Id<SetBoatJumpForceS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetBoatJumpForceS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 7,
                PacketCodecs.FLOAT, SetBoatJumpForceS2CPayload::jumpForce,
                SetBoatJumpForceS2CPayload::new);

        private SetBoatJumpForceS2CPayload(short id, float jumpForce) {
            this(jumpForce);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetModeS2CPayload(Mode mode) implements CustomPayload {
        public static final CustomPayload.Id<SetModeS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetModeS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 8,
                PacketCodecs.SHORT, mode -> mode.mode().id(),
                SetModeS2CPayload::new);

        private SetModeS2CPayload(short id, short mode) {
            this(Mode.fromId(mode));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetGravityS2CPayload(double gravity) implements CustomPayload {
        public static final CustomPayload.Id<SetGravityS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetGravityS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 9,
                PacketCodecs.DOUBLE, SetGravityS2CPayload::gravity,
                SetGravityS2CPayload::new);

        private SetGravityS2CPayload(short id, double gravity) {
            this(gravity);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetYawAccelerationS2CPayload(float acceleration) implements CustomPayload {
        public static final CustomPayload.Id<SetYawAccelerationS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetYawAccelerationS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 10,
                PacketCodecs.FLOAT, SetYawAccelerationS2CPayload::acceleration,
                SetYawAccelerationS2CPayload::new);

        private SetYawAccelerationS2CPayload(short id, float acceleration) {
            this(acceleration);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetForwardAccelerationS2CPayload(float acceleration) implements CustomPayload {
        public static final CustomPayload.Id<SetForwardAccelerationS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetForwardAccelerationS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 11,
                PacketCodecs.FLOAT, SetForwardAccelerationS2CPayload::acceleration,
                SetForwardAccelerationS2CPayload::new);

        private SetForwardAccelerationS2CPayload(short id, float acceleration) {
            this(acceleration);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetBackwardAccelerationS2CPayload(float acceleration) implements CustomPayload {
        public static final CustomPayload.Id<SetBackwardAccelerationS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetBackwardAccelerationS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 12,
                PacketCodecs.FLOAT, SetBackwardAccelerationS2CPayload::acceleration,
                SetBackwardAccelerationS2CPayload::new);

        private SetBackwardAccelerationS2CPayload(short id, float acceleration) {
            this(acceleration);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetTurningForwardAccelerationS2CPayload(float acceleration) implements CustomPayload {
        public static final CustomPayload.Id<SetTurningForwardAccelerationS2CPayload> ID = new CustomPayload.Id<>(
                CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetTurningForwardAccelerationS2CPayload> CODEC = PacketCodec
                .tuple(
                        PacketCodecs.SHORT, id -> 13,
                        PacketCodecs.FLOAT, SetTurningForwardAccelerationS2CPayload::acceleration,
                        SetTurningForwardAccelerationS2CPayload::new);

        private SetTurningForwardAccelerationS2CPayload(short id, float acceleration) {
            this(acceleration);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record AllowAccelerationStackingS2CPayload(boolean enabled) implements CustomPayload {
        public static final CustomPayload.Id<AllowAccelerationStackingS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, AllowAccelerationStackingS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 14,
                PacketCodecs.BOOLEAN, AllowAccelerationStackingS2CPayload::enabled,
                AllowAccelerationStackingS2CPayload::new);

        private AllowAccelerationStackingS2CPayload(short id, boolean enabled) {
            this(enabled);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ResendVersionS2CPayload() implements CustomPayload {
        public static final CustomPayload.Id<ResendVersionS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, ResendVersionS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 15,
                ResendVersionS2CPayload::new);

        private ResendVersionS2CPayload(short id) {
            this();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetUnderwaterControlS2CPayload(boolean enabled) implements CustomPayload {
        public static final CustomPayload.Id<SetUnderwaterControlS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetUnderwaterControlS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 16,
                PacketCodecs.BOOLEAN, SetUnderwaterControlS2CPayload::enabled,
                SetUnderwaterControlS2CPayload::new);

        private SetUnderwaterControlS2CPayload(short id, boolean enabled) {
            this(enabled);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetSurfaceWaterControlS2CPayload(boolean enabled) implements CustomPayload {
        public static final CustomPayload.Id<SetSurfaceWaterControlS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetSurfaceWaterControlS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 17,
                PacketCodecs.BOOLEAN, SetSurfaceWaterControlS2CPayload::enabled,
                SetSurfaceWaterControlS2CPayload::new);

        private SetSurfaceWaterControlS2CPayload(short id, boolean enabled) {
            this(enabled);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetExclusiveModeS2CPayload(Mode mode) implements CustomPayload {
        public static final CustomPayload.Id<SetExclusiveModeS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetExclusiveModeS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 18,
                PacketCodecs.SHORT, mode -> mode.mode().id(),
                SetExclusiveModeS2CPayload::new);

        private SetExclusiveModeS2CPayload(short id, short mode) {
            this(Mode.fromId(mode));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetCoyoteTimeS2CPayload(int time) implements CustomPayload {
        public static final CustomPayload.Id<SetCoyoteTimeS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetCoyoteTimeS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 19,
                PacketCodecs.INTEGER, SetCoyoteTimeS2CPayload::time,
                SetCoyoteTimeS2CPayload::new);

        private SetCoyoteTimeS2CPayload(short id, int time) {
            this(time);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetWaterJumpingS2CPayload(boolean enabled) implements CustomPayload {
        public static final CustomPayload.Id<SetWaterJumpingS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetWaterJumpingS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 20,
                PacketCodecs.BOOLEAN, SetWaterJumpingS2CPayload::enabled,
                SetWaterJumpingS2CPayload::new);

        private SetWaterJumpingS2CPayload(short id, boolean enabled) {
            this(enabled);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetSwimForceS2CPayload(float force) implements CustomPayload {
        public static final CustomPayload.Id<SetSwimForceS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetSwimForceS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 21,
                PacketCodecs.FLOAT, SetSwimForceS2CPayload::force,
                SetSwimForceS2CPayload::new);

        private SetSwimForceS2CPayload(short id, float force) {
            this(force);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClearBlocksSlipperinessS2CPayload(List<String> blocks) implements CustomPayload {
        public static final CustomPayload.Id<ClearBlocksSlipperinessS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, ClearBlocksSlipperinessS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 22,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), ClearBlocksSlipperinessS2CPayload::blocks,
                ClearBlocksSlipperinessS2CPayload::new);

        private ClearBlocksSlipperinessS2CPayload(short id, List<String> blocks) {
            this(blocks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClearAllSlipperinessS2CPayload() implements CustomPayload {
        public static final CustomPayload.Id<ClearAllSlipperinessS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, ClearAllSlipperinessS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 23,
                ClearAllSlipperinessS2CPayload::new);

        private ClearAllSlipperinessS2CPayload(short id) {
            this();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ApplyModeSeriesS2CPayload(List<Mode> modes) implements CustomPayload {
        public static final CustomPayload.Id<ApplyModeSeriesS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, ApplyModeSeriesS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 24,
                PacketCodecs.SHORT, p -> (short) p.modes().size(),
                PacketCodecs.SHORT.collect(PacketCodecs.toList()), p -> p.modes().stream().map(Mode::id).toList(),
                ApplyModeSeriesS2CPayload::new);

        private ApplyModeSeriesS2CPayload(short id, short amount, List<Short> modes) {
            this(modes.stream().map(Mode::fromId).toList());
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ApplyExclusiveModeSeriesS2CPayload(List<Mode> modes) implements CustomPayload {
        public static final CustomPayload.Id<ApplyExclusiveModeSeriesS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, ApplyExclusiveModeSeriesS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 25,
                PacketCodecs.SHORT, p -> (short) p.modes().size(),
                PacketCodecs.SHORT.collect(PacketCodecs.toList()), p -> p.modes().stream().map(Mode::id).toList(),
                ApplyExclusiveModeSeriesS2CPayload::new);

        private ApplyExclusiveModeSeriesS2CPayload(short id, short amount, List<Short> modes) {
            this(modes.stream().map(Mode::fromId).toList());
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetPerBlockSettingS2CPayload(BlockSetting setting, float value, List<String> blocks)
            implements CustomPayload {

        public static final CustomPayload.Id<SetPerBlockSettingS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetPerBlockSettingS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 26,
                PacketCodecs.SHORT, p -> p.setting().value(),
                PacketCodecs.FLOAT, SetPerBlockSettingS2CPayload::value,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), SetPerBlockSettingS2CPayload::blocks,
                SetPerBlockSettingS2CPayload::new);

        private SetPerBlockSettingS2CPayload(short id, short setting, float value, List<String> blocks) {
            this(BlockSetting.fromValue(setting), value, blocks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetCollisionModeS2CPayload(CollisionMode collisionMode) implements CustomPayload {
        public static final CustomPayload.Id<SetCollisionModeS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetCollisionModeS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 27,
                PacketCodecs.SHORT, p -> p.collisionMode().value(),
                SetCollisionModeS2CPayload::new);

        private SetCollisionModeS2CPayload(short id, short collisionMode) {
            this(CollisionMode.fromValue(collisionMode));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetAirSteppingS2CPayload(boolean enabled) implements CustomPayload {
        public static final CustomPayload.Id<SetAirSteppingS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetAirSteppingS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 28,
                PacketCodecs.BOOLEAN, SetAirSteppingS2CPayload::enabled,
                SetAirSteppingS2CPayload::new);

        private SetAirSteppingS2CPayload(short id, boolean enabled) {
            this(enabled);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetTenStepInterpolationS2CPayload(boolean enabled) implements CustomPayload {
        public static final CustomPayload.Id<SetTenStepInterpolationS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetTenStepInterpolationS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 29,
                PacketCodecs.BOOLEAN, SetTenStepInterpolationS2CPayload::enabled,
                SetTenStepInterpolationS2CPayload::new);

        private SetTenStepInterpolationS2CPayload(short id, boolean enabled) {
            this(enabled);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetCollisionResolutionS2CPayload(byte resolution) implements CustomPayload {
        public static final CustomPayload.Id<SetCollisionResolutionS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, SetCollisionResolutionS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 30,
                PacketCodecs.BYTE, SetCollisionResolutionS2CPayload::resolution,
                SetCollisionResolutionS2CPayload::new);

        private SetCollisionResolutionS2CPayload(short id, byte resolution) {
            this(resolution);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record AddToCollisionFilterS2CPayload(List<String> entities) implements CustomPayload {
        public static final CustomPayload.Id<AddToCollisionFilterS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, AddToCollisionFilterS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 31,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), AddToCollisionFilterS2CPayload::entities,
                AddToCollisionFilterS2CPayload::new);

        private AddToCollisionFilterS2CPayload(short id, List<String> entities) {
            this(entities);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
