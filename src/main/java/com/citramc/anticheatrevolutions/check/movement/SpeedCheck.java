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

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import com.cryptomorin.xseries.XMaterial;
import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.Backend;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.check.CheckResult.Result;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.util.Distance;
import com.citramc.anticheatrevolutions.util.Friction;
import com.citramc.anticheatrevolutions.util.MinecraftVersion;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;
import com.citramc.anticheatrevolutions.util.VelocityTracker;
import com.citramc.anticheatrevolutions.util.VersionLib;

/**
 * @author Rammelkast TODO soulsand speed TODO buffer system for all
 */
public final class SpeedCheck {

	public static final Map<UUID, Double> PREDICT_BUFFER = new HashMap<>();
	public static final Map<UUID, Double> AIRSPEED_BUFFER = new HashMap<>();
	public static final Map<UUID, Double> AIRACCELERATION_BUFFER = new HashMap<>();
	public static final Map<UUID, Double> GROUNDSPEED_BUFFER = new HashMap<>();

	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);

	private static boolean isSpeedExempt(final Player player, final Backend backend) {
		return backend.isMovingExempt(player) || VersionLib.isFlying(player);
	}

	/**
	 * Largely based on Elevated's Frequency speed check
	 * 
	 * @see https://github.com/ElevatedDev/Frequency/blob/master/src/main/java/xyz/elevated/frequency/check/impl/speed/Speed.java
	 * 
	 *      TODO fix falses with entity collisions
	 */
	public static CheckResult checkPredict(final Player player, final Location movingTowards) {
		if (!MinecraftVersion.getCurrentVersion().isAtLeast(MinecraftVersion.COMBAT_UPDATE)) {
			return PASS;
		}

		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();
		if (!checksConfig.isSubcheckEnabled(CheckType.SPEED, "predict")) {
			return PASS;
		}

		final Backend backend = AntiCheatRevolutions.getManager().getBackend();
		if (isSpeedExempt(player, backend) || player.getVehicle() != null || Utilities.isInWater(player)
				|| Utilities.isInWeb(player)) {
			return PASS;
		}

		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(player.getUniqueId());
		final UUID uuid = player.getUniqueId();
		final MovementManager movementManager = user.getMovementManager();
		final VelocityTracker velocityTracker = user.getVelocityTracker();

		final double motion = movementManager.getDistanceXZ();
		final boolean boxedIn = movementManager.isTopSolid() && movementManager.isBottomSolid();
		final boolean sprinting = true; // TODO do this using packets

		double buffer = PREDICT_BUFFER.getOrDefault(uuid, 0.0);
		double movementSpeed = Utilities.getMovementSpeed(player);
		if (movementManager.isWasOnGround()) {
			if (sprinting) {
				movementSpeed *= 1.3f;
			}
			movementManager.setFriction(movementManager.getFriction() * 0.91f);
			movementSpeed *= 0.16277136 / Math.pow(movementManager.getFriction(), 3);
			if (!movementManager.isOnGround()
					&& (movementManager.getMotionY() >= 0.42f || (boxedIn && movementManager.getMotionY() > 0.0f))
					&& sprinting) {
				movementSpeed += 0.2f;
			}
		} else {
			movementSpeed = sprinting ? 0.026f : 0.02f;
			movementManager.setFriction(0.91f);
		}

		movementSpeed += velocityTracker.getHorizontal();

		final double factor = (motion - movementManager.getAdjusted()) / movementSpeed;
		final boolean exempted = Utilities.isCollisionPoint(movingTowards,
				material -> material == XMaterial.HONEY_BLOCK.parseMaterial());
		if (factor > 1.001 && !exempted) {
			if (buffer++ > 2.5) {
				buffer /= 2;
				PREDICT_BUFFER.put(uuid, buffer);
				return new CheckResult(Result.FAILED, "Predict",
						"moved unexpectedly (factor=" + Utilities.roundDouble(factor, 3) + ")");
			}
		} else {
			buffer = Math.max(buffer - 0.05, 0.0);
		}

		PREDICT_BUFFER.put(uuid, buffer);

		movementManager.setAdjusted(motion * movementManager.getFriction());
		movementManager.setFriction(Friction.getFactor(movingTowards.clone().subtract(0, 1, 0).getBlock()));
		return PASS;
	}

	// Entry method for handling XZ speed checks
	public static CheckResult checkXZSpeed(final Player player, final Location movingTowards) {
		final Backend backend = AntiCheatRevolutions.getManager().getBackend();
		if (isSpeedExempt(player, backend) || player.getVehicle() != null || Utilities.isInWater(player)) {
			return PASS;
		}

		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(player.getUniqueId());
		final MovementManager movementManager = user.getMovementManager();
		final VelocityTracker velocityTracker = user.getVelocityTracker();

		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();

		// Execute specific speed checks based on conditions
		if (checksConfig.isSubcheckEnabled(CheckType.SPEED, "airSpeed") && !movementManager.isOnGround()) {
			return checkAirSpeed(player, movementManager, velocityTracker, checksConfig);
		}

		if (checksConfig.isSubcheckEnabled(CheckType.SPEED, "airAcceleration") && !movementManager.isOnGround()) {
			return checkAirAcceleration(player, movementManager, velocityTracker, checksConfig);
		}

		if (checksConfig.isSubcheckEnabled(CheckType.SPEED, "jumpBehaviour") && !movementManager.isOnGround()) {
			return checkJumpBehaviour(player, movementManager, checksConfig);
		}

		if (checksConfig.isSubcheckEnabled(CheckType.SPEED, "groundSpeed") && movementManager.isOnGround()) {
			return checkGroundSpeed(player, movementManager, velocityTracker, checksConfig);
		}

		return PASS;
	}

	// AirSpeed specific check
	private static CheckResult checkAirSpeed(Player player, MovementManager movementManager,
			VelocityTracker velocityTracker, Checks checksConfig) {
		if (!checksConfig.isSubcheckEnabled(CheckType.SPEED, "airSpeed")) {
			return PASS; // Early exit if the airSpeed check is disabled
		}

		double distanceXZ = movementManager.getDistanceXZ();
		double predictedSpeed = calculatePredictedAirSpeed(movementManager, checksConfig);
		double buffer = AIRSPEED_BUFFER.getOrDefault(player.getUniqueId(), 0.0);

		// Fetch configuration values from the configuration, using default values if
		// they are not set
		double baseLimit = checksConfig.getDouble(CheckType.SPEED, "airSpeed", "baseLimit");
		double walkSpeedMultiplier = checksConfig.getDouble(CheckType.SPEED, "airSpeed", "walkSpeedMultiplier");

		// Calculate effective speed limit
		double effectiveSpeedLimit = baseLimit + (walkSpeedMultiplier * (player.getWalkSpeed() / 0.2 - 1));

		if (distanceXZ - predictedSpeed > effectiveSpeedLimit) {
			buffer++; // Increment the buffer if the player moves faster than allowed
			if (buffer > 3) { // Trigger a failure if the buffer exceeds a threshold
				buffer /= 2; // Reset the buffer to allow for recovery
				AIRSPEED_BUFFER.put(player.getUniqueId(), buffer);
				return new CheckResult(Result.FAILED, "AirSpeed",
						"moved too fast in air: " + distanceXZ + " > " + effectiveSpeedLimit);
			}
		} else {
			buffer = Math.max(buffer - 0.1, 0); // Gradually reduce the buffer
		}

		AIRSPEED_BUFFER.put(player.getUniqueId(), buffer);
		return PASS;
	}

	private static double calculatePredictedAirSpeed(MovementManager movementManager, Checks checksConfig) {
		// Default values
		double decayFactor = 0.985;
		double baseSpeed = 0.36; // Base air speed coefficient

		// Calculate the predicted speed
		double predictedSpeed = baseSpeed * Math.pow(decayFactor, movementManager.getAirTicks() + 1);

		// Check if the player is boxed in which might affect their maneuverability and
		// speed
		boolean boxedIn = movementManager.isTopSolid() && movementManager.isBottomSolid();
		if (boxedIn) {
			double boxedInMultiplier = 1.2;
			predictedSpeed *= boxedInMultiplier;
		}

		return predictedSpeed;
	}

	// AirAcceleration specific check
	private static CheckResult checkAirAcceleration(Player player, MovementManager movementManager,
			VelocityTracker velocityTracker, Checks checksConfig) {
		double acceleration = movementManager.getAcceleration();
		double limit = checksConfig.getDouble(CheckType.SPEED, "airAcceleration", "baseLimit");
		double buffer = AIRACCELERATION_BUFFER.getOrDefault(player.getUniqueId(), 0.0);

		if (acceleration > limit) {
			if (++buffer > 1.5) {
				buffer /= 2;
				AIRACCELERATION_BUFFER.put(player.getUniqueId(), buffer);
				return new CheckResult(Result.FAILED, "AirAcceleration", "exceeded acceleration limits");
			}
		} else {
			buffer = Math.max(buffer - 0.05, 0);
		}

		AIRACCELERATION_BUFFER.put(player.getUniqueId(), buffer);
		return PASS;
	}

	private static CheckResult checkJumpBehaviour(Player player, MovementManager movementManager, Checks checksConfig) {
		if (!checksConfig.isSubcheckEnabled(CheckType.SPEED, "jumpBehaviour")) {
			return PASS;
		}

		// Variables to determine jump behavior
		double distanceXZ = movementManager.getDistanceXZ();
		boolean isOnGround = movementManager.isOnGround();
		int airTicks = movementManager.getAirTicks();
		int groundTicks = movementManager.getGroundTicks();

		// Configuration values
		double maxAllowedJumpDistance = checksConfig.getDouble(CheckType.SPEED, "jumpBehaviour", "maxDistance");
		double minAllowedJumpDistance = checksConfig.getDouble(CheckType.SPEED, "jumpBehaviour", "minDistance");
		int airTimeLimit = checksConfig.getInteger(CheckType.SPEED, "jumpBehaviour", "airTimeLimit");

		// Check conditions for a valid jump
		if (!isOnGround && airTicks < airTimeLimit
				&& (distanceXZ < minAllowedJumpDistance || distanceXZ > maxAllowedJumpDistance)) {
			return new CheckResult(Result.FAILED, "JumpBehaviour", String
					.format("Unusual jumping behaviour detected: distance=%.3f, airTicks=%d", distanceXZ, airTicks));
		}

		// Ensure the player returns to ground within the expected time
		if (isOnGround && groundTicks > 1 && airTicks > airTimeLimit) {
			return new CheckResult(Result.FAILED, "JumpBehaviour", "Jump air time exceeded expected limits");
		}

		return PASS;
	}

	// GroundSpeed specific check
	private static CheckResult checkGroundSpeed(Player player, MovementManager movementManager,
			VelocityTracker velocityTracker, Checks checksConfig) {
		double distanceXZ = movementManager.getDistanceXZ();
		double limit = checksConfig.getDouble(CheckType.SPEED, "groundSpeed", "baseLimit")
				- 0.0055 * Math.min(9, movementManager.getGroundTicks());
		double buffer = GROUNDSPEED_BUFFER.getOrDefault(player.getUniqueId(), 0.0);

		if (distanceXZ > limit) {
			if (++buffer > 1.5) {
				buffer /= 2;
				GROUNDSPEED_BUFFER.put(player.getUniqueId(), buffer);
				return new CheckResult(Result.FAILED, "GroundSpeed", "moved too fast on ground");
			}
		} else {
			buffer = Math.max(buffer - 0.05, 0);
		}

		GROUNDSPEED_BUFFER.put(player.getUniqueId(), buffer);
		return PASS;
	}

	public static CheckResult checkVerticalSpeed(final Player player, final Distance distance) {
		final Backend backend = AntiCheatRevolutions.getManager().getBackend();
		if (isSpeedExempt(player, backend) || player.getVehicle() != null || Utilities.isNearWater(player)
				|| player.isSleeping()) {
			return PASS;
		}

		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(player.getUniqueId());
		final MovementManager movementManager = user.getMovementManager();
		final VelocityTracker velocityTracker = user.getVelocityTracker();

		if (movementManager.getRiptideTicks() > 0) {
			return PASS;
		}

		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();
		if (!checksConfig.isSubcheckEnabled(CheckType.SPEED, "verticalSpeed")) {
			return PASS;
		}

		double maxMotionY = getMaxAcceptableMotionY(player, distance.getTo(), movementManager, checksConfig);
		double currentMotionY = movementManager.getMotionY();

		// Adjust for vertical velocity from effects like explosions or external forces
		maxMotionY += velocityTracker.getVertical();

		if (currentMotionY > maxMotionY) {
			return new CheckResult(Result.FAILED, "VerticalSpeed", String.format(
					"exceeded vertical speed limit (motionY=%.4f, max=%.4f)", currentMotionY, maxMotionY));
		}

		return PASS;
	}

	private static double getMaxAcceptableMotionY(final Player player, final Location toLocation,
			final MovementManager movementManager, final Checks checksConfig) {
		boolean nearBed = Utilities.isNearBed(toLocation);
		boolean onBoat = Utilities.couldBeOnBoat(player);
		boolean onClimbable = Utilities.isClimbableBlock(toLocation.getBlock()) ||
				Utilities.isClimbableBlock(toLocation.getBlock().getRelative(BlockFace.DOWN));
		boolean isHalfMovement = movementManager.isHalfMovement() || Utilities.isNearWall(toLocation);

		double base = onBoat ? 0.6 : (nearBed ? 0.5625 : (isHalfMovement ? 0.6 : 0.42));
		if (onClimbable) {
			base += checksConfig.getDouble(CheckType.SPEED, "verticalSpeed", "climbableCompensation");
		}

		if (player.hasPotionEffect(PotionEffectType.JUMP)) {
			base += VersionLib.getPotionLevel(player, PotionEffectType.JUMP) * 0.2;
		}

		return base;
	}

}
