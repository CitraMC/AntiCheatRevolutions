/*
 * AntiCheatRevolutions for Bukkit and Spigot.
 * Copyright (c) 2012-2015 AntiCheat Team
 * Copyright (c) 2016-2022 Rammelkast
 * Copyright (c) 2024 CitraMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.citramc.anticheatrevolutions.manage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.Configuration;
import com.citramc.anticheatrevolutions.util.VersionLib;

/**
 * The manager that AntiCheatRevolutions will check with to see if it should
 * watch
 * certain checks and certain players.
 */
public final class CheckManager {

    private final AntiCheatManager manager;
    private final Configuration config;

    private static final Set<CheckType> checkIgnoreList = new HashSet<>();
    private static final Map<UUID, Set<CheckType>> exemptList = new HashMap<>();

    public CheckManager(final AntiCheatManager manager) {
        this.manager = manager;
        this.config = manager.getConfiguration();
        loadCheckIgnoreList(this.config);
    }

    public void loadCheckIgnoreList(final Configuration configuration) {
        checkIgnoreList.clear();
        for (final CheckType type : CheckType.values()) {
            if (!config.getChecks().isEnabled(type)) {
                checkIgnoreList.add(type);
            }
        }

        if (!checkIgnoreList.isEmpty()) {
            manager.getLoggingManager()
                    .log(ChatColor.GOLD + "" + ChatColor.BOLD + "ACR " + ChatColor.DARK_GRAY + "> " + ChatColor.GRAY
                            + checkIgnoreList.size() + " check(s) have been disabled");
        }
    }

    /**
     * Turn a check on
     *
     * @param type The CheckType to enable
     */
    public void activateCheck(final CheckType type, final String className) {
        if (!isActive(type)) {
            checkIgnoreList.remove(type);
            manager.getLoggingManager().log("Activated check: " + type);
        }
    }

    /**
     * Turn a check off
     *
     * @param type The CheckType to disable
     */
    public void deactivateCheck(final CheckType type, final String className) {
        if (isActive(type)) {
            checkIgnoreList.add(type);
            manager.getLoggingManager().log("Deactivated check: " + type);
        }
    }

    /**
     * Determine whether a check is enabled
     *
     * @param type The CheckType to check
     * @return true if the check is active
     */
    public boolean isActive(final CheckType type) {
        return !checkIgnoreList.contains(type);
    }

    /**
     * Exempt a player from a check
     *
     * @param player The player
     * @param type   The check
     */
    public void exemptPlayer(final Player player, final CheckType type, final String className) {
        exemptList.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(type);
        manager.getLoggingManager().log(player.getName() + " was exempted from the " + type + " check.");
    }

    /**
     * Unexempt a player from a check
     *
     * @param player The player
     * @param type   The check
     */
    public void unexemptPlayer(final Player player, final CheckType type, final String className) {
        Set<CheckType> types = exemptList.getOrDefault(player.getUniqueId(), new HashSet<>());
        if (types.remove(type)) {
            manager.getLoggingManager().log(player.getName() + " was unexempted from the " + type + " check.");
        }
    }

    /**
     * Determine whether a player is exempt from a check
     *
     * @param player The player
     * @param type   The check
     */
    public boolean isExempt(final Player player, final CheckType type) {
        if (AntiCheatRevolutions.isFloodgateEnabled()
                && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            return true;
        }
        return exemptList.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(type);
    }

    /**
     * Determine whether a player is exempt from all checks from op status
     *
     * @param player The player
     */
    public boolean isOpExempt(final Player player) {
        return (this.manager.getConfiguration().getConfig().exemptOp.getValue() && player.isOp());
    }

    /**
     * Determine whether a player should be checked in their world
     *
     * @param player The player
     * @return true if the player's world is enabled
     */
    public boolean checkInWorld(final Player player) {
        return !config.getConfig().disabledWorlds.getValue().contains(player.getWorld().getName());
    }

    /**
     * Run a quick version of the "willCheck" method, using the other
     * non-check-specific methods beforehand
     *
     * @param player The player to check
     * @param type   The check being run
     * @return true if the check should run
     */
    public boolean willCheckQuick(final Player player, final CheckType type) {
        return isActive(type)
                && !isExempt(player, type)
                && !type.checkPermission(player);
    }

    /**
     * Determine whether a check should run on a player
     *
     * @param player The player to check
     * @param type   The check being run
     * @return true if the check should run
     */
    public boolean willCheck(final Player player, final CheckType type) {
        return isActive(type) &&
                checkInWorld(player) &&
                !isExempt(player, type) &&
                !type.checkPermission(player) &&
                !(config.getConfig().exemptOp.getValue() && player.isOp()) &&
                !VersionLib.isFlying(player);
    }

    /**
     * Determine whether a player is actually online; not an NPC
     *
     * @param player Player to check
     * @return true if the player is a real person
     */
    public boolean isOnline(final Player player) {
        // Check if the player is on the user list, e.g. is not an NPC
        for (final Player iterated : Bukkit.getOnlinePlayers()) {
            if (iterated.getUniqueId().equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }
}
