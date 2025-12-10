package com.abaan404.boatrace.compat.openboatutils;

import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
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
        // Serverbound
        PayloadTypeRegistry.playC2S().register(
                VersionC2SPayload.ID,
                VersionC2SPayload.CODEC);

        // Clientbound
        PayloadTypeRegistry.playS2C().register(
                OBUS2CPayload.ID,
                OBUS2CPayload.CODEC);
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
        BA_BLUE(15);

        private final short modeId;

        private Mode(int modeId) {
            this.modeId = (short) modeId;
        }

        public short id() {
            return this.modeId;
        }
    }

    enum BlockSetting {
        JUMP_FORCE(0),
        FORWARDS_ACCEL(1),
        BACKWARDS_ACCEL(2),
        YAW_ACCEL(3),
        TURN_FORWARDS_ACCEL(4);

        private final short value;

        private BlockSetting(int setting) {
            this.value = (short) setting;
        }

        public short value() {
            return this.value;
        }
    }

    enum CollisionMode {
        VANILLA(0),
        NO_COLLISION_WITH_BOATS_AND_PLAYERS(1),
        NO_COLLISION_WITH_ANY_ENTITIES(2),
        FILTERED_COLLISION(3),
        NO_COLLISION_WITH_BOATS_AND_PLAYERS_PLUS_FILTERED_COLLISION(4);

        private final short value;

        private CollisionMode(int setting) {
            this.value = (short) setting;
        }

        public short value() {
            return this.value;
        }
    }

    public record VersionC2SPayload(int version) implements CustomPayload {
        public static final CustomPayload.Id<VersionC2SPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, VersionC2SPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.SHORT, id -> 0,
                PacketCodecs.INTEGER, VersionC2SPayload::version,
                VersionC2SPayload::new);

        private VersionC2SPayload(short id, int version) {
            this(version);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public interface OBUS2CPayload extends CustomPayload {
        public static final CustomPayload.Id<OBUS2CPayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<PacketByteBuf, OBUS2CPayload> CODEC = PacketCodec.of(
                (payload, buf) -> payload.write(buf),
                buf -> {
                    buf.readBytes(buf.readableBytes()); // read everything so mc doesnt complain.
                    return new OBUS2CPayload() {
                        @Override
                        public short packetId() {
                            throw new UnsupportedOperationException("Unimplemented method 'packetId'");
                        }
                    };
                });

        public default void write(PacketByteBuf buffer) {
            buffer.writeShort(this.packetId());
        }

        public short packetId();

        @Override
        default Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ResetS2CPayload() implements OBUS2CPayload {
        @Override
        public short packetId() {
            return 0;
        }
    }

    public record SetStepHeightS2CPayload(float stepHeight) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeFloat(stepHeight);
        }

        @Override
        public short packetId() {
            return 1;
        }
    }

    public record SetDefaultSlipperinessS2CPayload(float slipperiness) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeFloat(slipperiness);
        }

        @Override
        public short packetId() {
            return 2;
        }
    }

    public record SetBlocksSlipperinessS2CPayload(float slipperiness, List<String> blocks) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeFloat(slipperiness);
            buf.writeString(String.join(",", blocks));
        }

        @Override
        public short packetId() {
            return 3;
        }
    }

    public record SetBoatFallDamageS2CPayload(boolean fallDamage) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeBoolean(fallDamage);
        }

        @Override
        public short packetId() {
            return 4;
        }
    }

    public record SetBoatWaterElevationS2CPayload(boolean waterElevation) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeBoolean(waterElevation);
        }

        @Override
        public short packetId() {
            return 5;
        }
    }

    public record SetBoatAirControlS2CPayload(boolean airControl) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeBoolean(airControl);
        }

        @Override
        public short packetId() {
            return 6;
        }
    }

    public record SetBoatJumpForceS2CPayload(float jumpForce) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeFloat(jumpForce);
        }

        @Override
        public short packetId() {
            return 7;
        }
    }

    public record SetModeS2CPayload(Mode mode) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeShort(mode.id());
        }

        @Override
        public short packetId() {
            return 8;
        }
    }

    public record SetGravityS2CPayload(double gravity) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeDouble(gravity);
        }

        @Override
        public short packetId() {
            return 9;
        }
    }

    public record SetYawAccelerationS2CPayload(float acceleration) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeFloat(acceleration);
        }

        @Override
        public short packetId() {
            return 10;
        }
    }

    public record SetForwardAccelerationS2CPayload(float acceleration) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeFloat(acceleration);
        }

        @Override
        public short packetId() {
            return 11;
        }
    }

    public record SetBackwardAccelerationS2CPayload(float acceleration) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeFloat(acceleration);
        }

        @Override
        public short packetId() {
            return 12;
        }
    }

    public record SetTurningForwardAccelerationS2CPayload(float acceleration) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeFloat(acceleration);
        }

        @Override
        public short packetId() {
            return 13;
        }
    }

    public record AllowAccelerationStackingS2CPayload(boolean enabled) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeBoolean(enabled);
        }

        @Override
        public short packetId() {
            return 14;
        }
    }

    public record ResendVersionS2CPayload() implements OBUS2CPayload {
        @Override
        public short packetId() {
            return 15;
        }
    }

    public record SetUnderwaterControlS2CPayload(boolean enabled) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeBoolean(enabled);
        }

        @Override
        public short packetId() {
            return 16;
        }
    }

    public record SetSurfaceWaterControlS2CPayload(boolean enabled) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeBoolean(enabled);
        }

        @Override
        public short packetId() {
            return 17;
        }
    }

    public record SetExclusiveModeS2CPayload(Mode mode) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeShort(mode.id());
        }

        @Override
        public short packetId() {
            return 18;
        }
    }

    public record SetCoyoteTimeS2CPayload(int time) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeInt(time);
        }

        @Override
        public short packetId() {
            return 19;
        }
    }

    public record SetWaterJumpingS2CPayload(boolean enabled) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeBoolean(enabled);
        }

        @Override
        public short packetId() {
            return 20;
        }
    }

    public record SetSwimForceS2CPayload(float force) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeFloat(force);
        }

        @Override
        public short packetId() {
            return 21;
        }
    }

    public record ClearBlocksSlipperinessS2CPayload(List<String> blocks) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeString(String.join(",", blocks));
        }

        @Override
        public short packetId() {
            return 22;
        }
    }

    public record ClearAllSlipperinessS2CPayload() implements OBUS2CPayload {
        @Override
        public short packetId() {
            return 23;
        }
    }

    public record ApplyModeSeriesS2CPayload(List<Mode> modes) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeShort(modes.size());
            for (Mode m : modes) {
                buf.writeShort(m.id());
            }
        }

        @Override
        public short packetId() {
            return 24;
        }
    }

    public record ApplyExclusiveModeSeriesS2CPayload(List<Mode> modes) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeShort(modes.size());
            for (Mode m : modes) {
                buf.writeShort(m.id());
            }
        }

        @Override
        public short packetId() {
            return 25;
        }
    }

    public record SetPerBlockSettingS2CPayload(BlockSetting setting, float value, List<String> blocks)
            implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeShort(setting.value());
            buf.writeFloat(value);
            buf.writeString(String.join(",", blocks));
        }

        @Override
        public short packetId() {
            return 26;
        }
    }

    public record SetCollisionModeS2CPayload(CollisionMode collisionMode) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeShort(collisionMode.value());
        }

        @Override
        public short packetId() {
            return 27;
        }
    }

    public record SetAirSteppingS2CPayload(boolean enabled) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeBoolean(enabled);
        }

        @Override
        public short packetId() {
            return 28;
        }
    }

    public record SetTenStepInterpolationS2CPayload(boolean enabled) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeBoolean(enabled);
        }

        @Override
        public short packetId() {
            return 29;
        }
    }

    public record SetCollisionResolutionS2CPayload(byte resolution) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeByte(resolution);
        }

        @Override
        public short packetId() {
            return 30;
        }
    }

    public record AddToCollisionFilterS2CPayload(List<String> entities) implements OBUS2CPayload {
        @Override
        public void write(PacketByteBuf buf) {
            OBUS2CPayload.super.write(buf);
            buf.writeString(String.join(",", entities));
        }

        @Override
        public short packetId() {
            return 31;
        }
    }

    public record ClearCollisionFilterS2CPayload() implements OBUS2CPayload {
        @Override
        public short packetId() {
            return 32;
        }
    }
}
