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
 * GNU General Public License for more details.-
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.citramc.anticheatrevolutions.manage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.plugin.RegisteredListener;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.Backend;
import com.citramc.anticheatrevolutions.config.Configuration;

/**
 * The internal hub for all managers.
 */
public final class AntiCheatManager {

    private static AntiCheatRevolutions plugin;
    private static Configuration configuration;
    private static UserManager userManager;
    private static CheckManager checkManager;
    private static LoggingManager loggingManager;
    private static Backend backend;
    private static Map<String, RegisteredListener[]> eventchains = new ConcurrentHashMap<>();
    private static Map<String, Long> eventcache = new ConcurrentHashMap<>();

    public AntiCheatManager(final AntiCheatRevolutions instance, final Logger logger) {
        plugin = instance;
        configuration = new Configuration(plugin, this);
        loggingManager = new LoggingManager(plugin, logger, configuration);
        userManager = new UserManager(this);
        checkManager = new CheckManager(this);
        backend = new Backend(this);
    }

    public void log(String message) {
        loggingManager.log(message);
    }

    public void debugLog(String message) {
        loggingManager.debugLog(message);
    }

    public void playerLog(String message) {
        loggingManager.logToPlayers(message);
    }

    public void addEvent(String eventName, RegisteredListener[] listeners) {
        if (!configuration.getConfig().eventChains.getValue())
            return;

        long currentTime = System.currentTimeMillis();
        eventcache.compute(eventName, (key, lastUpdated) -> {
            if (lastUpdated == null || currentTime - lastUpdated > 30000) {
                eventchains.put(eventName, listeners);
                return currentTime;
            }
            return lastUpdated;
        });
    }

    public String getEventChainReport() {
        if (!configuration.getConfig().eventChains.getValue()) {
            return "Event Chains is disabled by the configuration.\n";
        }

        if (eventchains.isEmpty()) {
            return "No event chains found.\n";
        }

        StringBuilder reportBuilder = new StringBuilder();
        eventchains.forEach((eventName, listeners) -> {
            reportBuilder.append(eventName).append(":\n");
            for (RegisteredListener listener : listeners) {
                String pluginName = listener.getPlugin().getName();
                if ("AntiCheat".equals(pluginName)) {
                    pluginName = "self";
                }
                reportBuilder.append("- ").append(pluginName).append("\n");
            }
        });

        return reportBuilder.toString();
    }

    public AntiCheatRevolutions getPlugin() {
        return plugin;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public Backend getBackend() {
        return backend;
    }

    public LoggingManager getLoggingManager() {
        return loggingManager;
    }

    public static void close() {
        loggingManager.closeHandler();
        if (configuration.getConfig().enterprise.getValue()) {
            configuration.getEnterprise().database.shutdown();
        }

        eventchains.clear();
        eventcache.clear();

        plugin = null;
        configuration = null;
        loggingManager = null;
        userManager = null;
        checkManager = null;
        backend = null;
        eventchains = null;
        eventcache = null;
    }
}
