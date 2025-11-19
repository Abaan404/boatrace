package com.abaan404.boatrace.events;

import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface PlayerPitSuccess {
    StimulusEvent<PlayerPitSuccess> EVENT = StimulusEvent.create(PlayerPitSuccess.class, ctx -> {
        return (player) -> {
            try {
                for (PlayerPitSuccess listener : ctx.getListeners()) {
                    EventResult result = listener.onPitSuccess(player);
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

    public EventResult onPitSuccess(ServerPlayerEntity player);
}
