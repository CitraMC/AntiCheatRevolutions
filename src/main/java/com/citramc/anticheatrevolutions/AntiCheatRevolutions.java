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
package com.citramc.anticheatrevolutions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.citramc.anticheatrevolutions.command.CommandHandler;
import com.citramc.anticheatrevolutions.config.Configuration;
import com.citramc.anticheatrevolutions.event.BlockListener;
import com.citramc.anticheatrevolutions.event.EntityListener;
import com.citramc.anticheatrevolutions.event.InventoryListener;
import com.citramc.anticheatrevolutions.event.PacketListener;
import com.citramc.anticheatrevolutions.event.PlayerListener;
import com.citramc.anticheatrevolutions.event.VehicleListener;
import com.citramc.anticheatrevolutions.manage.AntiCheatManager;
import com.citramc.anticheatrevolutions.util.UpdateManager;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.VersionLib;

import lombok.Getter;

public final class AntiCheatRevolutions extends JavaPlugin {

	public static final String PREFIX = ChatColor.GOLD + "" + ChatColor.BOLD + "ACR " + ChatColor.DARK_GRAY + "> "
			+ ChatColor.GRAY;
	public static final List<UUID> MUTE_ENABLED_MODS = new ArrayList<UUID>();

	@Getter
	private static AntiCheatRevolutions plugin;
	@Getter
	private static AntiCheatManager manager;
	@Getter
	private static ExecutorService executor;
	@Getter
	private static ProtocolManager protocolManager;
	@Getter
	private static UpdateManager updateManager;
	@Getter
	private static boolean floodgateEnabled;
	private static List<Listener> eventList = new ArrayList<Listener>();
	private static Configuration config;
	private static boolean verbose;

	private double tps = -1;

