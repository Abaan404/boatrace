package com.abaan404.boatrace.compat.openboatutils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRacePlayer;
import com.abaan404.boatrace.BoatRaceTrack;
import com.abaan404.boatrace.compat.openboatutils.OBUTrackConfig.BlockSettingType;
import com.abaan404.boatrace.compat.openboatutils.OBUTrackConfig.BlockSettings.BlockAttribute;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.AddToCollisionFilterS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.AllowAccelerationStackingS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.ApplyModeSeriesS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.CollisionMode;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.Mode;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.OBUS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.ResetS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetAirSteppingS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetBackwardAccelerationS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetBlocksSlipperinessS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetBoatAirControlS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetBoatFallDamageS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetBoatJumpForceS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetBoatWaterElevationS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetCollisionModeS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetCollisionResolutionS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetCoyoteTimeS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetDefaultSlipperinessS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetForwardAccelerationS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetGravityS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetPerBlockSettingS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetStepHeightS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetSurfaceWaterControlS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetSwimForceS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetTenStepInterpolationS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetTurningForwardAccelerationS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetUnderwaterControlS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetWaterJumpingS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.SetYawAccelerationS2CPayload;
import com.abaan404.boatrace.compat.openboatutils.OBUPackets.VersionC2SPayload;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;

public final class OBU {
    private static final Set<Integer> REJECTED_VERSIONS = Set.of(12, 8);
    private static final Set<BoatRacePlayer> PLAYERS = new ObjectOpenHashSet<>();

    private final GameSpace gameSpace;
    private final Optional<OBUGameConfig> gameConfig;
    private final Optional<OBUTrackConfig> trackConfig;
    private final boolean required;

    private OBU(GameSpace gameSpace, BoatRaceConfig config, BoatRaceTrack track) {
        this.gameSpace = gameSpace;
        this.gameConfig = config.openboatutils();
        this.trackConfig = track.getAttributes().openboatutils();
        this.required = this.gameConfig.isPresent() || this.trackConfig.isPresent();
    }

