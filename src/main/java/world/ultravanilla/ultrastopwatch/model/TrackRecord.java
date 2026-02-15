package world.ultravanilla.ultrastopwatch.model;

import java.util.UUID;

public class TrackRecord implements Comparable<TrackRecord> {

    private UUID playerUUID;
    private String playerName;
    private long timeMs;
    private long timestamp;

    public TrackRecord() {}

    public TrackRecord(UUID playerUUID, String playerName, long timeMs) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.timeMs = timeMs;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(TrackRecord other) {
        int timeCmp = Long.compare(this.timeMs, other.timeMs);
        if (timeCmp != 0) return timeCmp;
        return Long.compare(this.timestamp, other.timestamp);
    }

    public static String formatTime(long ms) {
        long minutes = ms / 60000;
        long seconds = (ms % 60000) / 1000;
        long millis = ms % 1000;

        char[] buf = new char[9]; // MM:SS.mmm
        buf[0] = (char) ('0' + (minutes / 10));
        buf[1] = (char) ('0' + (minutes % 10));
        buf[2] = ':';
        buf[3] = (char) ('0' + (seconds / 10));
        buf[4] = (char) ('0' + (seconds % 10));
        buf[5] = '.';
        buf[6] = (char) ('0' + (millis / 100));
        buf[7] = (char) ('0' + ((millis / 10) % 10));
        buf[8] = (char) ('0' + (millis % 10));
        return new String(buf);
    }
}
