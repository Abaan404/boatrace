package com.abaan404.boatrace.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.abaan404.boatrace.events.PlayerDismountEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"), cancellable = true)
    private void dismountVehicle(CallbackInfo ci) {
        Entity vehicle = this.getVehicle();
        if (vehicle == null || vehicle.isRemoved()) {
            // how did we get here?
            return;
        }

        if (!this.level().isClientSide()) {
            ServerPlayer player = (ServerPlayer) (Object) this;

            try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
                EventResult result = invokers.get(PlayerDismountEvent.EVENT).onDismount(player, vehicle);
                if (result == EventResult.DENY) {
                    ci.cancel();
                    return;
                }
            }
        }
    }
}