	@Override
	public void onLoad() {
		plugin = this;

		// Improved thread pool creation: Adaptively determine optimal size with a cap
		int coreCount = Runtime.getRuntime().availableProcessors();
		int poolSize = Math.min(coreCount, 4); // Limit max threads to 4 or less
		executor = Executors.newFixedThreadPool(poolSize);
		Bukkit.getConsoleSender().sendMessage(PREFIX + "Executor pool initialized with " + poolSize + " threads");

		// Check for ProtocolLib and disable plugin if missing
		if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
			String errorMsg = PREFIX + ChatColor.RED
					+ "ProtocolLib not found! AntiCheatRevolutions requires ProtocolLib to work, please download and install it.";
			Bukkit.getConsoleSender().sendMessage(errorMsg);
			Bukkit.getPluginManager().disablePlugin(this);
		} else {
			Bukkit.getConsoleSender().sendMessage(PREFIX + "ProtocolLib found. Plugin loading...");
		}
	}

	@Override
	public void onEnable() {
		manager = new AntiCheatManager(this, getLogger());

		// Setup all listeners efficiently in one go
		setupListeners();

		// Setup configurations, commands, enterprise features and restore levels
		setupConfig();
		setupCommands();
		setupEnterprise();
		restoreLevels();

		// Setup ProtocolLib hooks
		setupProtocol();

		// Server version compatibility check
		checkServerVersion();

		// Floodgate support check
		checkFloodgateSupport();

		// Initialize and start the update manager and TPS checker
		initializeUpdateManager();
		startTpsChecker();

		// Start metrics after a delay
		scheduleMetrics();

		verboseLog("Plugin enabled successfully.");
	}

	private void setupListeners() {
		eventList.addAll(Arrays.asList(
				new PlayerListener(), new BlockListener(), new EntityListener(),
				new VehicleListener(), new InventoryListener()));
		eventList.forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
		verboseLog("All event listeners registered.");
	}

	private void checkServerVersion() {
		String versionMsg = PREFIX + "Running Minecraft version " + VersionLib.getVersion() + " " +
				(VersionLib.isSupported() ? ChatColor.GREEN + "(supported)" : ChatColor.RED + "(NOT SUPPORTED!)");
		Bukkit.getConsoleSender().sendMessage(versionMsg);
	}

	private void checkFloodgateSupport() {
		if (Bukkit.getPluginManager().getPlugin("Floodgate") != null) {
			floodgateEnabled = true;
			Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.WHITE + "Floodgate support enabled");
		}
	}

	private void initializeUpdateManager() {
		updateManager = new UpdateManager();
	}

	private void startTpsChecker() {
		new BukkitRunnable() {
			long second;
			long currentSecond;
			int ticks;

			@Override
			public void run() {
				second = (System.currentTimeMillis() / 1000L);
				if (currentSecond == second) {
					ticks++;
				} else {
					currentSecond = second;
					tps = (tps == 0.0 ? ticks : (tps + ticks) / 2.0);
					ticks = 1;
					if (ticks % 864000 == 0) {
						updateManager.update();
					}
				}
			}
		}.runTaskTimer(this, 40L, 1L);
	}

	private void scheduleMetrics() {
		getServer().getScheduler().runTaskLater(this, () -> {
			try {
				Metrics metrics = new Metrics(this, 21647);
				metrics.addCustomChart(new SingleLineChart("cheaters_kicked", new Callable<Integer>() {
					@Override
					public Integer call() throws Exception {
						final int kicked = playersKicked;
						// Reset so we don't keep sending the same value
						playersKicked = 0;
						return kicked;
					}
				}));
				metrics.addCustomChart(new SimplePie("protocollib_version", new Callable<String>() {
					@Override
					public String call() throws Exception {
						return Bukkit.getPluginManager().getPlugin("ProtocolLib").getDescription().getVersion();
					}
				}));
				metrics.addCustomChart(new SimplePie("nms_version", new Callable<String>() {
					@Override
					public String call() throws Exception {
						return VersionLib.getVersion();
					}
				}));
				metrics.addCustomChart(new SimplePie("floodgate_enabled", new Callable<String>() {
					@Override
					public String call() throws Exception {
						return floodgateEnabled ? "Yes" : "No";
					}
				}));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 90L);
	}

	@Override
	public void onDisable() {
		// Cancel all scheduled tasks and running threads
		cancelScheduledTasks();

		// Shutdown executor service gracefully
		shutdownExecutorService();

		// Save configuration and user levels
		saveConfiguration();

		// Clean up and release resources
		performCleanup();

		// Nullify static references to facilitate garbage collection
		clearStaticReferences();

		verboseLog("Plugin disabled successfully.");
	}

	private void cancelScheduledTasks() {
		getServer().getScheduler().cancelTasks(this);
		verboseLog("All scheduled tasks have been cancelled.");
	}

	private void shutdownExecutorService() {
		if (executor != null) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					executor.shutdownNow();
					if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
						System.err.println("Executor did not terminate");
					}
				}
			} catch (InterruptedException ie) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
			verboseLog("Executor service shutdown completed.");
		}
	}

	private void saveConfiguration() {
		if (config != null) {
			config.getLevels().saveLevelsFromUsers(manager.getUserManager().getUsers());
			verboseLog("User levels and configurations have been saved.");
		}
	}

	private void performCleanup() {
		AntiCheatManager.close();
		verboseLog("Cleaned up AntiCheatManager resources.");
	}

	private void clearStaticReferences() {
		plugin = null;
		manager = null;
		executor = null;
		protocolManager = null;
		updateManager = null;
		eventList = null;
		config = null;
		verboseLog("Static references cleared.");
	}

	private void setupProtocol() {
		protocolManager = ProtocolLibrary.getProtocolManager();

		if (protocolManager == null) {
			verboseLog(
					"Failed to obtain ProtocolManager from ProtocolLib. Check if ProtocolLib is properly installed.");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		try {
			registerPacketListeners();
			verboseLog("ProtocolLib packet listeners registered successfully.");
		} catch (Exception e) {
			Bukkit.getLogger()
					.severe("An error occurred while setting up ProtocolLib packet listeners: " + e.getMessage());
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	private void registerPacketListeners() {
		// Example of packet listener registration
		PacketListener packetListener = new PacketListener(this);
		protocolManager.addPacketListener(packetListener);
	}

	private void setupCommands() {
		try {
			registerCommands();
			verboseLog("Commands registered successfully.");
		} catch (Exception e) {
			getLogger().severe("Failed to register commands: " + e.getMessage());
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	private void registerCommands() {
		getCommand("anticheat").setExecutor(new CommandHandler());
	}

	private void setupConfig() {
		try {
			config = manager.getConfiguration();
			if (config == null) {
				throw new IllegalStateException("Configuration could not be loaded.");
			}
			loadConfigurations();
			verboseLog("Configuration loaded successfully.");
		} catch (Exception e) {
			getLogger().severe("Failed to load configuration: " + e.getMessage());
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	private void loadConfigurations() {
		config.load();
	}

	private void setupEnterprise() {
		if (config == null) {
			verboseLog("Configuration not initialized. Cannot setup enterprise features.");
			return;
		}

		try {
			if (config.getConfig().enterprise.getValue()) {
				initializeEnterpriseFeatures();
				verboseLog("Enterprise features initialized successfully.");
			} else {
				verboseLog("Enterprise features are disabled.");
			}
		} catch (Exception e) {
			getLogger().severe("Failed to initialize enterprise features: " + e.getMessage());
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	private void initializeEnterpriseFeatures() {
		if (config.getEnterprise().loggingEnabled.getValue()) {
			setupEnterpriseLogging();
		}
	}

	private void setupEnterpriseLogging() {
		getLogger().info("Enterprise logging configured.");
	}

	private void restoreLevels() {
		if (config == null || config.getLevels() == null) {
			verboseLog("Configuration or levels are not set up properly. Cannot restore levels.");
			return;
		}

		Bukkit.getOnlinePlayers().forEach(player -> {
			UUID uuid = player.getUniqueId();
			restorePlayerLevel(uuid);
		});

		verboseLog("All player levels restored successfully.");
	}

	private void restorePlayerLevel(UUID uuid) {
		try {
			User user = manager.getUserManager().getUser(uuid);
			if (user == null) {
				user = new User(uuid);
				manager.getUserManager().addUser(user);
			}
			user.setIsWaitingOnLevelSync(true);
			config.getLevels().loadLevelToUser(user);
			verboseLog("Levels restored for " + uuid);
		} catch (Exception e) {
			getLogger().severe("Failed to restore levels for user: " + uuid + "; Error: " + e.getMessage());
		}
	}

	public static String getVersion() {
		return manager.getPlugin().getDescription().getVersion();
	}

	public static void debugLog(final String message) {
		if (manager == null || manager.getConfiguration() == null) {
			System.err.println("Debug logging attempted before manager or configuration was initialized.");
			return;
		}

		// Check if debug mode is enabled before proceeding with constructing and
		// logging the message
		if (manager.getConfiguration().getConfig().debugMode.getValue()) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				// Log the debug message to the console with a standardized prefix
				manager.debugLog("[DEBUG] " + message);
			});
		}
	}

	public void verboseLog(final String string) {
		if (verbose) {
			getLogger().info(string);
		}
	}

	public void setVerbose(boolean b) {
		verbose = b;
	}

	/**
	 * Amount of players kicked since start
	 */
	private int playersKicked = 0;

	public void onPlayerKicked() {
		this.playersKicked++;
	}

	public static void sendToMainThread(final Runnable runnable) {
		Bukkit.getScheduler().runTask(AntiCheatRevolutions.getPlugin(), runnable);
	}

	public void sendToStaff(final String message) {
		// Stream API to filter and send messages to appropriate staff
		Bukkit.getOnlinePlayers().stream()
				.filter(player -> player.hasPermission("anticheat.system.alert")
						&& !MUTE_ENABLED_MODS.contains(player.getUniqueId()))
				.forEach(player -> player.sendMessage(message));

		verboseLog("Sent staff alert message: " + message);
	}

	public double getTPS() {
		return Math.min(Math.max(this.tps, 0.0D), 20.0D);
	}

	public static void setManager(AntiCheatManager testManager) {
		manager = testManager;
	}

}
