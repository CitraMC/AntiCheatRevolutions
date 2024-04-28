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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.api.event.PlayerPunishEvent;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.Configuration;
import com.citramc.anticheatrevolutions.util.Group;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;

public final class UserManager {
	private Map<UUID, User> users = new ConcurrentHashMap<>();
	private final AntiCheatManager manager;
	private final Configuration config;

	/**
	 * Initialize the user manager
	 *
	 * @param manager The AntiCheat Manager
	 */
	public UserManager(final AntiCheatManager manager) {
		this.manager = manager;
		this.config = manager.getConfiguration();
	}

	/**
	 * Get a user with the given UUID
	 *
	 * @param uuid UUID
	 * @return User with UUID
	 */
	public User getUser(final UUID uuid) {
		return users.computeIfAbsent(uuid, User::new);
	}

	/**
	 * Get all users
	 *
	 * @return List of users
	 */
	@SuppressWarnings("unchecked")
	public List<User> getUsers() {
		return (List<User>) users;
	}

	/**
	 * Add a user to the list
	 *
	 * @param user User to add
	 */
	public void addUser(final User user) {
		users.putIfAbsent(user.getUUID(), user);

	}

	/**
	 * Remove a user from the list
	 *
	 * @param user User to remove
	 */
	public void removeUser(final User user) {
		users.remove(user.getUUID());
	}

	/**
	 * Save a user's level
	 *
	 * @param user User to save
	 */
	public void saveLevel(final User user) {
		config.getLevels().saveLevelFromUser(user);
	}

	/**
	 * Get users in group
	 *
	 * @param group Group to find users of
	 */
	@SuppressWarnings("unchecked")
	public List<User> getUsersInGroup(final Group group) {
		return ((List<User>) this.users).parallelStream().filter(user -> user.getGroup() == group)
				.collect(Collectors.toList());
	}

	/**
	 * Get a user's level, or 0 if the player isn't found
	 *
	 * @param uuid UUID of the player
	 * @return player level
	 */
	public int safeGetLevel(final UUID uuid) {
		final User user = getUser(uuid);
		if (user == null) {
			return 0;
		} else {
			return user.getLevel();
		}
	}

	/**
	 * Set a user's level
	 *
	 * @param uuid  UUID of the player
	 * @param level Group to set
	 */
	public void safeSetLevel(final UUID uuid, final int level) {
		final User user = getUser(uuid);
		if (user != null) {
			user.setLevel(level);
		}
	}

	/**
	 * Reset a user
	 *
	 * @param uuid UUID of the user
	 */
	public void safeReset(final UUID uuid) {
		final User user = getUser(uuid);
		if (user != null) {
			user.resetLevel();
		}
	}

	/**
	 * Fire an alert
	 *
	 * @param user  The user to alert
	 * @param group The user's group
	 * @param type  The CheckType that triggered the alert
	 */
	public void alert(final User user, final Group group, final CheckType type) {
		execute(user, group.getActions(), type);
	}

	/**
	 * Execute configuration actions for an alert
	 *
	 * @param user    The user
	 * @param actions The list of actions to execute
	 * @param type    The CheckType that triggered the alert
	 */
	public void execute(final User user, final List<String> actions, final CheckType type) {
		execute(user, actions, type, config.getLang().KICK_REASON(), config.getLang().WARNING(),
				config.getLang().BAN_REASON());
	}

	/**
	 * Execute configuration actions for an alert
	 *
	 * @param user       The user
	 * @param actions    The list of actions to execute
	 * @param type       The CheckType that triggered the alert
	 * @param kickReason The config's kick reason
	 * @param warning    The config's warning format
	 * @param banReason  The config's ban reason
	 */
	public void execute(final User user, final List<String> actions, final CheckType type, final String kickReason,
			final List<String> warning, final String banReason) {
		Player player = user.getPlayer();
		if (player == null) {
			return;
		}

		Bukkit.getScheduler().runTask(AntiCheatRevolutions.getPlugin(), () -> {
			PlayerPunishEvent event = new PlayerPunishEvent(user, actions);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}

			processActions(user, player, actions, type, kickReason, warning, banReason);
		});
	}

	private void processActions(User user, Player player, List<String> actions, CheckType type, String kickReason,
			List<String> warning, String banReason) {
		actions.forEach(action -> {
			action = action.replace("%player%", player.getName())
					.replace("%world%", player.getWorld().getName())
					.replace("%check%", type.name());

			switch (action.toUpperCase().split("\\[")[0]) { // Handles "COMMAND[something]"
				case "COMMAND":
					String[] commands = Utilities.getCommands(action); // Assuming it returns String[]
					Arrays.stream(commands).forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
					break;
				case "KICK":
					player.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickReason));
					broadcastAction("KICK_BROADCAST", player.getName(), type);
					break;
				case "WARN":
					warning.forEach(msg -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg)));
					break;
				case "BAN":
					Bukkit.getBanList(Type.NAME).addBan(player.getName(),
							ChatColor.translateAlternateColorCodes('&', banReason), null, "AntiCheat System");
					player.kickPlayer(ChatColor.translateAlternateColorCodes('&', banReason));
					broadcastAction("BAN_BROADCAST", player.getName(), type);
					break;
				case "RESET":
					user.resetLevel(); // Make sure User class has a resetLevel() method
					break;
				default:
					manager.getLoggingManager().log("Unhandled action: " + action);
					break;
			}
		});
	}

	private void broadcastAction(String actionKey, String playerName, CheckType type) {
		String messagePattern;
		switch (actionKey) {
			case "BAN_BROADCAST":
				messagePattern = manager.getConfiguration().getLang().BAN_BROADCAST();
				break;
			case "KICK_BROADCAST":
				messagePattern = manager.getConfiguration().getLang().KICK_BROADCAST();
				break;
			default:
				manager.getLoggingManager().log("Unhandled action key: " + actionKey);
				return;
		}

		if (messagePattern == null) {
			manager.getLoggingManager().log("Message pattern not found for key: " + actionKey);
			return;
		}

		String message = ChatColor.translateAlternateColorCodes('&',
				messagePattern.replace("%player%", playerName) + " (" + type.getName() + ")");
		if (!message.isEmpty()) {
			manager.getLoggingManager().log(message);
			Bukkit.broadcast(message, "anticheat.broadcast");
		}
	}

}
