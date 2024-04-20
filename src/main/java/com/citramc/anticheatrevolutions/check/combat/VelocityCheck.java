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

	public static final Map<UUID, Integer> VIOLATIONS = new HashMap<UUID, Integer>();
	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);

	public static CheckResult runCheck(final Player player, final Distance distance) {
		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(player.getUniqueId());
		final MovementManager movementManager = user.getMovementManager();
		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();
		final int minimumPercentage = checksConfig.getInteger(CheckType.VELOCITY, "minimumPercentage");
		final int vlBeforeFlag = checksConfig.getInteger(CheckType.VELOCITY, "vlBeforeFlag");
		
		if (movementManager.getVelocityExpectedMotionY() > 0 && !movementManager.isOnGround()) {
			double percentage = (movementManager.getMotionY() / movementManager.getVelocityExpectedMotionY()) * 100;
			if (percentage < 0) {
				percentage = 0;
			}
			// Reset expected Y motion
			movementManager.setVelocityExpectedMotionY(0);
			if (percentage < minimumPercentage) {
				final int vl = VIOLATIONS.getOrDefault(player.getUniqueId(), 0) + 1;
				VIOLATIONS.put(player.getUniqueId(), vl);
				if (vl >= vlBeforeFlag) {
					return new CheckResult(Result.FAILED, "ignored server velocity (pct=" + Utilities.roundDouble(percentage, 2) + ")");
				}
			} else {
				VIOLATIONS.remove(player.getUniqueId());
			}
		} else if (movementManager.getAirTicks() > 5 && movementManager.getVelocityExpectedMotionY() > 0) {
			movementManager.setVelocityExpectedMotionY(0);
		}
		return PASS;
	}

}
