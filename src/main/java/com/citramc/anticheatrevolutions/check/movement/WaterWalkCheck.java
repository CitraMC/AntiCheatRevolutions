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

import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckResult.Result;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;
import com.citramc.anticheatrevolutions.util.VersionLib;

public final class WaterWalkCheck {

	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);

	public static CheckResult runCheck(final Player player, final double x, final double y, final double z) {
		final UUID uuid = player.getUniqueId();
		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(uuid);
		final MovementManager movementManager = user.getMovementManager();

		if (movementManager.getDistanceXZ() <= 0 || player.getVehicle() != null || Utilities.isOnLilyPad(player) || Utilities.isOnCarpet(player)
				|| movementManager.getRiptideTicks() > 0 || VersionLib.isSwimming(player) || VersionLib.isFlying(player) || user.getVelocityTracker().isVelocitized()) {
			return PASS;
		}

		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();
		final Block blockBeneath = player.getLocation().clone().subtract(0, 0.1, 0).getBlock();
		if (checksConfig.isSubcheckEnabled(CheckType.WATER_WALK, "walk") && blockBeneath.isLiquid()
				&& Utilities.isSurroundedByWater(player)
				&& ((movementManager.getMotionY() == 0 && movementManager.getLastMotionY() == 0)
						|| movementManager.getMotionY() == Utilities.JUMP_MOTION_Y)
				&& movementManager.getDistanceXZ() > checksConfig.getDouble(CheckType.WATER_WALK, "walk", "minimumDistXZ")
				&& !movementManager.isTopSolid() && !Utilities.couldBeOnBoat(player, 3, false)) {
			return new CheckResult(Result.FAILED, "Walk",
					"tried to walk on water (xz=" + Utilities.roundDouble(movementManager.getDistanceXZ(), 5) + ")");
		}

		if (checksConfig.isSubcheckEnabled(CheckType.WATER_WALK, "hop") && blockBeneath.isLiquid()
				&& Utilities.isSurroundedByWater(player) && movementManager.isOnGround()
				&& Math.abs(movementManager.getMotionY()) < checksConfig.getDouble(CheckType.WATER_WALK, "hop", "maxMotionY")
				&& !Utilities.couldBeOnBoat(player, 0.3, false)) {
			return new CheckResult(Result.FAILED, "Hop",
					"tried to hop on water (mY=" + Utilities.roundDouble(movementManager.getMotionY(), 5) + ")");
		}

		final double minAbsMotionY = 0.12D + (VersionLib.getPotionLevel(player, PotionEffectType.SPEED) * 0.05D);
		if (checksConfig.isSubcheckEnabled(CheckType.WATER_WALK, "lunge") && blockBeneath.isLiquid()
				&& Utilities.isSurroundedByWater(player)
				&& Math.abs(movementManager.getLastMotionY() - movementManager.getMotionY()) > minAbsMotionY
				&& movementManager.getDistanceXZ() > checksConfig.getDouble(CheckType.WATER_WALK, "lunge", "minimumDistXZ")
				&& movementManager.getLastMotionY() > -0.25
				&& !Utilities.couldBeOnBoat(player, 0.3, false)) {
			return new CheckResult(Result.FAILED, "Lunge", "tried to lunge in water (xz="
					+ Utilities.roundDouble(movementManager.getDistanceXZ(), 5) + ", absMotionY="
					+ Utilities.roundDouble(Math.abs(movementManager.getLastMotionY() - movementManager.getMotionY()), 5) + ")");
		}

		if (checksConfig.isSubcheckEnabled(CheckType.WATER_WALK, "lunge") && Utilities.isSurroundedByWater(player)
				&& movementManager.getDistanceXZ() > 0.16 && movementManager.getMotionY() > 0.0
				&& movementManager.getLastMotionY() < 0.0
				&& (Math.abs(movementManager.getMotionY()) < 0.017 && Math.abs(movementManager.getLastMotionY()) < 0.02)
				&& Math.abs(movementManager.getLastMotionY() - 0.004) < 0.021
				&& user.getVelocityTracker().getHorizontal() < 0.04
				&& !Utilities.couldBeOnBoat(player, 0.3, false)) {
			return new CheckResult(Result.FAILED, "Lunge", "tried to lunge in water (xz="
					+ Utilities.roundDouble(movementManager.getDistanceXZ(), 5) + ", motionY="
					+ Utilities.roundDouble(movementManager.getMotionY(), 5) + ", lastMotionY="
							+ Utilities.roundDouble(movementManager.getLastMotionY(), 5) + ")");
		}
		return PASS;
	}

}
