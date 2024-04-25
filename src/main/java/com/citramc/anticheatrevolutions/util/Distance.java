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

import org.bukkit.Location;

/**
 * Represents the distance between two points in a Minecraft world.
 */
public class Distance {
	private final double fromY;
	private final double toY;

	private final double xDifference;
	private final double yDifference;
	private final double zDifference;

	private final Location from;
	private final Location to;

	/**
	 * Constructs a new Distance object calculating differences between two
	 * locations.
	 *
	 * @param from The starting location.
	 * @param to   The ending location.
	 */
	public Distance(Location from, Location to) {
		if (from == null || to == null) {
			throw new IllegalArgumentException("Locations cannot be null");
		}

		this.fromY = from.getY();
		this.toY = to.getY();

		this.xDifference = Math.abs(to.getX() - from.getX());
		this.yDifference = Math.abs(to.getY() - from.getY());
		this.zDifference = Math.abs(to.getZ() - from.getZ());

		this.from = from;
		this.to = to;
	}

	/**
	 * Default constructor for Distance, initializing locations to null and
	 * differences to zero.
	 */
	public Distance() {
		fromY = 0;
		toY = 0;

		xDifference = 0;
		yDifference = 0;
		zDifference = 0;

		this.from = null;
		this.to = null;
	}

	/**
	 * Gets the Y-coordinate of the from location.
	 *
	 * @return The Y-coordinate of the from location.
	 */
	public double fromY() {
		return fromY;
	}

	/**
	 * Gets the Y-coordinate of the to location.
	 *
	 * @return The Y-coordinate of the to location.
	 */
	public double toY() {
		return toY;
	}

	/**
	 * Gets the difference in the X-coordinates of the two locations.
	 *
	 * @return The X-coordinate difference.
	 */
	public double getXDifference() {
		return xDifference;
	}

	/**
	 * Gets the difference in the Z-coordinates of the two locations.
	 *
	 * @return The Z-coordinate difference.
	 */
	public double getZDifference() {
		return zDifference;
	}

	/**
	 * Gets the difference in the Y-coordinates of the two locations.
	 *
	 * @return The Y-coordinate difference.
	 */
	public double getYDifference() {
		return yDifference;
	}

	/**
	 * Gets the initial location from which distance is calculated.
	 *
	 * @return The initial location.
	 */
	public Location getFrom() {
		return this.from;
	}

	/**
	 * Gets the final location to which distance is calculated.
	 *
	 * @return The final location.
	 */
	public Location getTo() {
		return this.to;
	}
}
