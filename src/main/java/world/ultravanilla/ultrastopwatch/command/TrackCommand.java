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
package world.ultravanilla.ultrastopwatch.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import world.ultravanilla.ultrastopwatch.model.RaceEvent;
import world.ultravanilla.ultrastopwatch.model.Track;
import world.ultravanilla.ultrastopwatch.model.TrackRecord;
import world.ultravanilla.ultrastopwatch.storage.DataStore;
import world.ultravanilla.ultrastopwatch.timer.TimerManager;

import java.util.*;
import java.util.stream.Collectors;

public class TrackCommand {

    private final JavaPlugin plugin;
    private final DataStore dataStore;
    private final TimerManager timerManager;
    private final int leaderboardSize;
    private final List<Integer> pointsTable;

    public TrackCommand(JavaPlugin plugin, DataStore dataStore, TimerManager timerManager, int leaderboardSize, List<Integer> pointsTable) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.timerManager = timerManager;
        this.leaderboardSize = leaderboardSize;
        this.pointsTable = pointsTable;
    }

    public void register() {
        new CommandAPICommand("track")
                .withPermission("ultrastopwatch.use")
                // /track create <name>
                .withSubcommand(
                        new CommandAPICommand("create")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(new StringArgument("name"))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    if (dataStore.getTrack(name) != null) {
                                        player.sendMessage(Component.text("Track '" + name + "' already exists.", NamedTextColor.RED));
                                        return;
                                    }
                                    Track track = new Track(name.toLowerCase());
                                    dataStore.addTrack(track);
                                    player.sendMessage(Component.text("Track '" + name + "' created. Use ", NamedTextColor.GREEN)
                                            .append(Component.text("/track setstart " + name, NamedTextColor.YELLOW))
                                            .append(Component.text(" and ", NamedTextColor.GREEN))
                                            .append(Component.text("/track setend " + name, NamedTextColor.YELLOW))
                                            .append(Component.text(" to configure it.", NamedTextColor.GREEN)));
                                })
                )
                // /track delete <name>
                .withSubcommand(
                        new CommandAPICommand("delete")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(trackNameArgument())
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    Track removed = dataStore.removeTrack(name);
                                    if (removed == null) {
                                        player.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    player.sendMessage(Component.text("Track '" + name + "' deleted.", NamedTextColor.YELLOW));
                                })
                )
                // /track setstart <name>
                .withSubcommand(
                        new CommandAPICommand("setstart")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(trackNameArgument())
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    Track track = dataStore.getTrack(name);
                                    if (track == null) {
                                        player.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    if (!track.isStartASet() || track.isStartBSet()) {
                                        track.setStartPoint1(player.getLocation());
                                        dataStore.saveTracks();
                                        player.sendMessage(Component.text("Start line point 1 set at " + formatLocation(player.getLocation()) + ". Run ", NamedTextColor.YELLOW)
                                                .append(Component.text("/track setstart " + name, NamedTextColor.GOLD))
                                                .append(Component.text(" again to set point 2.", NamedTextColor.YELLOW)));
                                    } else {
                                        if (track.setStartPoint2(player.getLocation())) {
                                            dataStore.saveTracks();
                                            player.sendMessage(Component.text("Start line established!", NamedTextColor.GREEN));
                                        } else {
                                            player.sendMessage(Component.text("Invalid point 2. Must be aligned with point 1 (same X or Z) and same Y level.", NamedTextColor.RED));
                                        }
                                    }
                                })
                )
                // /track setend <name>
                .withSubcommand(
                        new CommandAPICommand("setend")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(trackNameArgument())
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    Track track = dataStore.getTrack(name);
                                    if (track == null) {
                                        player.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    if (!track.isEndASet() || track.isEndBSet()) {
                                        track.setEndPoint1(player.getLocation());
                                        dataStore.saveTracks();
                                        player.sendMessage(Component.text("Finish line point 1 set. Set point 2 to complete the line.", NamedTextColor.YELLOW));
                                    } else {
                                        if (track.setEndPoint2(player.getLocation())) {
                                            dataStore.saveTracks();
                                            player.sendMessage(Component.text("Finish line established!", NamedTextColor.GREEN));
                                        } else {
                                            player.sendMessage(Component.text("Invalid point 2. Must be aligned with point 1 (same X or Z) and same Y level.", NamedTextColor.RED));
                                        }
                                    }
                                })
                )
                // /track settrigger <name> <type>
                .withSubcommand(
                        new CommandAPICommand("settrigger")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(trackNameArgument())
                                .withArguments(new MultiLiteralArgument("type", "pressure_plate", "tripwire"))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    String type = (String) args.get("type");
                                    Track track = dataStore.getTrack(name);
                                    if (track == null) {
                                        player.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    Track.TriggerType triggerType = Track.TriggerType.valueOf(type.toUpperCase());
                                    track.setTriggerType(triggerType);
                                    dataStore.saveTracks();
                                    player.sendMessage(Component.text("Trigger type set to '" + type + "' for track '" + name + "'.", NamedTextColor.GREEN));
                                })
                )
                // /track setdelay <name> <seconds>
                .withSubcommand(
                        new CommandAPICommand("setdelay")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(trackNameArgument())
                                .withArguments(new LongArgument("seconds"))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    long seconds = (long) args.get("seconds");
                                    Track track = dataStore.getTrack(name);
                                    if (track == null) {
                                        player.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    track.setTriggerDelay(seconds * 1000);
                                    dataStore.saveTracks();
                                    player.sendMessage(Component.text("Trigger delay for track '" + name + "' set to " + seconds + "s.", NamedTextColor.GREEN));
                                })
                )
                // /track setlaps <name> <amount>
                .withSubcommand(
                        new CommandAPICommand("setlaps")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(trackNameArgument())
                                .withArguments(new IntegerArgument("amount", 1))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    int laps = (int) args.get("amount");
                                    Track track = dataStore.getTrack(name);
                                    if (track == null) {
                                        player.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    track.setLaps(laps);
                                    dataStore.saveTracks();
                                    player.sendMessage(Component.text("Laps for track '" + name + "' set to " + laps + ".", NamedTextColor.GREEN));
                                })
                )
                // /track setleaderboard <name> <enabled>
                .withSubcommand(
                        new CommandAPICommand("setleaderboard")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(trackNameArgument())
                                .withArguments(new BooleanArgument("enabled"))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    boolean enabled = (boolean) args.get("enabled");
                                    Track track = dataStore.getTrack(name);
                                    if (track == null) {
                                        player.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    track.setLeaderboardEnabled(enabled);
                                    dataStore.saveTracks();
                                    String status = enabled ? "enabled" : "disabled";
                                    player.sendMessage(Component.text("Leaderboard for track '" + name + "' " + status + ".", enabled ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
                                })
                )
                // /track toggleleaderboard <name>
                .withSubcommand(
                        new CommandAPICommand("toggleleaderboard")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(trackNameArgument())
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    Track track = dataStore.getTrack(name);
                                    if (track == null) {
                                        player.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    boolean newState = !track.isLeaderboardEnabled();
                                    track.setLeaderboardEnabled(newState);
                                    dataStore.saveTracks();
                                    String status = newState ? "enabled" : "disabled";
                                    player.sendMessage(Component.text("Leaderboard for track '" + name + "' is now " + status + ".", newState ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
                                })
                )
                // /track list
                .withSubcommand(
                        new CommandAPICommand("list")
                                .executes((sender, args) -> {
                                    var tracks = dataStore.getTracks();
                                    if (tracks.isEmpty()) {
                                        sender.sendMessage(Component.text("No tracks defined.", NamedTextColor.GRAY));
                                        return;
                                    }
                                    sender.sendMessage(Component.text("=== Tracks ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                                    for (Track track : tracks.values()) {
                                        String status = track.isFullyConfigured() ? "Ready" : "Incomplete";
                                        NamedTextColor statusColor = track.isFullyConfigured() ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
                                        sender.sendMessage(Component.text("  " + track.getName(), NamedTextColor.WHITE)
                                                .append(Component.text(" [" + status + "]", statusColor))
                                                .append(Component.text(" - " + track.getTriggerType().name().toLowerCase(), NamedTextColor.GRAY)));
                                    }
                                })
                )
                // /track info <name>
                .withSubcommand(
                        new CommandAPICommand("info")
                                .withArguments(trackNameArgument())
                                .executes((sender, args) -> {
                                    String name = (String) args.get("name");
                                    Track track = dataStore.getTrack(name);
                                    if (track == null) {
                                        sender.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    String startStr = track.isStartBSet() ?
                                            formatLocation(track.getStartPoint1()) + " -> " + formatLocation(track.getStartPoint2()) :
                                            (track.isStartASet() ? "Point 1: " + formatLocation(track.getStartPoint1()) : "Not set");

                                    String endStr = track.isEndBSet() ?
                                            formatLocation(track.getEndPoint1()) + " -> " + formatLocation(track.getEndPoint2()) :
                                            (track.isEndASet() ? "Point 1: " + formatLocation(track.getEndPoint1()) : "Not set");

                                    sender.sendMessage(Component.text("=== Track: " + track.getName() + " ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                                    sender.sendMessage(Component.text("  World: ", NamedTextColor.GRAY).append(Component.text(track.getWorld() != null ? track.getWorld() : "Not set", NamedTextColor.WHITE)));
                                    sender.sendMessage(Component.text("  Start: ", NamedTextColor.GRAY).append(Component.text(startStr, NamedTextColor.WHITE)));
                                    sender.sendMessage(Component.text("  End: ", NamedTextColor.GRAY).append(Component.text(endStr, NamedTextColor.WHITE)));
                                    sender.sendMessage(Component.text("  Trigger: ", NamedTextColor.GRAY).append(Component.text(track.getTriggerType().name().toLowerCase(), NamedTextColor.WHITE)));
                                    sender.sendMessage(Component.text("  Trigger Delay: ", NamedTextColor.GRAY).append(Component.text((track.getTriggerDelay() / 1000) + "s", NamedTextColor.WHITE)));
                                    sender.sendMessage(Component.text("  Laps: ", NamedTextColor.GRAY).append(Component.text(track.getLaps(), NamedTextColor.WHITE)));
                                    sender.sendMessage(Component.text("  Leaderboard: ", NamedTextColor.GRAY).append(Component.text(track.isLeaderboardEnabled() ? "Enabled" : "Disabled", track.isLeaderboardEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)));
                                })
                )
                // /track leaderboard <name>
                .withSubcommand(
                        new CommandAPICommand("leaderboard")
                                .withArguments(trackNameArgument())
                                .executes((sender, args) -> {
                                    String name = (String) args.get("name");
                                    Track track = dataStore.getTrack(name);
                                    if (track == null) {
                                        sender.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    showTrackLeaderboard(sender, name);
                                })
                )
                // /track resetleaderboard <name>
                .withSubcommand(
                        new CommandAPICommand("resetleaderboard")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(trackNameArgument())
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    if (dataStore.getTrack(name) == null) {
                                        player.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    dataStore.resetTrackLeaderboard(name);
                                    player.sendMessage(Component.text("Leaderboard for track '" + name + "' has been reset.", NamedTextColor.YELLOW));
                                })
                )
                // /track deleteplayer <name> <player>
                .withSubcommand(
                        new CommandAPICommand("deleteplayer")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(trackNameArgument())
                                .withArguments(new StringArgument("player"))
                                .executesPlayer((player, args) -> {
                                    String trackName = (String) args.get("name");
                                    String playerName = (String) args.get("player");
                                    if (dataStore.getTrack(trackName) == null) {
                                        player.sendMessage(Component.text("Track '" + trackName + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    if (dataStore.deletePlayerRecord(trackName, playerName)) {
                                        player.sendMessage(Component.text("Removed " + playerName + " from track '" + trackName + "' leaderboard.", NamedTextColor.GREEN));
                                    } else {
                                        player.sendMessage(Component.text("Player '" + playerName + "' not found on leaderboard for track '" + trackName + "'.", NamedTextColor.RED));
                                    }
                                })
                )
                // /track run <name>
                .withSubcommand(
                        new CommandAPICommand("run")
                                .withArguments(trackNameArgument())
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    Track track = dataStore.getTrack(name);
                                    if (track == null) {
                                        player.sendMessage(Component.text("Track '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    if (!track.isFullyConfigured()) {
                                        player.sendMessage(Component.text("Track '" + name + "' is not fully configured yet.", NamedTextColor.RED));
                                        return;
                                    }
                                    timerManager.startTrackRun(player, name.toLowerCase());
                                })
                )
                // /track submit
                .withSubcommand(
                        new CommandAPICommand("submit")
                                .executesPlayer((player, args) -> {
                                    timerManager.submitRun(player);
                                })
                )
                // /track event subcommands
                .withSubcommand(buildEventSubcommands())
                .executes((sender, args) -> {
                    sender.sendMessage(Component.text("Usage: /track <create|delete|setstart|setend|settrigger|setdelay|setlaps|setleaderboard|toggleleaderboard|resetleaderboard|list|info|leaderboard|run|submit|event>", NamedTextColor.YELLOW));
                })
                .register();
    }

    private CommandAPICommand buildEventSubcommands() {
        return new CommandAPICommand("event")
                // /track event create <name> <scoring>
                .withSubcommand(
                        new CommandAPICommand("create")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(new StringArgument("name"))
                                .withArguments(new MultiLiteralArgument("scoring", "total_time", "points"))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    String scoring = (String) args.get("scoring");
                                    if (dataStore.getEvent(name) != null) {
                                        player.sendMessage(Component.text("Event '" + name + "' already exists.", NamedTextColor.RED));
                                        return;
                                    }
                                    RaceEvent.ScoringType scoringType = RaceEvent.ScoringType.valueOf(scoring.toUpperCase());
                                    RaceEvent event = new RaceEvent(name.toLowerCase(), scoringType);
                                    dataStore.addEvent(event);
                                    player.sendMessage(Component.text("Event '" + name + "' created with scoring: " + scoring, NamedTextColor.GREEN));
                                })
                )
                // /track event delete <name>
                .withSubcommand(
                        new CommandAPICommand("delete")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(eventNameArgument())
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    RaceEvent removed = dataStore.removeEvent(name);
                                    if (removed == null) {
                                        player.sendMessage(Component.text("Event '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    for (String trackName : removed.getTrackNames()) {
                                        dataStore.resetTrackLeaderboard(trackName);
                                    }
                                    player.sendMessage(Component.text("Event '" + name + "' deleted and associated track leaderboards purged.", NamedTextColor.YELLOW));
                                })
                )
                // /track event addtrack <event> <track>
                .withSubcommand(
                        new CommandAPICommand("addtrack")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(eventNameArgument())
                                .withArguments(new StringArgument("track").replaceSuggestions(
                                        ArgumentSuggestions.strings(info -> dataStore.getTracks().keySet().toArray(new String[0]))))
                                .executesPlayer((player, args) -> {
                                    String eventName = (String) args.get("name");
                                    String trackName = (String) args.get("track");
                                    RaceEvent event = dataStore.getEvent(eventName);
                                    if (event == null) {
                                        player.sendMessage(Component.text("Event '" + eventName + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    if (dataStore.getTrack(trackName) == null) {
                                        player.sendMessage(Component.text("Track '" + trackName + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    event.addTrack(trackName.toLowerCase());
                                    dataStore.saveEvents();
                                    player.sendMessage(Component.text("Added track '" + trackName + "' to event '" + eventName + "'.", NamedTextColor.GREEN));
                                })
                )
                // /track event removetrack <event> <track>
                .withSubcommand(
                        new CommandAPICommand("removetrack")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(eventNameArgument())
                                .withArguments(new StringArgument("track").replaceSuggestions(
                                        ArgumentSuggestions.strings(info -> {
                                            String eventName = (String) info.previousArgs().get("name");
                                            RaceEvent event = dataStore.getEvent(eventName);
                                            if (event == null) return new String[0];
                                            return event.getTrackNames().toArray(new String[0]);
                                        })))
                                .executesPlayer((player, args) -> {
                                    String eventName = (String) args.get("name");
                                    String trackName = (String) args.get("track");
                                    RaceEvent event = dataStore.getEvent(eventName);
                                    if (event == null) {
                                        player.sendMessage(Component.text("Event '" + eventName + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    if (!event.removeTrack(trackName.toLowerCase())) {
                                        player.sendMessage(Component.text("Track '" + trackName + "' is not in event '" + eventName + "'.", NamedTextColor.RED));
                                        return;
                                    }
                                    dataStore.saveEvents();
                                    player.sendMessage(Component.text("Removed track '" + trackName + "' from event '" + eventName + "'.", NamedTextColor.YELLOW));
                                })
                )
                // /track event start <name>
                .withSubcommand(
                        new CommandAPICommand("start")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(eventNameArgument())
                                .executes((sender, args) -> {
                                    String name = (String) args.get("name");
                                    RaceEvent event = dataStore.getEvent(name);
                                    if (event == null) {
                                        sender.sendMessage(Component.text("Event '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    if (event.isActive()) {
                                        sender.sendMessage(Component.text("Event '" + name + "' is already active.", NamedTextColor.RED));
                                        return;
                                    }
                                    event.setActive(true);
                                    for (String trackName : event.getTrackNames()) {
                                        Track track = dataStore.getTrack(trackName);
                                        if (track != null) {
                                            dataStore.resetTrackLeaderboard(trackName);
                                            track.setLeaderboardEnabled(true);
                                        }
                                    }
                                    dataStore.saveTracks();
                                    dataStore.saveEvents();
                                    sender.sendMessage(Component.text("Event '" + name + "' is now active! Track leaderboards reset and enabled.", NamedTextColor.GREEN));
                                })
                )
                // /track event stop <name>
                .withSubcommand(
                        new CommandAPICommand("stop")
                                .withPermission("ultrastopwatch.admin")
                                .withArguments(eventNameArgument())
                                .executes((sender, args) -> {
                                    String name = (String) args.get("name");
                                    RaceEvent event = dataStore.getEvent(name);
                                    if (event == null) {
                                        sender.sendMessage(Component.text("Event '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    event.setActive(false);
                                    for (String trackName : event.getTrackNames()) {
                                        Track track = dataStore.getTrack(trackName);
                                        if (track != null) {
                                            track.setLeaderboardEnabled(false);
                                        }
                                    }
                                    dataStore.saveTracks();
                                    dataStore.saveEvents();
                                    sender.sendMessage(Component.text("Event '" + name + "' has been stopped. Track leaderboards disabled.", NamedTextColor.YELLOW));
                                })
                )
                // /track event info <name>
                .withSubcommand(
                        new CommandAPICommand("info")
                                .withArguments(eventNameArgument())
                                .executes((sender, args) -> {
                                    String name = (String) args.get("name");
                                    RaceEvent event = dataStore.getEvent(name);
                                    if (event == null) {
                                        sender.sendMessage(Component.text("Event '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    sender.sendMessage(Component.text("=== Event: " + event.getName() + " ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                                    sender.sendMessage(Component.text("  Status: ", NamedTextColor.GRAY).append(Component.text(event.isActive() ? "Active" : "Inactive", event.isActive() ? NamedTextColor.GREEN : NamedTextColor.RED)));
                                    sender.sendMessage(Component.text("  Scoring: ", NamedTextColor.GRAY).append(Component.text(event.getScoringType().name(), NamedTextColor.WHITE)));
                                    sender.sendMessage(Component.text("  Tracks: ", NamedTextColor.GRAY).append(Component.text(String.join(", ", event.getTrackNames()), NamedTextColor.WHITE)));
                                })
                )
                // /track event list
                .withSubcommand(
                        new CommandAPICommand("list")
                                .executes((sender, args) -> {
                                    var events = dataStore.getEvents();
                                    if (events.isEmpty()) {
                                        sender.sendMessage(Component.text("No events defined.", NamedTextColor.GRAY));
                                        return;
                                    }
                                    sender.sendMessage(Component.text("=== Events ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                                    for (RaceEvent event : events.values()) {
                                        NamedTextColor statusColor = event.isActive() ? NamedTextColor.GREEN : NamedTextColor.RED;
                                        String status = event.isActive() ? "Active" : "Inactive";
                                        sender.sendMessage(Component.text("  " + event.getName(), NamedTextColor.WHITE)
                                                .append(Component.text(" [" + status + "]", statusColor))
                                                .append(Component.text(" - " + event.getScoringType().name().toLowerCase() + " (" + event.getTrackNames().size() + " tracks)", NamedTextColor.GRAY)));
                                    }
                                })
                )
                // /track event leaderboard <name>
                .withSubcommand(
                        new CommandAPICommand("leaderboard")
                                .withArguments(eventNameArgument())
                                .executes((sender, args) -> {
                                    String name = (String) args.get("name");
                                    RaceEvent event = dataStore.getEvent(name);
                                    if (event == null) {
                                        sender.sendMessage(Component.text("Event '" + name + "' not found.", NamedTextColor.RED));
                                        return;
                                    }
                                    showEventLeaderboard(sender, event);
                                })
                )
                .executes((sender, args) -> {
                    sender.sendMessage(Component.text("Usage: /track event <create|delete|addtrack|removetrack|start|stop|list|info|leaderboard>", NamedTextColor.YELLOW));
                });
    }

    private void showTrackLeaderboard(CommandSender sender, String trackName) {
        sender.sendMessage(Component.text("Calculating leaderboard...", NamedTextColor.GRAY));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<TrackRecord> records = dataStore.getRecords(trackName);
            // Get best time per player, sorted
            Map<UUID, TrackRecord> bestPerPlayer = new LinkedHashMap<>();
            synchronized (records) {
                if (records.isEmpty()) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                        sender.sendMessage(Component.text("No records for track '" + trackName + "'.", NamedTextColor.GRAY))
                    );
                    return;
                }
                for (TrackRecord record : records) {
                    bestPerPlayer.merge(record.getPlayerUUID(), record, (a, b) -> a.getTimeMs() <= b.getTimeMs() ? a : b);
                }
            }
            List<TrackRecord> sorted = bestPerPlayer.values().stream()
                    .sorted()
                    .limit(leaderboardSize)
                    .collect(Collectors.toList());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(Component.text("=== Leaderboard: " + trackName + " ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                for (int i = 0; i < sorted.size(); i++) {
                    TrackRecord record = sorted.get(i);
                    NamedTextColor color = i == 0 ? NamedTextColor.GOLD : i == 1 ? NamedTextColor.GRAY : i == 2 ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;
                    sender.sendMessage(Component.text("  #" + (i + 1) + " ", color)
                            .append(Component.text(record.getPlayerName(), NamedTextColor.WHITE))
                            .append(Component.text(" - " + TrackRecord.formatTime(record.getTimeMs()), NamedTextColor.AQUA)));
                }
            });
        });
    }

    private void showEventLeaderboard(CommandSender sender, RaceEvent event) {
        List<String> trackNames = new ArrayList<>(event.getTrackNames());
        if (trackNames.isEmpty()) {
            sender.sendMessage(Component.text("Event '" + event.getName() + "' has no tracks.", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("Calculating leaderboard...", NamedTextColor.GRAY));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Gather all players who have records on any track in the event
            Set<UUID> allPlayers = new HashSet<>();
            Map<UUID, String> playerNames = new HashMap<>();
            Map<String, Map<UUID, Long>> bestTimesPerTrack = new HashMap<>();

            for (String trackName : trackNames) {
                List<TrackRecord> records = dataStore.getRecords(trackName);
                Map<UUID, Long> bestTimes = new HashMap<>();
                synchronized (records) {
                    for (TrackRecord record : records) {
                        allPlayers.add(record.getPlayerUUID());
                        playerNames.put(record.getPlayerUUID(), record.getPlayerName());
                        bestTimes.merge(record.getPlayerUUID(), record.getTimeMs(), Math::min);
                    }
                }
                bestTimesPerTrack.put(trackName, bestTimes);
            }

            if (allPlayers.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    sender.sendMessage(Component.text("No records for any tracks in event '" + event.getName() + "'.", NamedTextColor.GRAY))
                );
                return;
            }

            if (event.getScoringType() == RaceEvent.ScoringType.TOTAL_TIME) {
                showTotalTimeLeaderboard(sender, event, allPlayers, playerNames, bestTimesPerTrack, trackNames);
            } else {
                showPointsLeaderboard(sender, event, allPlayers, playerNames, bestTimesPerTrack, trackNames);
            }
        });
    }

    private void showTotalTimeLeaderboard(CommandSender sender, RaceEvent event,
                                           Set<UUID> allPlayers, Map<UUID, String> playerNames,
                                           Map<String, Map<UUID, Long>> bestTimesPerTrack, List<String> trackNames) {
        // Calculate total time for each player (only if they have times on all tracks)
        Map<UUID, Long> totalTimes = new HashMap<>();
        for (UUID uuid : allPlayers) {
            long total = 0;
            boolean complete = true;
            for (String trackName : trackNames) {
                Long best = bestTimesPerTrack.getOrDefault(trackName, Map.of()).get(uuid);
                if (best == null) {
                    complete = false;
                    break;
                }
                total += best;
            }
            if (complete) {
                totalTimes.put(uuid, total);
            }
        }

        List<Map.Entry<UUID, Long>> sorted = totalTimes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(leaderboardSize)
                .collect(Collectors.toList());

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(Component.text("=== Event: " + event.getName() + " (Total Time) ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            if (sorted.isEmpty()) {
                sender.sendMessage(Component.text("  No players have completed all tracks.", NamedTextColor.GRAY));
                return;
            }
            for (int i = 0; i < sorted.size(); i++) {
                Map.Entry<UUID, Long> entry = sorted.get(i);
                NamedTextColor color = i == 0 ? NamedTextColor.GOLD : i == 1 ? NamedTextColor.GRAY : i == 2 ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;
                sender.sendMessage(Component.text("  #" + (i + 1) + " ", color)
                        .append(Component.text(playerNames.get(entry.getKey()), NamedTextColor.WHITE))
                        .append(Component.text(" - " + TrackRecord.formatTime(entry.getValue()), NamedTextColor.AQUA)));
            }
        });
    }

    private void showPointsLeaderboard(CommandSender sender, RaceEvent event,
                                        Set<UUID> allPlayers, Map<UUID, String> playerNames,
                                        Map<String, Map<UUID, Long>> bestTimesPerTrack, List<String> trackNames) {
        // Calculate points for each player across all tracks
        Map<UUID, Integer> playerPoints = new HashMap<>();

        for (String trackName : trackNames) {
            Map<UUID, Long> bestTimes = bestTimesPerTrack.getOrDefault(trackName, Map.of());
            List<Map.Entry<UUID, Long>> ranked = bestTimes.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toList());

            for (int i = 0; i < ranked.size(); i++) {
                int points = i < pointsTable.size() ? pointsTable.get(i) : 0;
                playerPoints.merge(ranked.get(i).getKey(), points, Integer::sum);
            }
        }

        List<Map.Entry<UUID, Integer>> sorted = playerPoints.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(leaderboardSize)
                .collect(Collectors.toList());

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(Component.text("=== Event: " + event.getName() + " (Points) ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            if (sorted.isEmpty()) {
                sender.sendMessage(Component.text("  No records yet.", NamedTextColor.GRAY));
                return;
            }
            for (int i = 0; i < sorted.size(); i++) {
                Map.Entry<UUID, Integer> entry = sorted.get(i);
                NamedTextColor color = i == 0 ? NamedTextColor.GOLD : i == 1 ? NamedTextColor.GRAY : i == 2 ? NamedTextColor.DARK_RED : NamedTextColor.WHITE;
                sender.sendMessage(Component.text("  #" + (i + 1) + " ", color)
                        .append(Component.text(playerNames.get(entry.getKey()), NamedTextColor.WHITE))
                        .append(Component.text(" - " + entry.getValue() + " pts", NamedTextColor.AQUA)));
            }
        });
    }

    private StringArgument trackNameArgument() {
        return (StringArgument) new StringArgument("name").replaceSuggestions(
                ArgumentSuggestions.strings(info -> dataStore.getTracks().keySet().toArray(new String[0])));
    }

    private StringArgument eventNameArgument() {
        return (StringArgument) new StringArgument("name").replaceSuggestions(
                ArgumentSuggestions.strings(info -> dataStore.getEvents().keySet().toArray(new String[0])));
    }

    private String formatLocation(org.bukkit.Location loc) {
        if (loc == null) return "Not set";
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }
}
