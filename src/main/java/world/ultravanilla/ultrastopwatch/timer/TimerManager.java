/*
 * Copyright (C) 2026 cutelilreno <https://github.com/cutelilreno>
 *
 * This file is part of UltraStopwatch.
 *
 * UltraStopwatch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * UltraStopwatch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with UltraStopwatch.  If not, see <https://www.gnu.org/licenses/>.
 */
package world.ultravanilla.ultrastopwatch.timer;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import world.ultravanilla.ultrastopwatch.model.Track;
import world.ultravanilla.ultrastopwatch.model.TrackRecord;
import world.ultravanilla.ultrastopwatch.storage.DataStore;

import java.util.*;

public class TimerManager {

    private final JavaPlugin plugin;
    private final DataStore dataStore;

    // Hot path
    private final Object2LongOpenHashMap<UUID> runningTimers = new Object2LongOpenHashMap<>(); // uuid -> startNanos
    private final Object2IntOpenHashMap<UUID> lapsRemaining = new Object2IntOpenHashMap<>();
    private final Object2LongOpenHashMap<UUID> lastLapNanos = new Object2LongOpenHashMap<>();
    private final ObjectOpenHashSet<UUID> pendingTimers = new ObjectOpenHashSet<>(); // players waiting to move

    private record PendingRun(String trackName, long timeMs) {}
    private final Map<UUID, PendingRun> pendingSubmissions = new HashMap<>();

    // Cold path
    private final Map<UUID, PlayerTimer> timerDetails = new HashMap<>();

    private BukkitTask actionBarTask;
    private final long maxTimerMillis;
    private final ObjectArrayList<UUID> expiryList = new ObjectArrayList<>();

    public TimerManager(JavaPlugin plugin, DataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.maxTimerMillis = plugin.getConfig().getLong("max-timer-seconds", 3600) * 1000L;
        runningTimers.defaultReturnValue(Long.MIN_VALUE);
    }

