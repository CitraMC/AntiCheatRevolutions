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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.config.Configuration;
import com.citramc.anticheatrevolutions.util.FileFormatter;
import com.citramc.anticheatrevolutions.util.Permission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingManager {

    private final Logger fileLogger;
    private FileHandler fileHandler;
    private final List<String> logs = new ArrayList<>();
    private final Configuration config;

    public LoggingManager(AntiCheatRevolutions plugin, Logger logger, Configuration config) {
        this.fileLogger = Logger.getLogger("com.citramc.anticheatrevolutions.AntiCheatRevolutions");
        this.config = config;
        initializeLogger(plugin, logger);
    }

    private void initializeLogger(AntiCheatRevolutions plugin, Logger logger) {
        try {
            File logDir = new File(plugin.getDataFolder(), "log");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            fileHandler = new FileHandler(new File(logDir, "anticheat.log").getPath(), true);
            fileHandler.setFormatter(new FileFormatter());
            fileLogger.addHandler(fileHandler);
            fileLogger.setUseParentHandlers(false);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to initialize logging file handler", ex);
        }
    }

    public void log(String message) {
        if (config.getConfig().logToConsole.getValue()) {
            logToConsole(message);
        }
        if (config.getConfig().logToFile.getValue()) {
            logToFile(message);
        }
        synchronized (logs) {
            logs.add(ChatColor.stripColor(message));
        }
    }

    public void debugLog(String message) {
        if (config.getConfig().debugMode.getValue()) {
            Bukkit.getConsoleSender()
                    .sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "ACR " + ChatColor.GRAY + message);
            synchronized (logs) {
                logs.add(ChatColor.stripColor(message));
            }
        }
    }

    public void logToConsole(String message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public void logToFile(String message) {
        fileLogger.info(ChatColor.stripColor(message));
    }

    public void logToPlayers(String message) {
        if (config.getConfig().disableBroadcast.getValue())
            return;
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (Permission.SYSTEM_NOTICE.get(player)) {
                player.sendMessage(message);
            }
        });
    }

    public List<String> getLastLogs() {
        synchronized (logs) {
            List<String> recentLogs = new ArrayList<>(logs);
            logs.clear();
            return recentLogs;
        }
    }

    public void closeHandler() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}
