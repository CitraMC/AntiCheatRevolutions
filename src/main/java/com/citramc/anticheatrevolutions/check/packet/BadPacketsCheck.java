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
package com.citramc.anticheatrevolutions.check.packet;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.Backend;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.event.EventListener;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;

public final class BadPacketsCheck {

	public static void runCheck(final Player player, final PlayerMoveEvent event) {
		if (event.isCancelled()) {
			// Do not check if cancelled
			return;
		}

		final Backend backend = AntiCheatRevolutions.getManager().getBackend();
		// Confirm if we should even check for BadPackets
		if (!AntiCheatRevolutions.getManager().getCheckManager().willCheck(player, CheckType.BADPACKETS)
				|| backend.isMovingExempt(player) || player.isDead()) {
			return;
		}

		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(player.getUniqueId());
		final Location to = event.getTo();
		if (to == null) {
			return;
		}
		final float pitch = to.getPitch();
		// Check for derp
		if (Math.abs(pitch) > 90) {
			flag(player, event, "had an illegal pitch", null);
			return;
		}

		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();
		final double tps = AntiCheatRevolutions.getPlugin().getTPS();
		final MovementManager movementManager = AntiCheatRevolutions.getManager().getUserManager()
				.getUser(player.getUniqueId()).getMovementManager();
		if (user.isLagging() || tps < checksConfig.getDouble(CheckType.BADPACKETS, "minimumTps")
				|| (System.currentTimeMillis() - movementManager.getLastTeleport() <= checksConfig
						.getInteger(CheckType.BADPACKETS, "teleportCompensation"))) {
			return;
		}

		final double x = to.getX();
		final double y = to.getY();
		final double z = to.getZ();
		final float yaw = to.getYaw();
		// Create location from new data
		final Location previous = player.getLocation().clone();
		// Only take horizontal distance
		previous.setY(0);
		final Location current = new Location(previous.getWorld(), x, 0, z, yaw, pitch);
		final double distanceHorizontal = previous.distanceSquared(current);
		final double distanceVertical = y - player.getLocation().getY();
		final double maxDistanceHorizontal = checksConfig.getDouble(CheckType.BADPACKETS, "maxDistance")
				+ user.getVelocityTracker().getHorizontal();
		if (distanceHorizontal > maxDistanceHorizontal) {
			flag(player, event,
					"moved too far between packets (HT, distance=" + Utilities.roundDouble(distanceHorizontal, 1)
							+ ", max=" + Utilities.roundDouble(maxDistanceHorizontal, 1) + ")",
					user.getGoodLocation(previous));
			return;
		}

		if (distanceVertical < -4.0D && user.getVelocityTracker().getVertical() == 0.0D) {
			flag(player, event, "moved too far between packets (VT, distance="
					+ Utilities.roundDouble(Math.abs(distanceVertical), 1) + ", max=4.0)",
					user.getGoodLocation(previous));
			return;
		}
	}

	private static void flag(final Player player, final PlayerMoveEvent event, final String message,
			final Location setback) {
		event.setCancelled(true);
		// We are currently not in the main server thread, so switch
		AntiCheatRevolutions.sendToMainThread(() -> {
			EventListener.log(new CheckResult(CheckResult.Result.FAILED, message).getMessage(), player,
					CheckType.BADPACKETS, null);
			player.teleport(Utilities.getSafeLocation(setback != null ? setback : player.getLocation()));
		});
	}

}
