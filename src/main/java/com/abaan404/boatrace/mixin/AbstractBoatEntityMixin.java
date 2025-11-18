package com.abaan404.boatrace.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.abaan404.boatrace.BoatRaceGameRules;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.impl.game.manager.GameSpaceManagerImpl;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(AbstractBoatEntity.class)
public abstract class AbstractBoatEntityMixin extends VehicleEntity {
    public AbstractBoatEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "getMaxPassengers", at = @At("HEAD"), cancellable = true)
    private void getMaxPassengers(CallbackInfoReturnable<Integer> cir) {
        GameSpace gameSpace = GameSpaceManagerImpl.get().byWorld(this.getWorld());

        if (gameSpace != null) {
            EventResult singleSeat = gameSpace.getBehavior().testRule(BoatRaceGameRules.SINGLE_SEAT);

            if (singleSeat == EventResult.ALLOW) {
                cir.setReturnValue(1);
                cir.cancel();
            }
        }
    }
}