    /**
     * Setup events for tracking if the player has openboatutils
     */
    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            BoatRacePlayer player = BoatRacePlayer.of(handler.player);
            PLAYERS.remove(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            BoatRacePlayer player = BoatRacePlayer.of(handler.player);
            PLAYERS.remove(player);
        });

        ServerPlayNetworking.registerGlobalReceiver(VersionC2SPayload.ID, (payload, context) -> {
            if (REJECTED_VERSIONS.contains(payload.version())) {
                return;
            }

            BoatRacePlayer player = BoatRacePlayer.of(context.player());

            PLAYERS.add(player);
        });
    }

    /**
     * Add OpenBoatUtils support to a game.
     *
     * @param activity The game activity.
     * @param config   The game with OBU config.
     * @param track    The track with OBU config.
     * @return The instance to handle OBU settings.
     */
    public static OBU addTo(GameActivity activity, BoatRaceConfig config, BoatRaceTrack track) {
        OBU openboatutils = new OBU(activity.getGameSpace(), config, track);

        activity.listen(GamePlayerEvents.ADD, openboatutils::onAddPlayer);
        activity.listen(GamePlayerEvents.REMOVE, openboatutils::onRemovePlayer);
        activity.listen(GameActivityEvents.DISABLE, openboatutils::onDisable);

        return openboatutils;
    }

    /**
     * Check if the player needs OpenBoatUtils support. The check is done when the
     * player sends the {@link VersionC2SPayload} packet.
     *
     * @param player The player to check.
     * @return If the player needs OpenBoatUtils.
     */
    public boolean canPlay(BoatRacePlayer player) {
        return !this.required || PLAYERS.contains(player);
    }

    /**
     * Apply OBU configs on add.
     *
     * @param player The player.
     */
    private void onAddPlayer(ServerPlayerEntity player) {
        // reset to vanilla before applying anything
        ServerPlayNetworking.send(player, new ResetS2CPayload());

        // apply mode before any other setting to let the player customize the mode
        this.trackConfig.flatMap(config -> config.mode()).ifPresent(mode -> {
            List<Mode> mode2 = mode.stream().map(m -> switch (m) {
                case RALLY -> Mode.RALLY;
                case RALLY_BLUE -> Mode.RALLY_BLUE;
                case BA_NOFD -> Mode.BA_NOFD;
                case PARKOUR -> Mode.PARKOUR;
                case BA_BLUE_NOFD -> Mode.BA_BLUE_NOFD;
                case PARKOUR_BLUE -> Mode.PARKOUR_BLUE;
                case BA -> Mode.BA;
                case BA_BLUE -> Mode.BA_BLUE;
            }).toList();

            OBUS2CPayload payload = new ApplyModeSeriesS2CPayload(mode2);
            ServerPlayNetworking.send(player, payload);
        });

        this.gameConfig.flatMap(config -> config.collision()).ifPresent(collision -> {
            CollisionMode mode = switch (collision.mode()) {
                case VANILLA -> CollisionMode.VANILLA;
                case NO_COLLISION_WITH_BOATS_AND_PLAYERS -> CollisionMode.NO_COLLISION_WITH_BOATS_AND_PLAYERS;
                case NO_COLLISION_WITH_ANY_ENTITIES -> CollisionMode.NO_COLLISION_WITH_ANY_ENTITIES;
                case FILTERED_COLLISION -> CollisionMode.FILTERED_COLLISION;
                case NO_COLLISION_WITH_BOATS_AND_PLAYERS_PLUS_FILTERED_COLLISION ->
                    CollisionMode.NO_COLLISION_WITH_BOATS_AND_PLAYERS_PLUS_FILTERED_COLLISION;
            };

            ServerPlayNetworking.send(player, new AddToCollisionFilterS2CPayload(collision.filter()));
            ServerPlayNetworking.send(player, new SetCollisionModeS2CPayload(mode));
            ServerPlayNetworking.send(player, new SetCollisionResolutionS2CPayload(collision.resolution()));
        });

        this.trackConfig.flatMap(config -> config.settings()).ifPresent(settings -> {
            for (Map.Entry<BlockSettingType, Float> defaults : settings.defaults().entrySet()) {
                OBUS2CPayload payload = switch (defaults.getKey()) {
                    case SLIPPERINESS -> new SetDefaultSlipperinessS2CPayload(defaults.getValue());
                    case JUMP_FORCE -> new SetBoatJumpForceS2CPayload(defaults.getValue());
                    case FORWARDS_ACCELERATION -> new SetForwardAccelerationS2CPayload(defaults.getValue());
                    case BACKWARDS_ACCELERATION -> new SetBackwardAccelerationS2CPayload(defaults.getValue());
                    case YAW_ACCELERATION -> new SetYawAccelerationS2CPayload(defaults.getValue());
                    case TURN_FORWARDS_ACCELERATION -> new SetTurningForwardAccelerationS2CPayload(defaults.getValue());
                };

                ServerPlayNetworking.send(player, payload);
            }

            for (Map.Entry<BlockSettingType, List<BlockAttribute>> setting : settings
                    .attributes().entrySet()) {

                for (BlockAttribute blockAttribute : setting.getValue()) {
                    OBUS2CPayload payload = switch (setting.getKey()) {
                        case JUMP_FORCE -> new SetPerBlockSettingS2CPayload(
                                OBUPackets.BlockSetting.JUMP_FORCE,
                                blockAttribute.value(),
                                blockAttribute.blocks());
                        case FORWARDS_ACCELERATION -> new SetPerBlockSettingS2CPayload(
                                OBUPackets.BlockSetting.FORWARDS_ACCEL,
                                blockAttribute.value(),
                                blockAttribute.blocks());
                        case BACKWARDS_ACCELERATION -> new SetPerBlockSettingS2CPayload(
                                OBUPackets.BlockSetting.BACKWARDS_ACCEL,
                                blockAttribute.value(),
                                blockAttribute.blocks());
                        case TURN_FORWARDS_ACCELERATION -> new SetPerBlockSettingS2CPayload(
                                OBUPackets.BlockSetting.TURN_FORWARDS_ACCEL,
                                blockAttribute.value(),
                                blockAttribute.blocks());
                        case YAW_ACCELERATION -> new SetPerBlockSettingS2CPayload(
                                OBUPackets.BlockSetting.YAW_ACCEL,
                                blockAttribute.value(),
                                blockAttribute.blocks());
                        case SLIPPERINESS -> new SetBlocksSlipperinessS2CPayload(
                                blockAttribute.value(),
                                blockAttribute.blocks());
                    };

                    ServerPlayNetworking.send(player, payload);
                }
            }
        });

        this.trackConfig.flatMap(config -> config.swimForce()).ifPresent(swimForce -> {
            ServerPlayNetworking.send(player, new SetSwimForceS2CPayload(swimForce));
        });

        this.trackConfig.flatMap(config -> config.stepHeight()).ifPresent(stepHeight -> {
            ServerPlayNetworking.send(player, new SetStepHeightS2CPayload(stepHeight));
        });

        this.trackConfig.flatMap(config -> config.gravity()).ifPresent(gravity -> {
            ServerPlayNetworking.send(player, new SetGravityS2CPayload(gravity));
        });

        this.trackConfig.flatMap(config -> config.coyoteTime()).ifPresent(coyoteTime -> {
            ServerPlayNetworking.send(player, new SetCoyoteTimeS2CPayload(coyoteTime));
        });

        this.trackConfig.flatMap(config -> config.fallDamage()).ifPresent(fallDamage -> {
            ServerPlayNetworking.send(player, new SetBoatFallDamageS2CPayload(fallDamage));
        });

        this.trackConfig.flatMap(config -> config.airControl()).ifPresent(airControl -> {
            ServerPlayNetworking.send(player, new SetBoatAirControlS2CPayload(airControl));
        });

        this.trackConfig.flatMap(config -> config.waterElevation()).ifPresent(waterElevation -> {
            ServerPlayNetworking.send(player, new SetBoatWaterElevationS2CPayload(waterElevation));
        });

        this.trackConfig.flatMap(config -> config.accelerationStacking()).ifPresent(accelerationStacking -> {
            ServerPlayNetworking.send(player, new AllowAccelerationStackingS2CPayload(accelerationStacking));
        });

        this.trackConfig.flatMap(config -> config.underwaterControl()).ifPresent(underwaterControl -> {
            ServerPlayNetworking.send(player, new SetUnderwaterControlS2CPayload(underwaterControl));
        });

        this.trackConfig.flatMap(config -> config.surfaceWaterControl()).ifPresent(surfaceWaterControl -> {
            ServerPlayNetworking.send(player, new SetSurfaceWaterControlS2CPayload(surfaceWaterControl));
        });

        this.trackConfig.flatMap(config -> config.waterJumping()).ifPresent(waterJumping -> {
            ServerPlayNetworking.send(player, new SetWaterJumpingS2CPayload(waterJumping));
        });

        this.trackConfig.flatMap(config -> config.airStepping()).ifPresent(airStepping -> {
            ServerPlayNetworking.send(player, new SetAirSteppingS2CPayload(airStepping));
        });

        this.trackConfig.flatMap(config -> config.tenStepInterpolation()).ifPresent(tenStepInterpolation -> {
            ServerPlayNetworking.send(player, new SetTenStepInterpolationS2CPayload(tenStepInterpolation));
        });
    }

    /**
     * Reset to vanilla on remove.
     *
     * @param player The player.
     */
    private void onRemovePlayer(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new ResetS2CPayload());
    }

    /**
     * Reset everyone to vanilla on disable.
     */
    private void onDisable() {
        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            ServerPlayNetworking.send(player, new ResetS2CPayload());
        }
    }
}
