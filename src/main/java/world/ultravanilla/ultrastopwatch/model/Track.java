package world.ultravanilla.ultrastopwatch.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class Track {

    public enum TriggerType {
        PRESSURE_PLATE,
        TRIPWIRE
    }

    private String name;
    private String world;
    private int startAx, startAy, startAz;
    private int startBx, startBy, startBz;
    private boolean startASet = false;
    private boolean startBSet = false;
    private int endAx, endAy, endAz;
    private int endBx, endBy, endBz;
    private boolean endASet = false;
    private boolean endBSet = false;
    private TriggerType triggerType = TriggerType.PRESSURE_PLATE;
    private boolean leaderboardEnabled = true;
    private long triggerDelay = 0;

    public Track() {}

    public Track(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorld() {
        return world;
    }

    public void setStartPoint1(Location loc) {
        String newWorld = loc.getWorld().getName();
        if (world != null && !newWorld.equals(world)) {
            // World changed, clear end
            endASet = false;
            endBSet = false;
        }
        this.world = newWorld;
        this.startAx = loc.getBlockX();
        this.startAy = loc.getBlockY();
        this.startAz = loc.getBlockZ();
        this.startASet = true;
        this.startBSet = false;
    }

    public boolean setStartPoint2(Location loc) {
        if (!loc.getWorld().getName().equals(this.world)) return false;
        if (loc.getBlockY() != startAy) return false;
        boolean xAligned = loc.getBlockX() == startAx;
        boolean zAligned = loc.getBlockZ() == startAz;
        if (!xAligned && !zAligned) return false;

        this.startBx = loc.getBlockX();
        this.startBy = loc.getBlockY();
        this.startBz = loc.getBlockZ();
        this.startBSet = true;
        return true;
    }

    public boolean isStartBlock(Block block) {
        if (!startBSet) return false;
        if (!block.getWorld().getName().equals(world)) return false;
        if (block.getY() != startAy) return false;
        int x = block.getX();
        int z = block.getZ();
        int minX = Math.min(startAx, startBx);
        int maxX = Math.max(startAx, startBx);
        int minZ = Math.min(startAz, startBz);
        int maxZ = Math.max(startAz, startBz);
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public Location getStartPoint1() {
        if (!startASet) return null;
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, startAx, startAy, startAz);
    }

    public Location getStartPoint2() {
        if (!startBSet) return null;
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, startBx, startBy, startBz);
    }

    public boolean setEndPoint1(Location loc) {
        if (world != null && !loc.getWorld().getName().equals(world)) return false;
        if (world == null) this.world = loc.getWorld().getName();
        this.endAx = loc.getBlockX();
        this.endAy = loc.getBlockY();
        this.endAz = loc.getBlockZ();
        this.endASet = true;
        this.endBSet = false;
        return true;
    }

    public boolean setEndPoint2(Location loc) {
        if (!loc.getWorld().getName().equals(this.world)) return false;
        if (loc.getBlockY() != endAy) return false;
        boolean xAligned = loc.getBlockX() == endAx;
        boolean zAligned = loc.getBlockZ() == endAz;
        if (!xAligned && !zAligned) return false;

        this.endBx = loc.getBlockX();
        this.endBy = loc.getBlockY();
        this.endBz = loc.getBlockZ();
        this.endBSet = true;
        return true;
    }

    public boolean isEndBlock(Block block) {
        if (!endBSet) return false;
        if (!block.getWorld().getName().equals(world)) return false;
        if (block.getY() != endAy) return false;
        int x = block.getX();
        int z = block.getZ();
        int minX = Math.min(endAx, endBx);
        int maxX = Math.max(endAx, endBx);
        int minZ = Math.min(endAz, endBz);
        int maxZ = Math.max(endAz, endBz);
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public Location getEndPoint1() {
        if (!endASet) return null;
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, endAx, endAy, endAz);
    }

    public Location getEndPoint2() {
        if (!endBSet) return null;
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, endBx, endBy, endBz);
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public boolean isLeaderboardEnabled() {
        return leaderboardEnabled;
    }

    public void setLeaderboardEnabled(boolean leaderboardEnabled) {
        this.leaderboardEnabled = leaderboardEnabled;
    }

    public long getTriggerDelay() {
        return triggerDelay;
    }

    public void setTriggerDelay(long triggerDelay) {
        this.triggerDelay = triggerDelay;
    }

    public boolean isStartASet() {
        return startASet;
    }

    public boolean isStartBSet() {
        return startBSet;
    }

    public boolean isEndASet() {
        return endASet;
    }

    public boolean isEndBSet() {
        return endBSet;
    }

    public boolean isFullyConfigured() {
        return startBSet && endBSet;
    }
}
