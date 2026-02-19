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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import world.ultravanilla.ultrastopwatch.timer.TimerManager;

public class StopwatchCommand {

    private final TimerManager timerManager;

    public StopwatchCommand(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    public void register() {

        // /sw
        new CommandAPICommand("sw")
            .withPermission("ultrastopwatch.use")
            .executesPlayer((player, args) -> {
                timerManager.toggle(player);
            })
            .register();

        // /sw <start|stop|reset|check>    
        new CommandAPICommand("sw")
            .withPermission("ultrastopwatch.use")
            .withSubcommand(
                new CommandAPICommand("start")
                    .executesPlayer((player, args) -> {
                        timerManager.startManual(player);
                    })
            )
            .withSubcommand(
                new CommandAPICommand("stop")
                    .executesPlayer((player, args) -> {
                        timerManager.stop(player);
                    })
            )
            .withSubcommand(
                new CommandAPICommand("reset")
                    .executesPlayer((player, args) -> {
                        timerManager.reset(player);
                    })
            )
            .withSubcommand(
                new CommandAPICommand("check")
                    .executesPlayer((player, args) -> {
                        timerManager.check(player);
                    })
            )
            .register();
    }
}
