package world.ultravanilla.ultrastopwatch.model;

import java.util.ArrayList;
import java.util.List;

public class RaceEvent {

    public enum ScoringType {
        TOTAL_TIME,
        POINTS
    }

    private String name;
    private List<String> trackNames = new ArrayList<>();
    private ScoringType scoringType = ScoringType.TOTAL_TIME;
    private boolean active = false;

    public RaceEvent() {}

    public RaceEvent(String name, ScoringType scoringType) {
        this.name = name;
        this.scoringType = scoringType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTrackNames() {
        return trackNames;
    }

    public void addTrack(String trackName) {
        if (!trackNames.contains(trackName)) {
            trackNames.add(trackName);
        }
    }

    public boolean removeTrack(String trackName) {
        return trackNames.remove(trackName);
    }

    public ScoringType getScoringType() {
        return scoringType;
    }

    public void setScoringType(ScoringType scoringType) {
        this.scoringType = scoringType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
