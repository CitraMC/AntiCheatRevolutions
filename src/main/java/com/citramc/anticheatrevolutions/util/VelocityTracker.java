/*
 * AntiCheatRevolutions for Bukkit and Spigot.
 * Copyright (c) 2021 Marco Moesman
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class VelocityTracker {

	private final long velocitizedTime;
	private final List<VelocityWrapper> velocities = new LinkedList<>();

	public void registerVelocity(final Vector vector) {
		this.velocities.add(new VelocityWrapper(vector.getX(), vector.getY(), vector.getZ(),
				Math.hypot(vector.getX(), vector.getZ()), Math.abs(vector.getY()), System.currentTimeMillis()));
	}

	public void tick() {
		Iterator<VelocityWrapper> iterator = this.velocities.iterator();
		long currentTime = System.currentTimeMillis();
		while (iterator.hasNext()) {
			if (iterator.next().getTimestamp() + this.velocitizedTime < currentTime) {
				iterator.remove();
			}
		}
	}

	public double getHorizontal() {
		return this.velocities.stream()
				.mapToDouble(VelocityWrapper::getHorizontal)
				.max()
				.orElse(0.0);
	}

	public double getVertical() {
		return this.velocities.stream()
				.mapToDouble(VelocityWrapper::getVertical)
				.max()
				.orElse(0.0);
	}

	public boolean isVelocitized() {
		return !this.velocities.isEmpty();
	}

	@RequiredArgsConstructor
	@Getter
	private class VelocityWrapper {
		private final double motionX, motionY, motionZ;
		private final double horizontal, vertical;
		private final long timestamp;
	}

}
