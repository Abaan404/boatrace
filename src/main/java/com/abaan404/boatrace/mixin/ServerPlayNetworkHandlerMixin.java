package com.abaan404.boatrace.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetCursorItemS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
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

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin extends ServerCommonNetworkHandler {
    public ServerPlayNetworkHandlerMixin(MinecraftServer server, ClientConnection connection,
            ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onClickSlot", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V", shift = At.Shift.AFTER))
    private void onClickSlot(ClickSlotC2SPacket packet, CallbackInfo ci) {
        GameSpace gameSpace = GameSpaceManagerImpl.get().byPlayer(this.player);

        if (gameSpace != null) {
            ScreenHandler screenHandler = this.player.currentScreenHandler;

            // dont do anything if sgui has a window open
            if (screenHandler instanceof VirtualScreenHandler) {
                return;
            }

            EventResult modifyInventory = gameSpace.getBehavior().testRule(BoatRaceGameRules.MODIFY_INVENTORIES);
            if (modifyInventory == EventResult.DENY) {
                ItemStack stack = screenHandler.getSlot(packet.getSlot()).getStack();

                this.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                        packet.getSyncId(),
                        screenHandler.nextRevision(),
                        packet.getSlot(),
                        stack));

                this.sendPacket(new SetCursorItemS2CPacket(screenHandler.getCursorStack()));

                ci.cancel();
            }
        }
    }
}
