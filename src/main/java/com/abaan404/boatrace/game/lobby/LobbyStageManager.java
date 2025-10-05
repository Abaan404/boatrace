package com.abaan404.boatrace.game.lobby;

import java.util.List;
import java.util.Optional;

import com.abaan404.boatrace.game.maps.TrackMap;

/**
 * Handles lobby state.
 */
public class LobbyStageManager {
    private final List<TrackMap> tracks;
    private int selectedTrackIdx = 0;

    public LobbyStageManager(List<TrackMap> trackIds) {
        this.tracks = trackIds;
    }

    /**
     * Cycle loaded tracks.
     */
    public void cycleTracks() {
        this.selectedTrackIdx = (this.selectedTrackIdx + 1) % this.tracks.size();
    }

    /**
     * Get the selected track.
     *
     * @return The selected track.
     */
    public Optional<TrackMap> getTrack() {
        try {
            return Optional.of(this.tracks.get(this.selectedTrackIdx));
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }
}
