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
package world.ultravanilla.ultrastopwatch.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;
import world.ultravanilla.ultrastopwatch.model.RaceEvent;
import world.ultravanilla.ultrastopwatch.model.Track;
import world.ultravanilla.ultrastopwatch.model.TrackRecord;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DataStore {

    private final Path dataDir;
    private final Path tracksFile;
    private final Path recordsDir;
    private final Path playerRecordsDir;
    private final Path eventsFile;
    private final Gson gson;
    private final Logger logger;
    private final JavaPlugin plugin;
    private final ExecutorService ioExecutor;

    private final Map<String, Track> tracks = new ConcurrentHashMap<>();
    private final Map<String, List<TrackRecord>> records = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, List<Long>>> playerRecords = new ConcurrentHashMap<>();
    private final Map<String, RaceEvent> events = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    public DataStore(Path dataDir, Logger logger, JavaPlugin plugin) {
        this.dataDir = dataDir;
        this.tracksFile = dataDir.resolve("tracks.json");
        this.recordsDir = dataDir.resolve("records").resolve("tracks");
        this.playerRecordsDir = dataDir.resolve("records").resolve("player");
        this.eventsFile = dataDir.resolve("events.json");
        this.logger = logger;
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.ioExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean load() {
        try {
            Files.createDirectories(recordsDir);
            Files.createDirectories(playerRecordsDir);
        } catch (IOException e) {
            logger.severe("Failed to create data directories: " + e.getMessage());
            return false;
        }

        if (!loadTracks()) return false;
        if (!loadAllRecords()) return false;
        if (!loadEvents()) return false;
        loaded = true;
        return true;
    }

    
    // --- Synchronous save for shutdown ---
    // lowk scared ive missed something here T.T
    public void saveAll() {
        ioExecutor.shutdown();
            try {
                // 2. Wait a moment for active background tasks to finish
                if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    ioExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                ioExecutor.shutdownNow();
                Thread.currentThread().interrupt(); // Re-set the status for the server
            }

        if (!loaded) {
            logger.warning("DataStore was not fully loaded. Skipping save to prevent data loss.");
            Thread.currentThread().interrupt();
            return;
        }

        saveFileSync(tracksFile, gson.toJson(tracks));
        for (Map.Entry<String, List<TrackRecord>> entry : records.entrySet()) {
            List<TrackRecord> trackRecords = entry.getValue();
            if (trackRecords != null && !trackRecords.isEmpty()) {
                Path file = recordsDir.resolve(entry.getKey().toLowerCase() + ".json");
                saveFileSync(file, gson.toJson(trackRecords));
            }
        }
        for (Map.Entry<UUID, Map<String, List<Long>>> entry : playerRecords.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                Path file = playerRecordsDir.resolve(entry.getKey().toString() + ".json");
                saveFileSync(file, gson.toJson(entry.getValue()));
            }
        }
        saveFileSync(eventsFile, gson.toJson(events));
    }

    // --- Atomic write helpers ---

    private void saveFileSync(Path target, String json) {
        Path tmpFile = target.resolveSibling(target.getFileName().toString() + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.writeString(tmpFile, json, StandardCharsets.UTF_8);
            Files.move(tmpFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.severe("Failed to save " + target.getFileName() + ": " + e.getMessage());
            try { Files.deleteIfExists(tmpFile); } catch (IOException ignored) {}
        }
    }

    // --- Tracks ---

    private boolean loadTracks() {
        if (!Files.exists(tracksFile)) return true;
        try (Reader reader = Files.newBufferedReader(tracksFile, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Track>>() {}.getType();
            Map<String, Track> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                tracks.putAll(loaded);
            }
            return true;
        } catch (IOException | JsonSyntaxException e) {
            logger.severe("Failed to load tracks: " + e.getMessage());
            return false;
        }
    }

    public void saveTracks() {
        // Serialize on main thread
        String json = gson.toJson(tracks);
        ioExecutor.submit(() -> saveFileSync(tracksFile, json));
    }

    public Map<String, Track> getTracks() {
        return tracks;
    }

    public Track getTrack(String name) {
        return tracks.get(name.toLowerCase());
    }

    public void addTrack(Track track) {
        tracks.put(track.getName().toLowerCase(), track);
        saveTracks();
    }

    public Track removeTrack(String name) {
        Track removed = tracks.remove(name.toLowerCase());
        if (removed != null) {
            saveTracks();
            records.remove(name.toLowerCase());
            Path recordFile = recordsDir.resolve(name.toLowerCase() + ".json");
            ioExecutor.submit(() -> {
                try {
                    Files.deleteIfExists(recordFile);
                } catch (IOException e) {
                    logger.warning("Failed to delete record file for track " + name + ": " + e.getMessage());
                }
            });
        }
        return removed;
    }

    // --- Records ---

    private boolean loadAllRecords() {
        if (!Files.exists(recordsDir)) return true;
        try (Stream<Path> files = Files.list(recordsDir)) {
            for (Path p : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                if (!loadRecordFile(p)) return false;
            }
            return true;
        } catch (IOException e) {
            logger.severe("Failed to list record files: " + e.getMessage());
            return false;
        }
    }

    private boolean loadRecordFile(Path file) {
        String fileName = file.getFileName().toString();
        String trackName = fileName.substring(0, fileName.length() - 5); // strip .json
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<TrackRecord>>() {}.getType();
            List<TrackRecord> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                records.put(trackName, Collections.synchronizedList(new ArrayList<>(loaded)));
            }
            return true;
        } catch (IOException | JsonSyntaxException e) {
            logger.severe("Failed to load records for track " + trackName + ": " + e.getMessage());
            return false;
        }
    }

    private void saveRecordsForTrack(String trackName) {
        List<TrackRecord> trackRecords = records.get(trackName.toLowerCase());
        if (trackRecords == null || trackRecords.isEmpty()) return;
        
        // Snapshot for async
        List<TrackRecord> snapshot;
        synchronized (trackRecords) {
            snapshot = new ArrayList<>(trackRecords);
        }
        Path file = recordsDir.resolve(trackName.toLowerCase() + ".json");
        
        ioExecutor.submit(() -> {
            Collections.sort(snapshot);
            String json = gson.toJson(snapshot);
            saveFileSync(file, json);
        });
    }

    public List<TrackRecord> getRecords(String trackName) {
        String key = trackName.toLowerCase();
        List<TrackRecord> list = records.get(key);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<>());
            List<TrackRecord> existing = records.putIfAbsent(key, list);
            if (existing != null) {
                list = existing;
            }
        }
        return list;
    }

    // --- Player Records ---

    public void loadPlayerRecords(UUID uuid) {
        ioExecutor.submit(() -> {
            Path file = playerRecordsDir.resolve(uuid.toString() + ".json");
            if (!Files.exists(file)) return;

            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, List<Long>>>() {}.getType();
                Map<String, List<Long>> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    Map<String, List<Long>> prepared = new ConcurrentHashMap<>();
                    for (Map.Entry<String, List<Long>> entry : loaded.entrySet()) {
                        prepared.put(entry.getKey(), Collections.synchronizedList(new ArrayList<>(entry.getValue())));
                    }
                    playerRecords.merge(uuid, prepared, (existing, fromDisk) -> {
                        for (Map.Entry<String, List<Long>> entry : fromDisk.entrySet()) {
                            existing.merge(entry.getKey(), entry.getValue(), (currentList, diskList) -> {
                                synchronized (currentList) {
                                    currentList.addAll(diskList);
                                    Collections.sort(currentList);
                                    while (currentList.size() > 3) {
                                        currentList.remove(currentList.size() - 1);
                                    }
                                }
                                return currentList;
                            });
                        }
                        return existing;
                    });
                }
            } catch (Exception e) {
                logger.warning("Failed to load player records for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void unloadPlayerRecords(UUID uuid) {
        if (ioExecutor.isShutdown()) return;
        ioExecutor.submit(() -> {
            Map<String, List<Long>> records = playerRecords.remove(uuid);
            if (records != null) {
                savePlayerRecordsSnapshot(uuid, records);
            }
        });
    }

    private void savePlayerRecords(UUID uuid) {
        ioExecutor.submit(() -> {
            Map<String, List<Long>> records = playerRecords.get(uuid);
            if (records != null) {
                savePlayerRecordsSnapshot(uuid, records);
            }
        });
    }

    private void savePlayerRecordsSnapshot(UUID uuid, Map<String, List<Long>> records) {
        Map<String, List<Long>> snapshot = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : records.entrySet()) {
            synchronized (entry.getValue()) {
                snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        Path file = playerRecordsDir.resolve(uuid.toString() + ".json");

        String json = gson.toJson(snapshot);
        saveFileSync(file, json);
    }

    public boolean addRecord(String trackName, TrackRecord record) {
        boolean isPersonalBest = addPlayerRecord(record.getPlayerUUID(), trackName, record.getTimeMs());

        Track track = getTrack(trackName);
        if (track != null && track.isLeaderboardEnabled()) {
            addGlobalRecord(trackName, record);
        }
        return isPersonalBest;
    }

    private void addGlobalRecord(String trackName, TrackRecord record) {
        List<TrackRecord> trackRecords = getRecords(trackName);
        boolean updated = false;
        synchronized (trackRecords) {
            long bestTime = Long.MAX_VALUE;
            for (TrackRecord r : trackRecords) {
                if (r.getPlayerUUID().equals(record.getPlayerUUID())) {
                    bestTime = Math.min(bestTime, r.getTimeMs());
                }
            }

            if (record.getTimeMs() < bestTime) {
                trackRecords.removeIf(r -> r.getPlayerUUID().equals(record.getPlayerUUID()));
                trackRecords.add(record);
                updated = true;
            }
        }
        
        if (updated) {
            saveRecordsForTrack(trackName);
        }
    }

    private boolean addPlayerRecord(UUID uuid, String trackName, long timeMs) {
        Map<String, List<Long>> pRecords = playerRecords.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        List<Long> times = pRecords.computeIfAbsent(trackName.toLowerCase(), k -> Collections.synchronizedList(new ArrayList<>()));
        
        boolean isBest = false;
        synchronized (times) {
            if (times.isEmpty() || timeMs < times.get(0)) {
                isBest = true;
            }
            times.add(timeMs);
            Collections.sort(times);
            if (times.size() > 3) {
                times.remove(times.size() - 1);
            }
        }
        savePlayerRecords(uuid);
        return isBest;
    }

    public void resetTrackLeaderboard(String trackName) {
        records.remove(trackName.toLowerCase());
        Path file = recordsDir.resolve(trackName.toLowerCase() + ".json");
        ioExecutor.submit(() -> {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {}
        });
    }

    // --- Events ---

    private boolean loadEvents() {
        if (!Files.exists(eventsFile)) return true;
        try (Reader reader = Files.newBufferedReader(eventsFile, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, RaceEvent>>() {}.getType();
            Map<String, RaceEvent> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                events.putAll(loaded);
            }
            return true;
        } catch (IOException | JsonSyntaxException e) {
            logger.severe("Failed to load events: " + e.getMessage());
            return false;
        }
    }

    public void saveEvents() {
        // Serialize on main thread
        String json = gson.toJson(events);
        ioExecutor.submit(() -> saveFileSync(eventsFile, json));
    }

    public Map<String, RaceEvent> getEvents() {
        return events;
    }

    public RaceEvent getEvent(String name) {
        return events.get(name.toLowerCase());
    }

    public void addEvent(RaceEvent event) {
        events.put(event.getName().toLowerCase(), event);
        saveEvents();
    }

    public RaceEvent removeEvent(String name) {
        RaceEvent removed = events.remove(name.toLowerCase());
        if (removed != null) {
            saveEvents();
        }
        return removed;
    }
}
