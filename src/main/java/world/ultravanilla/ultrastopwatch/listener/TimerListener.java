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
package world.ultravanilla.ultrastopwatch.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import world.ultravanilla.ultrastopwatch.model.Track;
import world.ultravanilla.ultrastopwatch.storage.DataStore;
import world.ultravanilla.ultrastopwatch.timer.PlayerTimer;
import world.ultravanilla.ultrastopwatch.timer.TimerManager;

import java.util.EnumSet;
import java.util.Set;

public class TimerListener implements Listener {

    // TODO: Move this list to conf to make it future-proof
    private static final Set<Material> PRESSURE_PLATES = EnumSet.of(
            Material.OAK_PRESSURE_PLATE,
            Material.SPRUCE_PRESSURE_PLATE,
            Material.BIRCH_PRESSURE_PLATE,
            Material.JUNGLE_PRESSURE_PLATE,
            Material.ACACIA_PRESSURE_PLATE,
            Material.DARK_OAK_PRESSURE_PLATE,
            Material.CHERRY_PRESSURE_PLATE,
            Material.BAMBOO_PRESSURE_PLATE,
            Material.MANGROVE_PRESSURE_PLATE,
            Material.CRIMSON_PRESSURE_PLATE,
            Material.WARPED_PRESSURE_PLATE,
            Material.STONE_PRESSURE_PLATE,
            Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
            Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Material.HEAVY_WEIGHTED_PRESSURE_PLATE
    );

    private final TimerManager timerManager;
    private final DataStore dataStore;

    public TimerListener(TimerManager timerManager, DataStore dataStore) {
        this.timerManager = timerManager;
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Hot path check
        if (!timerManager.isPending(event.getPlayer().getUniqueId())) return;
        Location to = event.getTo();
        if (!hasPositionChanged(event.getFrom(), to)) return;

        PlayerTimer timer = timerManager.getTimer(event.getPlayer().getUniqueId());
        if (timer != null && timer.type() == PlayerTimer.TimerType.TRACK) {
            Track track = dataStore.getTrack(timer.trackName());
            if (track != null && track.isFullyConfigured()) {
                // Check both current block and block below to account for jumping or floating tripwires
                Block toBlock = to.getBlock();
                Block fromBlock = event.getFrom().getBlock();

                boolean entering = track.isStartBlock(toBlock) || track.isStartBlock(toBlock.getRelative(BlockFace.DOWN));
                boolean leaving = track.isStartBlock(fromBlock) || track.isStartBlock(fromBlock.getRelative(BlockFace.DOWN));

                if (!entering && !leaving) return;
            }
        }

        timerManager.triggerStart(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        // Check block type before checking passengers
        Material type = event.getBlock().getType();
        if (!PRESSURE_PLATES.contains(type) && type != Material.TRIPWIRE) return;

        for (org.bukkit.entity.Entity passenger : event.getEntity().getPassengers()) {
            if (passenger instanceof Player player) {
                if (!timerManager.isRunning(player.getUniqueId())) continue;
                handlePhysicalInteract(player, event.getBlock());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        if (!PRESSURE_PLATES.contains(type) && type != Material.TRIPWIRE) return;

        if (!timerManager.isRunning(event.getPlayer().getUniqueId())) return;

        handlePhysicalInteract(event.getPlayer(), block);
    }

    private void handlePhysicalInteract(Player player, Block block) {
        Material blockType = block.getType();
        boolean isPlate = PRESSURE_PLATES.contains(blockType);
        PlayerTimer timer = timerManager.getTimer(player.getUniqueId());
        if (timer == null) return;

        if (timer.type() == PlayerTimer.TimerType.CASUAL) {
            if (isPlate) {
                timerManager.stop(player);
            }
            return;
        }

        if (timer.type() == PlayerTimer.TimerType.TRACK) {
            Track track = dataStore.getTrack(timer.trackName());
            if (track == null) return;

            // Trigger Delay check (needed for tracks with startline on or near endline)
            if (track.getTriggerDelay() > 0) {
                long startNanos = timerManager.getStartTime(player.getUniqueId());
                long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
                if (elapsed < track.getTriggerDelay()) return;
            }

            // Check Trigger Type
            if (track.getTriggerType() == Track.TriggerType.PRESSURE_PLATE && !isPlate) return;
            if (track.getTriggerType() == Track.TriggerType.TRIPWIRE && blockType != Material.TRIPWIRE) return;

            // part of the finish line?
            boolean isEnd = track.isEndBlock(block);
            if (!isEnd && blockType == Material.TRIPWIRE) {
                isEnd = track.isEndBlock(block.getRelative(BlockFace.DOWN));
            }
            if (!isEnd) {
                return;
            }

            // Check for laps
            if (track.getLaps() > 1) {
                if (!timerManager.lap(player, track.getLaps())) {
                    return;
                }
            }

            timerManager.stop(player, true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        dataStore.loadPlayerRecords(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        timerManager.removeTimer(event.getPlayer().getUniqueId());
        dataStore.unloadPlayerRecords(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        timerManager.removeTimer(event.getEntity().getUniqueId());
    }

    private boolean hasPositionChanged(Location from, Location to) {
        if (to == null) return false;
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }
}
