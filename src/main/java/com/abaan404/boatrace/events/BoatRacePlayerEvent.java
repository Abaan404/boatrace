package com.abaan404.boatrace.events;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface BoatRacePlayerEvent {
    StimulusEvent<Dismount> DISMOUNT = StimulusEvent.create(Dismount.class, ctx -> (player, source) -> {
        try {
            for (Dismount listener : ctx.getListeners()) {
                EventResult result = listener.onDismount(player, source);
                if (result != EventResult.PASS) {
                    return result;
                }
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }

        return EventResult.PASS;
    });

    public interface Dismount {
        public EventResult onDismount(ServerPlayerEntity player, Entity vehicle);
    }
}
