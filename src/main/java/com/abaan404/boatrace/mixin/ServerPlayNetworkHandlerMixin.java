package com.abaan404.boatrace.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.abaan404.boatrace.BoatRaceGameRules;

import eu.pb4.sgui.virtual.inventory.VirtualScreenHandler;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.impl.game.manager.GameSpaceManagerImpl;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin extends ServerCommonPacketListenerImpl {
    public ServerPlayNetworkHandlerMixin(MinecraftServer server, Connection connection,
            CommonListenerCookie clientData) {
        super(server, connection, clientData);
    }

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleContainerClick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER))
    private void onClickSlot(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        GameSpace gameSpace = GameSpaceManagerImpl.get().byPlayer(this.player);

        if (gameSpace != null) {
            AbstractContainerMenu screenHandler = this.player.containerMenu;

            // dont do anything if sgui has a window open
            if (screenHandler instanceof VirtualScreenHandler) {
                return;
            }

            EventResult modifyInventory = gameSpace.getBehavior().testRule(BoatRaceGameRules.MODIFY_INVENTORIES);
            if (modifyInventory == EventResult.DENY) {
                ItemStack stack = screenHandler.getSlot(packet.slotNum()).getItem();

                this.send(new ClientboundContainerSetSlotPacket(
                        packet.containerId(),
                        screenHandler.incrementStateId(),
                        packet.slotNum(),
                        stack));

                this.send(new ClientboundSetCursorItemPacket(screenHandler.getCarried()));

                ci.cancel();
            }
        }
    }
}
