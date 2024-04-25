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

package com.citramc.anticheatrevolutions.util;

import org.bukkit.ChatColor;
import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import java.util.Arrays;
import java.util.List;

public class Group {

	private String name;
	private int level;
	private ChatColor color = ChatColor.RED;
	private List<String> actions;

	public Group(String name, int level, String color, List<String> actions) {
		this.name = name;
		this.level = level;
		this.actions = actions;

		try {
			this.color = ChatColor.valueOf(color.toUpperCase());
		} catch (IllegalArgumentException e) {
			AntiCheatRevolutions.getPlugin().getLogger().warning("Event '" + name
					+ "' was initialized with an invalid color '" + color + "'. Using default color RED.");
		}
	}

	public String getName() {
		return name;
	}

	public int getLevel() {
		return level;
	}

	public ChatColor getColor() {
		return color;
	}

	public List<String> getActions() {
		return actions;
	}

	public static Group load(String string) {
		String[] parts = string.split(" : ");
		if (parts.length == 4) {
			String name = parts[0];
			int level = Integer.parseInt(parts[1]);
			String color = parts[2];
			List<String> actions = Arrays.asList(parts[3].split(","));
			return new Group(name, level, color, actions);
		} else {
			AntiCheatRevolutions.getPlugin().getLogger().warning("Failed to initialize Group from string: '" + string
					+ "'. Expected format: 'name : level : color : actions'");
			return null;
		}
	}

	@Override
	public String toString() {
		return name + " : " + level + " : " + color.name() + " : " + String.join(", ", actions);
	}
}
