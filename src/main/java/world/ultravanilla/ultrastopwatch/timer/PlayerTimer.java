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

public record PlayerTimer(TimerType type, String trackName) {

    public enum TimerType {
        MANUAL,
        CASUAL,
        TRACK
    }

    public static PlayerTimer manual() {
        return new PlayerTimer(TimerType.MANUAL, null);
    }

    public static PlayerTimer casual() {
        return new PlayerTimer(TimerType.CASUAL, null);
    }

    public static PlayerTimer track(String trackName) {
        return new PlayerTimer(TimerType.TRACK, trackName);
    }
}
