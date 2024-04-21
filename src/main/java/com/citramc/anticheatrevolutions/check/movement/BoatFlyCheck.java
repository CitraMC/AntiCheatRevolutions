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
package com.citramc.anticheatrevolutions.check.movement;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.cryptomorin.xseries.XMaterial;
import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.Configuration;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.manage.AntiCheatManager;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.Utilities;
import com.citramc.anticheatrevolutions.util.VersionLib;

public class BoatFlyCheck {

	private static final Map<UUID, Integer> VIOLATIONS = new ConcurrentHashMap<>();

	public static CheckResult runCheck(final Player player, final MovementManager movementManager, final Location to) {
		final AntiCheatManager manager = AntiCheatRevolutions.getManager();
		final Configuration configuration = manager.getConfiguration();
		final Checks checksConfig = configuration.getChecks();

		if (!player.isInsideVehicle() || player.getVehicle().getType() != EntityType.BOAT
				|| movementManager.getMotionY() <= 1E-3
				|| (System.currentTimeMillis() - movementManager.getLastTeleport() <= 150)
				|| VersionLib.isFlying(player)) {
			return new CheckResult(CheckResult.Result.PASSED);
		}

		Block bottom = player.getWorld().getBlockAt(to.getBlockX(), to.getBlockY() - 1, to.getBlockZ());
		if (!Utilities.cantStandAt(bottom) || bottom.getType() == XMaterial.WATER.parseMaterial()) {
			return new CheckResult(CheckResult.Result.PASSED);
		}

		UUID uuid = player.getUniqueId();
		int violationsCount = VIOLATIONS.getOrDefault(uuid, 0) + 1;
		VIOLATIONS.put(uuid, violationsCount);
		if (violationsCount >= checksConfig.getInteger(CheckType.BOATFLY, "vlBeforeFlag")) {
			VIOLATIONS.put(uuid, 0); // Reset violations after flagging
			return new CheckResult(CheckResult.Result.FAILED,
					"tried to fly in a boat (mY=" + movementManager.getMotionY()
							+ ", bottom=" + bottom.getType().name().toLowerCase() + ")");
		}
		return new CheckResult(CheckResult.Result.PASSED);
	}
}
