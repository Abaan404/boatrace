package com.abaan404.boatrace.events;

import com.abaan404.boatrace.screen.PitBoxGui;

import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface PlayerPitSuccess {
    StimulusEvent<PlayerPitSuccess> EVENT = StimulusEvent.create(PlayerPitSuccess.class, ctx -> {
        return (gui) -> {
            try {
                for (PlayerPitSuccess listener : ctx.getListeners()) {
                    listener.onPitSuccess(gui);
                }
            } catch (Throwable t) {
                ctx.handleException(t);
            }
        };
    });

    public void onPitSuccess(PitBoxGui gui);
}
