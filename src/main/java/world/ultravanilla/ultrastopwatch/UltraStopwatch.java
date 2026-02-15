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
package world.ultravanilla.ultrastopwatch;

import org.bukkit.plugin.java.JavaPlugin;
import world.ultravanilla.ultrastopwatch.command.StartRunCommand;
import world.ultravanilla.ultrastopwatch.command.TrackCommand;
import world.ultravanilla.ultrastopwatch.command.StopwatchCommand;
import world.ultravanilla.ultrastopwatch.listener.TimerListener;
import world.ultravanilla.ultrastopwatch.storage.DataStore;
import world.ultravanilla.ultrastopwatch.timer.TimerManager;

import java.util.List;

public class UltraStopwatch extends JavaPlugin {

    private DataStore dataStore;
    private TimerManager timerManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        // inits
        dataStore = new DataStore(getDataFolder().toPath().resolve("data"), getLogger(), this);
        if (!dataStore.load()) {
            getLogger().severe("Failed to load data! Disabling plugin to prevent data loss.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        timerManager = new TimerManager(this, dataStore);

        // Commands
        new StopwatchCommand(timerManager).register();
        new StartRunCommand(timerManager).register();

        int leaderboardSize = getConfig().getInt("leaderboard-size", 10);
        List<Integer> pointsTable = getConfig().getIntegerList("points-table");
        if (pointsTable.isEmpty()) {
            pointsTable = List.of(10, 8, 6, 5, 4, 3, 2, 1);
        }
        new TrackCommand(this, dataStore, timerManager, leaderboardSize, pointsTable).register();

        // Event listeners
        getServer().getPluginManager().registerEvents(new TimerListener(timerManager, dataStore), this);

        // we did it!! :D
        getLogger().info("UltraStopwatch has been enabled!");
    }

    @Override
    public void onDisable() {
        if (timerManager != null) {
            timerManager.shutdown();
        }

        if (dataStore != null) {
            dataStore.saveAll();
        }

        getLogger().info("UltraStopwatch has been disabled!");
    }
}
