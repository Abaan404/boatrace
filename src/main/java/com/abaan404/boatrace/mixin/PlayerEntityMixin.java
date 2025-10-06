package com.abaan404.boatrace.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.abaan404.boatrace.events.BoatRacePlayerEvent;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "dismountVehicle", at = @At("HEAD"), cancellable = true)
    private void dismountVehicle(CallbackInfo ci) {
        Entity vehicle = this.getVehicle();
        if (vehicle == null || vehicle.isRemoved()) {
            // how did we get here?
            return;
        }

        if (!this.getWorld().isClient()) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

            try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
                EventResult result = invokers.get(BoatRacePlayerEvent.DISMOUNT).onDismount(player, vehicle);
                if (result == EventResult.DENY) {
                    ci.cancel();
                    return;
                }
            }
        }
    }
}
