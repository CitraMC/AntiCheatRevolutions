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
package com.citramc.anticheatrevolutions.check.combat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.check.CheckResult.Result;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.manage.AntiCheatManager;
import com.citramc.anticheatrevolutions.util.Distance;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;

public class VelocityCheck {

	public final static Map<UUID, Integer> VIOLATIONS = new ConcurrentHashMap<>();

	public static CheckResult runCheck(final Player player, final Distance distance) {
		final AntiCheatManager manager = AntiCheatRevolutions.getManager();
		final Checks checksConfig = manager.getConfiguration().getChecks();
		final UUID playerId = player.getUniqueId();
		final User user = manager.getUserManager().getUser(playerId);
		final MovementManager movementManager = user.getMovementManager();

		final int minimumPercentage = checksConfig.getInteger(CheckType.VELOCITY, "minimumPercentage");
		final int vlBeforeFlag = checksConfig.getInteger(CheckType.VELOCITY, "vlBeforeFlag");

		double expectedY = movementManager.getVelocityExpectedMotionY();
		if (expectedY > 0 && !movementManager.isOnGround()) {
			double motionY = movementManager.getMotionY();
			double percentage = motionY > 0 ? (motionY / expectedY) * 100 : 0;
			movementManager.setVelocityExpectedMotionY(0); // Reset expected Y motion immediately to avoid re-use.

			if (percentage < minimumPercentage) {
				int vl = VIOLATIONS.getOrDefault(playerId, 0) + 1;
				VIOLATIONS.put(playerId, vl);
				if (vl >= vlBeforeFlag) {
					return new CheckResult(Result.FAILED,
							"Ignored server velocity (pct=" + Utilities.roundDouble(percentage, 2) + ")");
				}
			} else {
				VIOLATIONS.remove(playerId);
			}
		} else if (movementManager.getAirTicks() > 5 && expectedY > 0) {
			movementManager.setVelocityExpectedMotionY(0);
		}

		return new CheckResult(CheckResult.Result.PASSED);
	}
}
