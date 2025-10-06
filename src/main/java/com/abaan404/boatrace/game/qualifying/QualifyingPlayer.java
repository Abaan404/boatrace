package com.abaan404.boatrace.game.qualifying;

import java.util.List;

import com.abaan404.boatrace.maps.TrackMap;

public record QualifyingPlayer(int gridBox) {
    TrackMap.RespawnRegion getPitBox(TrackMap track) {
        List<TrackMap.RespawnRegion> pitBoxes = track.getRegions().gridBoxes();

        if (this.gridBox > pitBoxes.size() || this.gridBox < 0) {
            return pitBoxes.getLast();
        }

        return pitBoxes.get(this.gridBox);
    }
}
