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
package com.citramc.anticheatrevolutions.check.movement;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.Backend;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.Configuration;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.manage.AntiCheatManager;
import com.citramc.anticheatrevolutions.manage.UserManager;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;

public class AimbotCheck {

	private static final double EXPANDER = 1000000.0;

	public static CheckResult runCheck(final Player player, final EntityDamageByEntityEvent event) {
		final AntiCheatManager manager = AntiCheatRevolutions.getManager();
		final Backend backend = manager.getBackend();
		final UserManager userManager = manager.getUserManager();
		final Configuration configuration = manager.getConfiguration();

		if (backend.isMovingExempt(player)) {
			return new CheckResult(CheckResult.Result.PASSED);
		}

		final User user = userManager.getUser(player.getUniqueId());
		final MovementManager movementManager = user.getMovementManager();
		final Checks checksConfig = configuration.getChecks();

		final float deltaPitch = movementManager.getDeltaPitch();
		final float lastDeltaPitch = movementManager.getLastDeltaPitch();
		final float pitchAcceleration = Math.abs(deltaPitch - lastDeltaPitch);

		final long gcd = Utilities.getGcd((long) (deltaPitch * EXPANDER),
				(long) (lastDeltaPitch * EXPANDER));
		final double mod = Math.abs(player.getLocation().getPitch() % (gcd / EXPANDER));

		final double minAcceleration = checksConfig.getDouble(CheckType.AIMBOT, "minAcceleration");
		final double maxMod = checksConfig.getDouble(CheckType.AIMBOT, "maxMod");

		if (gcdValid(gcd) && mod <= maxMod && pitchAcceleration > minAcceleration && pitchValid(deltaPitch)) {
			return new CheckResult(CheckResult.Result.FAILED,
					String.format("failed computational check (gcd=%d, mod=%.5f, accel=%.3f, delta=%.1f)",
							gcd, mod, pitchAcceleration, deltaPitch));
		}
		return new CheckResult(CheckResult.Result.PASSED);
	}

	private static boolean gcdValid(long gcd) {
		return gcd > 0L && gcd < 131072L;
	}

	private static boolean pitchValid(float deltaPitch) {
		return deltaPitch > 5.0f && deltaPitch < 20.0f;
	}
}
