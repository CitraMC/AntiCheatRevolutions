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

package com.citramc.anticheatrevolutions.util.enterprise;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {

    private static final String EVENTS_TABLE = "logs";

    private String sqlLogEvent;
    private String sqlCleanEvents;
    private String sqlCreateEvents;

    public enum DatabaseType {
        MySQL,
    }

    private DatabaseType type;
    private String hostname;
    private int port;
    private String username;
    private String password;
    private String prefix;
    private String schema;
    private String serverName;

    private long logInterval;
    private long logLife;

    private boolean syncLevels;
    private long syncInterval;

    private Connection connection;

    private PreparedStatement eventBatch;

    private BukkitTask eventTask;
    private BukkitTask syncTask;

    public Database(DatabaseType type, String hostname, int port, String username, String password, String prefix,
            String schema, String serverName, String logInterval, String logLife, boolean syncLevels,
            String syncInterval) {
        this.type = type;
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.prefix = prefix;
        this.schema = schema;
        this.serverName = serverName;

        this.logInterval = Utilities.lifeToSeconds(logInterval);
        this.logLife = Utilities.lifeToSeconds(logLife);

        this.syncLevels = syncLevels;
        this.syncInterval = Utilities.lifeToSeconds(syncInterval);

        initializeSQL();
        connect();
    }

    private void initializeSQL() {
        sqlLogEvent = "INSERT INTO " + prefix + EVENTS_TABLE + " (server, user, check_type) VALUES (?, ?, ?)";
        sqlCleanEvents = "DELETE FROM " + prefix + EVENTS_TABLE
                + " WHERE time < (CURRENT_TIMESTAMP - INTERVAL ? SECOND)";
        sqlCreateEvents = "CREATE TABLE IF NOT EXISTS " + prefix + EVENTS_TABLE + "(" +
                "id INT NOT NULL AUTO_INCREMENT," +
                "server VARCHAR(45) NOT NULL," +
                "time TIMESTAMP NOT NULL DEFAULT NOW()," +
                "user VARCHAR(45) NOT NULL," +
                "check_type VARCHAR(45) NOT NULL," +
                "PRIMARY KEY (id));";
    }

    public DatabaseType getType() {
        return type;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSchema() {
        return schema;
    }

    public void connect() {
        String url = "jdbc:" + type.toString().toLowerCase() + "://" + hostname + ":" + port + "/" + schema;

        try {
            connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(false);

            connection.prepareStatement(sqlCreateEvents).executeUpdate();
            eventBatch = connection.prepareStatement(sqlLogEvent);

            scheduleTasks();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Failed to connect to the database: " + e.getMessage());
        }
    }

    private void scheduleTasks() {
        if (logInterval > 0) {
            eventTask = Bukkit.getScheduler().runTaskTimerAsynchronously(AntiCheatRevolutions.getPlugin(),
                    this::flushEvents, logInterval * 20, logInterval * 20);
        }

        if (syncLevels && syncInterval > 0) {
            syncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(AntiCheatRevolutions.getPlugin(),
                    this::syncUsers, syncInterval * 20, syncInterval * 20);
        }
    }

    public void shutdown() {
        if (eventTask != null)
            eventTask.cancel();
        if (syncTask != null)
            syncTask.cancel();

        try {
            flushEvents();
            if (eventBatch != null)
                eventBatch.close();
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error shutting down database connection: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void logEvent(User user, CheckType checkType) {
        try {
            eventBatch.setString(1, serverName);
            eventBatch.setString(2, user.getUUID().toString());
            eventBatch.setString(3, checkType.toString());

            eventBatch.addBatch();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Failed to log event: " + e.getMessage());
        }
    }

    public void flushEvents() {
        try {
            eventBatch.executeBatch();
            connection.commit();
            eventBatch.clearBatch();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error flushing events: " + e.getMessage());
        }
    }

    public void cleanEvents() {
        if (logLife != 0) {
            Bukkit.getScheduler().runTaskAsynchronously(AntiCheatRevolutions.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    try {
                        PreparedStatement statement = connection.prepareStatement(sqlCleanEvents);
                        statement.setLong(1, logLife);

                        statement.executeUpdate();

                        connection.commit();
                        AntiCheatRevolutions.getPlugin()
                                .verboseLog("Cleaned " + statement.getUpdateCount() + " old events from the database");
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void syncUsers() {
        for (User user : AntiCheatRevolutions.getManager().getUserManager().getUsers()) {
            AntiCheatRevolutions.getManager().getConfiguration().getLevels().updateLevelToUser(user);
        }
    }
}
