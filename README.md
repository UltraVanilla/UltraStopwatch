# UltraStopwatch - UNTESTED!!

A high-performance, thread-safe stopwatch and racing plugin for Minecraft servers.

## Features

*   **Manual Stopwatch**: Start, stop, and reset a personal timer.
*   **Tracks**: Define start and end points for racing tracks.
*   **Leaderboards**: Track best times for tracks and events.
*   **Events**: Group tracks into events with scoring (Total Time or Points).
*   **Async I/O**: All data storage and heavy calculations are handled asynchronously to prevent server lag.
*   **Strict Thread Safety**: Compliant with Bukkit API concurrency standards.

## Commands

*   `/sw start|stop|reset|check` - Manual stopwatch control.
*   `/track ...` - Manage tracks and view leaderboards.
*   `/sr` (Start Run) - Start a casual timer.

## License

Copyright (C) 2026 cutelilreno <https://github.com/cutelilreno>

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.