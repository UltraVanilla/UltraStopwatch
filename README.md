# UltraStopwatch

A stopwatch and racing plugin used on UltraVanilla.

## Features

*   **Manual Stopwatch**: Start, stop, and reset a personal timer.
*   **Tracks**: Define start and end points for racing tracks.
*   **Leaderboards**: Track best times for tracks and events.
*   **Events**: Group tracks into events with scoring (Total Time or Points).
*   **Async I/O**: All data storage and heavy calculations are handled asynchronously to prevent server lag.
*   **Strict Thread Safety**: Compliant with Bukkit API concurrency standards.

## Commands

### Stopwatch
*   `/stopwatch` (Aliases: `/sw`, `/timer`) - Toggle the stopwatch or view help.
*   `/sw start` - Start the manual stopwatch.
*   `/sw stop` - Stop the manual stopwatch.
*   `/sw reset` - Reset the manual stopwatch.
*   `/sw check` - Check the current time of the manual stopwatch.

### Casual Run
*   `/startrun` (Alias: `/sr`) - Start a casual timer (starts upon movement, stops when you walk over a pressure plate).

### Track Management
*   `/track list` - List all tracks.
*   `/track info <trackname>` - View details about a track.
*   `/track leaderboard <trackname>` - View the leaderboard for a track.
*   `/track run <trackname>` - Start a run on a specific track.
*   `/track submit` - Submit your last run time to the leaderboard (if applicable).
*   `/track create <trackname>` - Create a new track.
*   `/track delete <trackname>` - Delete a track.
*   `/track setstart <trackname>` - Set the start points (requires 2 points for a line).
*   `/track setend <trackname>` - Set the end points (requires 2 points for a line).
*   `/track settrigger <trackname> <type>` - Set trigger type (`pressure_plate` or `tripwire`).
*   `/track setdelay <trackname> <seconds>` - Set a delay before the finish line is active.
*   `/track setleaderboard <trackname> <enabled|disabled>` - Enable or disable the leaderboard.
*   `/track toggleleaderboard <trackname>` - Toggle the leaderboard status (enabled/disabled).
*   `/track resetleaderboard <trackname>` - Clear the leaderboard for a track.
*   `/track deleteplayer <trackname> <player>` - Remove a specific player from a track's leaderboard.

### Events
*   `/track event list` - List all events.
*   `/track event leaderboard <trackname>` - View the leaderboard for an event.
*   `/track event create <trackname> <scoring>` - Create an event (`total_time` or `points`).
*   `/track event delete <trackname>` - Delete an event.
*   `/track event addtrack <event> <track>` - Add a track to an event.
*   `/track event removetrack <event> <track>` - Remove a track from an event.
*   `/track event start <trackname>` - Start an event (enables leaderboards for all included tracks).
*   `/track event stop <trackname>` - Stop an event.

## Permissions

*   `ultrastopwatch.use` - Allows access to basic stopwatch, casual run, and track participation commands.
*   `ultrastopwatch.admin` - Allows access to track creation, configuration, and event management commands.

## License

Copyright (C) 2026 cutelilreno <https://github.com/cutelilreno>

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.