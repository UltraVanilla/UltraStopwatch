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

### User Commands

#### Stopwatch
*   `/sw` - Toggle the stopwatch or view help.
*   `/sw start` - Start the manual stopwatch.
*   `/sw stop` - Stop the manual stopwatch.
*   `/sw reset` - Reset the manual stopwatch.
*   `/sw check` - Check the current time of the manual stopwatch.

#### Casual Run
*   `/startrun` (Alias: `/sr`) - Start a casual timer (starts upon movement, stops when you walk over a pressure plate).

#### Tracks
*   `/track list` - List all tracks.
*   `/track info <track>` - View details about a track.
*   `/track leaderboard <track>` - View the leaderboard for a track.
*   `/track run <track>` - Start a run on a specific track.
*   `/track submit` - Submit your last run time to the leaderboard (if applicable).

#### Events
*   `/track event list` - List all events.
*   `/track event info <event>` - View details about an event.
*   `/track event leaderboard <event>` - View the leaderboard for an event.

### Admin Commands

#### Track Management
*   `/track create <track>` - Create a new track.
*   `/track delete <track>` - Delete a track.
*   `/track setstart <track>` - Set the start points (requires 2 points for a line).
*   `/track setend <track>` - Set the end points (requires 2 points for a line).
*   `/track settrigger <track> <type>` - Set trigger type (`pressure_plate` or `tripwire`).
*   `/track setdelay <track> <seconds>` - Set a delay before the finish line is active.
*   `/track setlaps <track> <laps>` - Set the number of laps for a track.
*   `/track setleaderboard <track> <true|false>` - Enable or disable the leaderboard.
*   `/track toggleleaderboard <track>` - Toggle the leaderboard status (enabled/disabled).
*   `/track resetleaderboard <track>` - Clear the leaderboard for a track.
*   `/track deleteplayer <track> <player>` - Remove a specific player from a track's leaderboard.

#### Event Management
*   `/track event create <event> <scoring>` - Create an event (`total_time` or `points`).
*   `/track event delete <event>` - Delete an event.
*   `/track event addtrack <event> <track>` - Add a track to an event.
*   `/track event removetrack <event> <track>` - Remove a track from an event.
*   `/track event start <event>` - Start an event (enables leaderboards for all included tracks).
*   `/track event stop <event>` - Stop an event.

## Permissions

*   `ultrastopwatch.use` - Allows access to basic stopwatch, casual run, and track participation commands.
*   `ultrastopwatch.admin` - Allows access to track creation, configuration, and event management commands.

## License

Copyright (C) 2026 cutelilreno <https://github.com/cutelilreno>

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.