package com.abaan404.boatrace.events;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface PlayerDismountEvent {
    StimulusEvent<PlayerDismountEvent> EVENT = StimulusEvent.create(PlayerDismountEvent.class, ctx -> {
        return (player, vehicle) -> {
            try {
                for (PlayerDismountEvent listener : ctx.getListeners()) {
                    EventResult result = listener.onDismount(player, vehicle);
                    if (result != EventResult.PASS) {
                        return result;
                    }
                }
            } catch (Throwable t) {
                ctx.handleException(t);
            }

            return EventResult.PASS;
        };
    });

    public EventResult onDismount(ServerPlayerEntity player, Entity vehicle);
}
