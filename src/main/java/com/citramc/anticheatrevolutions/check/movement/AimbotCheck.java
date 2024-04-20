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
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;

public final class AimbotCheck {

	private static final double EXPANDER = Math.pow(2, 24);
	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);

	/**
	 * This falses sometimes?
	 * TODO we need to check yaw deltas
	 */
	public static CheckResult runCheck(final Player player, final EntityDamageByEntityEvent event) {
		final Backend backend = AntiCheatRevolutions.getManager().getBackend();
		if (backend.isMovingExempt(player)) {
			return PASS;
		}

		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(player.getUniqueId());
		final MovementManager movementManager = user.getMovementManager();
		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();

		if (shouldFlagAimbot(movementManager, checksConfig, player)) {
			double mod = calculateMod(player, movementManager);
			double pitchAcceleration = calculatePitchAcceleration(movementManager);
			long gcd = calculateGCD(movementManager);

			return new CheckResult(CheckResult.Result.FAILED,
					String.format("failed computational check (gcd=%d, mod=%.5f, accel=%.3f, delta=%.1f)",
							gcd, mod, pitchAcceleration, movementManager.getDeltaPitch()));
		}
		return PASS;
	}

	private static boolean shouldFlagAimbot(MovementManager movementManager, Checks checksConfig, Player player) {
		final float deltaPitch = movementManager.getDeltaPitch();
		final float pitchAcceleration = Math.abs(deltaPitch - movementManager.getLastDeltaPitch());
		final double minAcceleration = checksConfig.getDouble(CheckType.AIMBOT, "minAcceleration");
		final double maxMod = checksConfig.getDouble(CheckType.AIMBOT, "maxMod");

		long gcd = calculateGCD(movementManager);
		double mod = calculateMod(player, movementManager);

		return (gcd > 0L && gcd < 131072L) && mod <= maxMod && pitchAcceleration > minAcceleration && deltaPitch > 5.0f
				&& deltaPitch < 20.0f;
	}

	private static long calculateGCD(MovementManager movementManager) {
		return Utilities.getGcd((long) (movementManager.getDeltaPitch() * EXPANDER),
				(long) (movementManager.getLastDeltaPitch() * EXPANDER));
	}

	private static double calculateMod(Player player, MovementManager movementManager) {
		long gcd = calculateGCD(movementManager);
		return Math.abs(player.getLocation().getPitch() % (gcd / EXPANDER));
	}

	private static double calculatePitchAcceleration(MovementManager movementManager) {
		return Math.abs(movementManager.getDeltaPitch() - movementManager.getLastDeltaPitch());
	}
}
