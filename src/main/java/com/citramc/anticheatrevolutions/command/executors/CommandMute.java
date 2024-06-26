/*
 * AntiCheatRevolutions for Bukkit and Spigot.
 * Copyright (c) 2012-2015 AntiCheat Team
 * Copyright (c) 2016-2022 Rammelkast
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
package com.citramc.anticheatrevolutions.command.executors;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.command.CommandBase;
import com.citramc.anticheatrevolutions.util.Permission;

public class CommandMute extends CommandBase {

	private static final String NAME = "AntiCheatRevolutions Mute";
	private static final String COMMAND = "mute";
	private static final String USAGE = "anticheat mute";
	private static final Permission PERMISSION = Permission.SYSTEM_MUTE;
	private static final String[] HELP = {
			GRAY + "Use: " + AQUA + "/anticheat mute" + GRAY + " to mute all notifications", };

	public CommandMute() {
		super(NAME, COMMAND, USAGE, HELP, PERMISSION);
	}

	@Override
	protected void execute(CommandSender cs, String[] args) {
		if (!(cs instanceof Player)) {
			cs.sendMessage(ChatColor.BOLD + "" + GOLD + "ACR " + GRAY + "This command is only for players");
			return;
		}
		UUID uuid = ((Player) cs).getUniqueId();
		if (AntiCheatRevolutions.MUTE_ENABLED_MODS.contains(uuid)) {
			cs.sendMessage(GOLD + "" + ChatColor.BOLD + "ACR " + GRAY + "Player alerts have been unmuted");
			AntiCheatRevolutions.MUTE_ENABLED_MODS.remove(uuid);
			return;
		} else {
			cs.sendMessage(GOLD + "" + ChatColor.BOLD + "ACR " + GRAY + "Player alerts have been muted");
			AntiCheatRevolutions.MUTE_ENABLED_MODS.add(uuid);
			return;
		}
	}
}
