package com.abaan404.boatrace.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.abaan404.boatrace.BoatRaceGameRules;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.impl.game.manager.GameSpaceManagerImpl;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin extends VehicleEntity {
    public AbstractBoatMixin(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "getMaxPassengers", at = @At("HEAD"), cancellable = true)
    private void getMaxPassengers(CallbackInfoReturnable<Integer> cir) {
        GameSpace gameSpace = GameSpaceManagerImpl.get().byLevel(this.level());

        if (gameSpace != null) {
            EventResult singleSeat = gameSpace.getBehavior().testRule(BoatRaceGameRules.SINGLE_SEAT);

            if (singleSeat == EventResult.ALLOW) {
                cir.setReturnValue(1);
                cir.cancel();
            }
        }
    }
}
