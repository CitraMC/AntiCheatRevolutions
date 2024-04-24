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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.util.Distance;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;
import com.citramc.anticheatrevolutions.util.VelocityTracker;
import com.citramc.anticheatrevolutions.util.VersionLib;

/**
 * @author Rammelkast
 */
public final class FlightCheck {

	public static final Map<UUID, Long> MOVING_EXEMPT = new HashMap<UUID, Long>();
	public static final Map<UUID, Float> GRAVITY_VIOLATIONS = new HashMap<UUID, Float>();

	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);
	private static final double GRAVITY_FRICTION = 0.98f;

	public static CheckResult runCheck(final Player player, final Distance distance) {
		if (distance.getYDifference() >= AntiCheatRevolutions.getManager().getBackend().getMagic().TELEPORT_MIN()
				|| VersionLib.isFlying(player) || player.getVehicle() != null
				|| AntiCheatRevolutions.getManager().getBackend().isMovingExempt(player)) {
			// This was a teleport or user is flying/using elytra/in a vehicle, so we don't
			// care about it.
			return PASS;
		}

		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(player.getUniqueId());
		final MovementManager movementManager = user.getMovementManager();
		final VelocityTracker velocityTracker = user.getVelocityTracker();
		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();

		if (movementManager.getNearLiquidTicks() > 0 || movementManager.isHalfMovement() || Utilities.isNearClimbable(player)
				|| movementManager.getRiptideTicks() > 0) {
			return PASS;
		}

		int minAirTicks = 13;
		if (player.hasPotionEffect(VersionLib.getJumpEffectType())) {
			minAirTicks += VersionLib.getPotionLevel(player, VersionLib.getJumpEffectType()) * 3;
		}

		if (movementManager.getHalfMovementHistoryCounter() > 25) {
			minAirTicks += 5;
		}

		// Start AirFlight
		if (checksConfig.isSubcheckEnabled(CheckType.FLIGHT, "airFlight") && movementManager.getAirTicks() > minAirTicks
				&& movementManager.getElytraEffectTicks() <= 25) {
			// Config default base is 1200ms
			// Ping clamped to max. 1000 to prevent spoofing for an advantage
			int blockPlaceAccountingTime = (int) (checksConfig.getInteger(CheckType.FLIGHT, "airFlight",
					"accountForBlockPlacement") + (0.25 * (user.getPing() > 1000 ? 1000 : user.getPing())));
			// Config default account is 250ms
			if (AntiCheatRevolutions.getPlugin().getTPS() < 18.0) {
				blockPlaceAccountingTime += checksConfig.getInteger(CheckType.FLIGHT, "airFlight",
						"accountForTpsDrops");
			}

			final long lastPlacedBlock = AntiCheatRevolutions.getManager().getBackend().placedBlock
					.containsKey(player.getUniqueId())
							? AntiCheatRevolutions.getManager().getBackend().placedBlock.get(player.getUniqueId())
							: (blockPlaceAccountingTime + 1);
			double maxMotionY = System.currentTimeMillis() - lastPlacedBlock > blockPlaceAccountingTime ? 0 : 0.42;
			// Fixes snow false positive
			if (movementManager.getMotionY() < 0.004 && Utilities
					.isNearHalfblock(distance.getFrom().getBlock().getRelative(BlockFace.DOWN).getLocation())) {
				maxMotionY += 0.004D;
			}

			// Fixes water false positive
			if (Utilities.isNearWater(player)
					|| Utilities.isNearWater(distance.getFrom().clone().subtract(0, 0.51, 0))) {
				maxMotionY += 0.05;
			}

			// Velocity adjustment
			maxMotionY += velocityTracker.getVertical();

			if (movementManager.getMotionY() > maxMotionY && movementManager.getSlimeInfluenceTicks() <= 0
					&& !Utilities.isNearClimbable(distance.getTo().clone().subtract(0, 1.25, 0))
					&& !Utilities.isNearClimbable(distance.getTo().clone().subtract(0, 0.75, 0))
					&& (!Utilities.isNearWater(distance.getTo().clone().subtract(0, 1.5, 0))
							&& distance.getTo().clone().subtract(0, 0.5, 0).getBlock().getType() != Material.AIR)) {
				return new CheckResult(CheckResult.Result.FAILED, "AirFlight",
						"tried to fly on the Y-axis (mY=" + Utilities.roundDouble(movementManager.getMotionY(), 4) + ", max=" + Utilities.roundDouble(maxMotionY, 4) + ")");
			}

			// TODO falses when falling large distances
			if (Math.abs(movementManager.getMotionY()
					- movementManager.getLastMotionY()) < (movementManager.getAirTicks() >= 115 ? 1E-3 : 5E-3)
					&& !Utilities.couldBeOnBoat(player)
					&& (System.currentTimeMillis() - movementManager.getLastTeleport() >= checksConfig
							.getInteger(CheckType.FLIGHT, "airFlight", "accountForTeleports"))
					&& !VersionLib.isSlowFalling(player) && !Utilities.isNearWeb(player)
					&& movementManager.getElytraEffectTicks() <= 25
					&& !Utilities.isNearClimbable(distance.getFrom().clone().subtract(0, 0.51D, 0))
					&& !Utilities.isNearWater(player)
					&& !Utilities.isNearWater(distance.getFrom().clone().subtract(0, 0.51, 0))
					&& velocityTracker.getVertical() == 0.0) {
				return new CheckResult(CheckResult.Result.FAILED, "AirFlight", "had too little Y dropoff (diff="
						+ Utilities.roundDouble(Math.abs(movementManager.getMotionY() - movementManager.getLastMotionY()), 4) + ")");
			}
		}
		// End AirFlight

		// Start AirClimb
		if (checksConfig.isSubcheckEnabled(CheckType.FLIGHT, "airClimb") && movementManager.getLastMotionY() > 0
				&& movementManager.getMotionY() > 0 && movementManager.getAirTicks() == 2
				&& Math.round(movementManager.getLastMotionY() * 1000) != 420
				&& Math.round(movementManager.getMotionY() * 1000) != 248
				&& !(Math.round(movementManager.getMotionY() * 1000) == 333
						&& Math.round(movementManager.getLastMotionY() * 1000) != 333)
				&& !velocityTracker.isVelocitized() && !player.hasPotionEffect(VersionLib.getJumpEffectType())
				&& (System.currentTimeMillis() - movementManager.getLastTeleport() >= checksConfig
						.getInteger(CheckType.FLIGHT, "airClimb", "accountForTeleports"))
				&& (!Utilities.isNearBed(distance.getTo()) || ((Utilities.isNearBed(distance.getTo())
						|| Utilities.isNearBed(distance.getTo().clone().add(0, -0.51, 0)))
						&& movementManager.getMotionY() > 0.15))
				&& movementManager.getSlimeInfluenceTicks() == 0 && movementManager.getElytraEffectTicks() <= 25
				&& !Utilities.couldBeOnBoat(player, 0.8, false)) {
			return new CheckResult(CheckResult.Result.FAILED, "AirClimb",
					"tried to climb air (mY=" + Utilities.roundDouble(movementManager.getMotionY(), 4) + ")");
		}

		if (checksConfig.isSubcheckEnabled(CheckType.FLIGHT, "airClimb") && movementManager.getMotionY() > 0.42
				&& movementManager.getAirTicks() > 2 && !velocityTracker.isVelocitized()
				&& !player.hasPotionEffect(VersionLib.getJumpEffectType())
				&& !(Math.round(movementManager.getMotionY() * 1000) == 425 && movementManager.getAirTicks() == 11)
				&& (System.currentTimeMillis() - movementManager.getLastTeleport() >= checksConfig
						.getInteger(CheckType.FLIGHT, "airClimb", "accountForTeleports"))
				&& movementManager.getSlimeInfluenceTicks() == 0 && movementManager.getElytraEffectTicks() <= 25) {
			return new CheckResult(CheckResult.Result.FAILED, "AirClimb",
					"tried to climb air (mY=" + Utilities.roundDouble(movementManager.getMotionY(), 4) + ", at=" + movementManager.getAirTicks() + ")");
		}

		if (checksConfig.isSubcheckEnabled(CheckType.FLIGHT, "airClimb") && movementManager.getAirTicks() >= minAirTicks
				&& movementManager.getLastMotionY() < 0 && movementManager.getMotionY() > 0
				&& movementManager.getMotionY() > velocityTracker.getVertical() && movementManager.getElytraEffectTicks() <= 25
				&& (System.currentTimeMillis() - movementManager.getLastTeleport() >= checksConfig
						.getInteger(CheckType.FLIGHT, "airClimb", "accountForTeleports"))
				&& !(Math.round(movementManager.getMotionY() * 1000) == 396) && movementManager.getAirTicks() == 15) {
			return new CheckResult(CheckResult.Result.FAILED, "AirClimb",
					"tried to climb air (mY=" + Utilities.roundDouble(movementManager.getMotionY(), 4) + ", at=" + movementManager.getAirTicks() + ")");
		}

		if (checksConfig.isSubcheckEnabled(CheckType.FLIGHT, "airClimb") && movementManager.getLastMotionY() > 0.0
				&& movementManager.getMotionY() > 0.0 && movementManager.getAirTicks() > 3
				&& movementManager.getMotionY() > movementManager.getLastMotionY()
				&& user.getVelocityTracker().getVertical() <= 0.0
				&& (System.currentTimeMillis() - movementManager.getLastTeleport() >= checksConfig
				.getInteger(CheckType.FLIGHT, "airClimb", "accountForTeleports"))) {
			return new CheckResult(CheckResult.Result.FAILED, "AirClimb",
					"tried to climb air (mY=" + Utilities.roundDouble(movementManager.getMotionY(), 4) + ", at=" + movementManager.getAirTicks() + ")");
		}

		if (checksConfig.isSubcheckEnabled(CheckType.FLIGHT, "airClimb") && movementManager.getAirTicks() >= minAirTicks
				&& !velocityTracker.isVelocitized() && movementManager.getSlimeInfluenceTicks() <= 0
				&& movementManager.getElytraEffectTicks() <= 25
				&& (System.currentTimeMillis() - movementManager.getLastTeleport() >= checksConfig
						.getInteger(CheckType.FLIGHT, "airClimb", "accountForTeleports"))
				&& !Utilities.isNearClimbable(distance.getFrom(), 0.8)) {
			// Config default base is 1200ms
			// Ping clamped to max. 1000 to prevent spoofing for an advantage
			int blockPlaceAccountingTime = (int) (checksConfig.getInteger(CheckType.FLIGHT, "airFlight",
					"accountForBlockPlacement") + (0.25 * (user.getPing() > 1000 ? 1000 : user.getPing())));
			// Config default account is 250ms
			if (AntiCheatRevolutions.getPlugin().getTPS() < 18.0) {
				blockPlaceAccountingTime += checksConfig.getInteger(CheckType.FLIGHT, "airFlight",
						"accountForTpsDrops");
			}

			final long lastPlacedBlock = AntiCheatRevolutions.getManager().getBackend().placedBlock
					.containsKey(player.getUniqueId())
							? AntiCheatRevolutions.getManager().getBackend().placedBlock.get(player.getUniqueId())
							: (blockPlaceAccountingTime + 1);
			final double maxMotionY = System.currentTimeMillis() - lastPlacedBlock > blockPlaceAccountingTime ? 0
					: 0.42;
			if (movementManager.getMotionY() > maxMotionY) {
				return new CheckResult(CheckResult.Result.FAILED, "AirClimb",
						"tried to climb air (mY=" + Utilities.roundDouble(movementManager.getMotionY(), 4) + ", at=" + movementManager.getAirTicks() + ")");
			}
		}
		// End AirClimb

		// Start GroundFlight
		if (checksConfig.isSubcheckEnabled(CheckType.FLIGHT, "groundFlight") && movementManager.isOnGround()
				&& Utilities.cantStandAt(distance.getTo().getBlock().getRelative(BlockFace.DOWN))
				&& Utilities.cantStandAt(distance.getFrom().getBlock().getRelative(BlockFace.DOWN))
				&& Utilities.cantStandAt(distance.getTo().getBlock())) {
			return new CheckResult(CheckResult.Result.FAILED, "GroundFlight",
					"faked ground to fly (mY=" + Utilities.roundDouble(movementManager.getMotionY(), 4) + ", gt=" + movementManager.getGroundTicks() + ")");
		}
		// End GroundFlight

		// Start Gravity
		if (checksConfig.isSubcheckEnabled(CheckType.FLIGHT, "gravity") && !movementManager.isOnGround()
				&& movementManager.getMotionY() < 0 && Math.abs(movementManager.getMotionY()) > velocityTracker.getVertical()
				&& (System.currentTimeMillis() - movementManager.getLastTeleport() >= checksConfig
						.getInteger(CheckType.FLIGHT, "gravity", "accountForTeleports"))
				&& !Utilities.isNearWeb(player) && movementManager.getElytraEffectTicks() <= 25
				&& !VersionLib.isSlowFalling(player)) {
			final double gravitatedY = (movementManager.getLastMotionY() - 0.08) * GRAVITY_FRICTION;
			final double offset = Math.abs(gravitatedY - movementManager.getMotionY());
			double maxOffset = checksConfig.getDouble(CheckType.FLIGHT, "gravity", "maxOffset");
			if (Utilities.isNearClimbable(distance.getFrom().clone().subtract(0, 0.51D, 0))
					|| Utilities.isNearClimbable(distance.getFrom()) || Utilities.isNearWater(player)
					|| (!Utilities.isNearWater(distance.getTo().clone().subtract(0, 1.5, 0))
							&& distance.getTo().clone().subtract(0, 0.5, 0).getBlock().getType() != Material.AIR))
				maxOffset += 0.15D;
			if (offset > maxOffset && movementManager.getAirTicks() > 2) {
				float vl = GRAVITY_VIOLATIONS.getOrDefault(player.getUniqueId(), 0f) + 1;
				GRAVITY_VIOLATIONS.put(player.getUniqueId(), vl);
				int vlBeforeFlag = checksConfig.getInteger(CheckType.FLIGHT, "gravity", "vlBeforeFlag");
				if (vl >= vlBeforeFlag) {
					GRAVITY_VIOLATIONS.put(player.getUniqueId(), Math.max(0, vl - 2));
					return new CheckResult(CheckResult.Result.FAILED, "Gravity",
							"ignored gravity (offset=" + Utilities.roundDouble(offset, 4) + ", at=" + movementManager.getAirTicks() + ")");
				}
			} else {
				if (GRAVITY_VIOLATIONS.containsKey(player.getUniqueId())) {
					final float vl = GRAVITY_VIOLATIONS.getOrDefault(player.getUniqueId(), 0f);
					GRAVITY_VIOLATIONS.put(player.getUniqueId(), Math.max(0, vl - 0.5f));
				}
			}
		}
		// End Gravity

		return PASS;
	}

}