    public void startManual(Player player) {
        if (timerDetails.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have an active timer! Use ", NamedTextColor.RED)
                    .append(Component.text("/sw stop", NamedTextColor.YELLOW))
                    .append(Component.text(" or ", NamedTextColor.RED))
                    .append(Component.text("/sw reset", NamedTextColor.YELLOW))
                    .append(Component.text(" first.", NamedTextColor.RED)));
            return;
        }
        PlayerTimer timer = PlayerTimer.manual();
        timerDetails.put(player.getUniqueId(), timer);
        long now = System.nanoTime();
        if (now == Long.MIN_VALUE) now++;
        runningTimers.put(player.getUniqueId(), now);
        player.sendActionBar(Component.text("Timer started!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Timer started! Run ", NamedTextColor.GREEN)
                .append(Component.text("/sw reset", NamedTextColor.YELLOW))
                .append(Component.text(" to cancel.", NamedTextColor.GREEN)));
        ensureActionBarTask();
    }

    public void startCasual(Player player) {
        if (timerDetails.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have an active timer! Use ", NamedTextColor.RED)
                    .append(Component.text("/sw reset", NamedTextColor.YELLOW))
                    .append(Component.text(" first.", NamedTextColor.RED)));
            return;
        }
        timerDetails.put(player.getUniqueId(), PlayerTimer.casual());
        pendingTimers.add(player.getUniqueId());
        player.sendActionBar(Component.text("Ready! Move to start the timer.", NamedTextColor.AQUA));
        player.sendMessage(Component.text("Ready! Move to start the timer.", NamedTextColor.AQUA));
    }

    public void startTrackRun(Player player, String trackName) {
        if (timerDetails.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have an active timer!", NamedTextColor.RED));
            return;
        }
        timerDetails.put(player.getUniqueId(), PlayerTimer.track(trackName.toLowerCase()));
        pendingTimers.add(player.getUniqueId());
        player.sendActionBar(Component.text("Ready! Move to start the timer.", NamedTextColor.AQUA));
        player.sendMessage(Component.text("Ready! Move to start the timer.", NamedTextColor.AQUA));
    }

    public void triggerStart(Player player) {
        if (!pendingTimers.remove(player.getUniqueId())) return;

        PlayerTimer timer = timerDetails.get(player.getUniqueId());
        if (timer == null) return;

        long now = System.nanoTime();
        if (now == Long.MIN_VALUE) now++;
        runningTimers.put(player.getUniqueId(), now);

        if (timer.type() == PlayerTimer.TimerType.TRACK && timer.trackName() != null) {
            Track track = dataStore.getTrack(timer.trackName());
            if (track != null && track.getLaps() > 1) {
                lapsRemaining.put(player.getUniqueId(), track.getLaps());
                lastLapNanos.put(player.getUniqueId(), now);
            }
        }

        player.sendActionBar(Component.text("GO!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Timer started! Run ", NamedTextColor.GREEN)
                .append(Component.text("/sw reset", NamedTextColor.YELLOW))
                .append(Component.text(" to cancel.", NamedTextColor.GREEN)));
        ensureActionBarTask();
    }

    public boolean lap(Player player, int totalLaps) {
        UUID uuid = player.getUniqueId();
        if (!lapsRemaining.containsKey(uuid)) {
            lapsRemaining.put(uuid, totalLaps);
            lastLapNanos.put(uuid, runningTimers.getLong(uuid));
        }

        int remaining = lapsRemaining.getInt(uuid);

        long now = System.nanoTime();
        long last = lastLapNanos.getLong(uuid);
        long split = (now - last) / 1_000_000;
        lastLapNanos.put(uuid, now);

        int currentLap = totalLaps - remaining + 1;
        String splitStr = TrackRecord.formatTime(split);

        player.sendMessage(Component.text("Lap " + currentLap + ": ", NamedTextColor.GRAY)
                .append(Component.text(splitStr, NamedTextColor.AQUA)));

        if (remaining <= 1) {
            return true;
        }

        remaining--;
        lapsRemaining.put(uuid, remaining);
        player.sendActionBar(Component.text("Lap " + currentLap + ": " + splitStr, NamedTextColor.AQUA));

        return false;
    }

    public long stop(Player player) {
        return stop(player, false);
    }

    public long stop(Player player, boolean saveRecord) {
        long startNanos = runningTimers.removeLong(player.getUniqueId());
        boolean wasPending = pendingTimers.remove(player.getUniqueId());
        PlayerTimer timer = timerDetails.remove(player.getUniqueId());

        if (timer == null) {
            player.sendMessage(Component.text("You don't have an active timer.", NamedTextColor.RED));
            return -1;
        }
        if (startNanos == Long.MIN_VALUE) {
            if (wasPending) {
                player.sendMessage(Component.text("Pending timer cancelled.", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("Timer was never started.", NamedTextColor.RED));
            }
            return -1;
        }

        long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
        String formatted = TrackRecord.formatTime(elapsed);

        player.sendActionBar(Component.text(formatted, NamedTextColor.GOLD));

        // Handle track completion or standard message
        if (saveRecord && timer.type() == PlayerTimer.TimerType.TRACK && timer.trackName() != null) {
            handleTrackFinish(player, timer.trackName(), elapsed, formatted);
        } else {
            player.sendMessage(Component.text("Time: ", NamedTextColor.GRAY)
                    .append(Component.text(formatted, NamedTextColor.GOLD)));
        }

        cleanupActionBarTask();
        return elapsed;
    }

    private void handleTrackFinish(Player player, String trackName, long elapsed, String formattedTime) {
        // Always cache the latest run so it overwrites any previous pending run
        pendingSubmissions.put(player.getUniqueId(), new PendingRun(trackName, elapsed));

        long bestTime = dataStore.getPlayerBestTime(player.getUniqueId(), trackName);
        long leaderboardTime = dataStore.getPlayerLeaderboardTime(player.getUniqueId(), trackName);

        Component message = Component.text("Time: ", NamedTextColor.GRAY)
                .append(Component.text(formattedTime, NamedTextColor.GOLD));

        if (bestTime == -1 || elapsed < bestTime) {
            String extra = (bestTime == -1) ? " (First Run!)" : " (New Personal Best!)";
            player.sendMessage(message.append(Component.text(extra, NamedTextColor.LIGHT_PURPLE)));
        } else {
            String pbStr = TrackRecord.formatTime(bestTime);
            player.sendMessage(message.append(Component.text(" (PB: " + pbStr + ")", NamedTextColor.YELLOW)));
        }

        Track track = dataStore.getTrack(trackName);
        if (track != null && track.isLeaderboardEnabled()) {
            if (leaderboardTime == -1 || elapsed < leaderboardTime) {
                player.sendMessage(Component.text("Click here to submit to leaderboard", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/track submit"))
                        .hoverEvent(HoverEvent.showText(Component.text("Submit " + formattedTime))));
            }
        }
    }

    public void submitRun(Player player) {
        PendingRun run = pendingSubmissions.remove(player.getUniqueId());
        if (run == null) {
            player.sendMessage(Component.text("No pending run to submit.", NamedTextColor.RED));
            return;
        }

        Track track = dataStore.getTrack(run.trackName());
        if (track != null && !track.isLeaderboardEnabled()) {
            player.sendMessage(Component.text("Leaderboard for track '" + run.trackName() + "' is disabled.", NamedTextColor.RED));
            return;
        }

        TrackRecord record = new TrackRecord(player.getUniqueId(), player.getName(), run.timeMs());
        dataStore.addRecord(run.trackName(), record);
        player.sendMessage(Component.text("Run submitted!", NamedTextColor.GREEN));
    }

    public void reset(Player player) {
        runningTimers.removeLong(player.getUniqueId());
        pendingTimers.remove(player.getUniqueId());
        PlayerTimer removed = timerDetails.remove(player.getUniqueId());

        if (removed == null) {
            player.sendMessage(Component.text("You don't have an active timer.", NamedTextColor.RED));
            return;
        }
        player.sendActionBar(Component.text("Timer reset.", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Timer reset.", NamedTextColor.YELLOW));
        cleanupActionBarTask();
    }

    public void check(Player player) {
        long startNanos = runningTimers.getLong(player.getUniqueId());

        if (startNanos == Long.MIN_VALUE) {
            // Not running - maybe pending?
            if (pendingTimers.contains(player.getUniqueId())) {
                player.sendMessage(Component.text("Timer hasn't started yet. Move to begin!", NamedTextColor.AQUA));
            } else {
                player.sendMessage(Component.text("You don't have an active timer.", NamedTextColor.RED));
            }
            return;
        }

        long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
        String formatted = TrackRecord.formatTime(elapsed);
        player.sendMessage(Component.text("Elapsed: ", NamedTextColor.GRAY)
                .append(Component.text(formatted, NamedTextColor.GOLD)));
    }

    public void toggle(Player player) {
        if (hasTimer(player.getUniqueId())) {
            stop(player);
        } else {
            startManual(player);
        }
    }

    // --- Hot-path queries used by listeners ---

    public boolean isPending(UUID uuid) {
        return pendingTimers.contains(uuid);
    }

    public boolean isRunning(UUID uuid) {
        return runningTimers.containsKey(uuid);
    }

    public long getStartTime(UUID uuid) {
        return runningTimers.getLong(uuid);
    }

    public PlayerTimer getTimer(UUID playerUUID) {
        return timerDetails.get(playerUUID);
    }

    public boolean hasTimer(UUID playerUUID) {
        return timerDetails.containsKey(playerUUID);
    }

    public void removeTimer(UUID playerUUID) {
        runningTimers.removeLong(playerUUID);
        pendingTimers.remove(playerUUID);
        timerDetails.remove(playerUUID);
        pendingSubmissions.remove(playerUUID);
        lapsRemaining.removeInt(playerUUID);
        lastLapNanos.removeLong(playerUUID);
        cleanupActionBarTask();
    }

    // --- Action bar magic ^_^ ---

    private void ensureActionBarTask() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) return;
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (runningTimers.isEmpty()) {
                    this.cancel();
                    actionBarTask = null;
                    return;
                }
                long now = System.nanoTime();
                expiryList.clear();

                for (UUID uuid : runningTimers.keySet()) {
                    long startNanos = runningTimers.getLong(uuid);
                    if (startNanos == Long.MIN_VALUE) continue;

                    long elapsed = (now - startNanos) / 1_000_000;

                    if (elapsed > maxTimerMillis) {
                        expiryList.add(uuid);
                        continue;
                    }

                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;
                    String formatted = TrackRecord.formatTime(elapsed);
                    player.sendActionBar(Component.text(formatted, NamedTextColor.GOLD));
                }

                for (int i = 0; i < expiryList.size(); i++) {
                    UUID uuid = expiryList.get(i);
                    removeTimer(uuid);
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(Component.text("Timer expired (max duration reached).", NamedTextColor.RED));
                        player.sendActionBar(Component.text("Timer expired", NamedTextColor.RED));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void cleanupActionBarTask() {
        if (runningTimers.isEmpty() && actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    public void shutdown() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        runningTimers.clear();
        pendingTimers.clear();
        timerDetails.clear();
    }
}
