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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.check.CheckResult.Result;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.util.Distance;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;

public final class VelocityCheck {

	public static final Map<UUID, Integer> VIOLATIONS = new HashMap<>();
	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);

	public static CheckResult runCheck(final Player player, final Distance distance) {
		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(player.getUniqueId());
		final MovementManager movementManager = user.getMovementManager();
		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();
		final int minimumPercentage = checksConfig.getInteger(CheckType.VELOCITY, "minimumPercentage");
		final int vlBeforeFlag = checksConfig.getInteger(CheckType.VELOCITY, "vlBeforeFlag");

		if (movementManager.getVelocityExpectedMotionY() > 0) {
			if (!movementManager.isOnGround() || movementManager.getAirTicks() > 5) {
				double percentage = calculatePercentage(movementManager);
				movementManager.setVelocityExpectedMotionY(0); // Reset expected Y motion after calculation

				if (percentage < minimumPercentage) {
					return handleViolation(player.getUniqueId(), percentage, vlBeforeFlag);
				} else {
					VIOLATIONS.remove(player.getUniqueId());
				}
			}
		}

		return PASS;
	}

	private static double calculatePercentage(MovementManager movementManager) {
		double motionY = movementManager.getMotionY();
		double expectedMotionY = movementManager.getVelocityExpectedMotionY();
		double percentage = (motionY / expectedMotionY) * 100;
		return Math.max(percentage, 0); // Ensure percentage is not negative
	}

	private static CheckResult handleViolation(UUID playerId, double percentage, int vlBeforeFlag) {
		int vl = VIOLATIONS.getOrDefault(playerId, 0) + 1;
		VIOLATIONS.put(playerId, vl);
		if (vl >= vlBeforeFlag) {
			return new CheckResult(Result.FAILED,
					"ignored server velocity (pct=" + Utilities.roundDouble(percentage, 2) + ")");
		}
		return PASS;
	}
}
