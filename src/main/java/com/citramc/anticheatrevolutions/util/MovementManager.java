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
package com.citramc.anticheatrevolutions.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;

import lombok.Getter;
import lombok.Setter;

public final class MovementManager {

	// Ticks in air
	@Getter
	private int airTicks = 0;
	// Ticks on ground
	@Getter
	private int groundTicks = 0;
	// Ticks on ice
	@Getter
	private int iceTicks = 0;
	// Ticks on slime
	@Getter
	private int slimeTicks = 0;
	// Ticks in air before last grounded moment
	@Getter
	private int airTicksBeforeGrounded = 0;
	// Ticks influenced by ice
	@Getter
	private int iceInfluenceTicks = 0;
	// Ticks influenced by slime
	@Getter
	private int slimeInfluenceTicks = 0;
	// Ticks influenced by soil
	@Getter
	private int soilInfluenceTicks = 0;
	// Y motion of the movement
	@Getter
	private double motionY;
	// Previous Y motion of the movement
	@Getter
	private double lastMotionY;
	// Horizontal distance of movement
	@Getter
	private double distanceXZ;
	// Horizontal distance on x-axis of movement
	@Getter
	private double distanceX;
	// Horizontal distance on z-axis of movement
	@Getter
	private double distanceZ;
	// Previous horizontal distance of movement
	@Getter
	private double lastDistanceXZ;
	// Previous horizontal distance on x-axis of movement
	@Getter
	private double lastDistanceX;
	// Previous horizontal distance on z-axis of movement
	@Getter
	private double lastDistanceZ;
	// Delta pitch
	@Getter
	private float deltaPitch;
	// Delta yaw
	@Getter
	private float deltaYaw;
	// Previous delta pitch
	@Getter
	private float lastDeltaPitch;
	// Previous delta yaw
	@Getter
	private float lastDeltaYaw;
	// If the player touched the ground again this tick
	@Getter
	private boolean touchedGroundThisTick = false;
	// Last recorded distance
	@Getter
	private Distance lastDistance = new Distance();
	// Movement acceleration
	@Getter
	private double acceleration;
	// Last movement acceleration
	@Getter
	private double lastAcceleration;
	// Is the block above solid
	@Getter
	private boolean topSolid;
	// Is the block below solid
	@Getter
	private boolean bottomSolid;
	// If the current movement is up a slab or stair
	@Getter
	private boolean halfMovement;
	// If the player is on the ground (determined clientside!)
	@Getter
	private boolean onGround;
	// If the player was on the ground (determined clientside!)
	@Getter
	private boolean wasOnGround;
	// Ticks counter for last halfMovement
	@Getter
	private int halfMovementHistoryCounter = 0;
	// Time of last teleport
	@Getter
	@Setter
	private long lastTeleport;
	// Elytra effect ticks
	@Getter
	private int elytraEffectTicks;
	// Used by Velocity check, represents the currently expected Y motion
	@Getter
	@Setter
	private double velocityExpectedMotionY;
	// Used by Velocity check, represents the currently expected XZ motion
	@Getter
	private double velocityExpectedMotionXZ;
	// Amount of ticks a player is sneaking
	@Getter
	private int sneakingTicks;
	// Ticks counter after being near a liquid
	@Getter
	private int nearLiquidTicks;
	// Ticks of player blocking
	@Getter
	private int blockingTicks;
	// If the player has a speed effect
	@Getter
	private boolean hasSpeedEffect = false;
	// If the player had speed effect previous tick
	@Getter
	private boolean hadSpeedEffect = false;
	// Riptiding ticks
	@Getter
	private int riptideTicks;
	// Time of last update
	@Getter
	private long lastUpdate;
	// Last location
	@Getter
	private Location lastLocation;
	
	/**
	 * Used by prediction speed check
	 */
	@Getter
	@Setter
	private float friction;
	@Getter
	@Setter
	private double adjusted;

	// If the player is boxed
	@Getter
	private boolean hasBoxedIn = false;
	// If the player was boxed on previous tick
	@Getter
	private boolean hadBoxedIn = false;

	@SuppressWarnings("deprecation")
	public void handle(final Player player, final Location from, final Location to, final Distance distance) {
		this.wasOnGround = this.onGround;
		this.onGround = player.isOnGround();
		
		this.lastLocation = from;

		final double x = distance.getXDifference();
		final double z = distance.getZDifference();
		this.lastDistanceXZ = this.distanceXZ;
		this.lastDistanceX = this.distanceX;
		this.lastDistanceZ = this.distanceZ;
		this.distanceXZ = Math.sqrt(x * x + z * z);
		this.distanceX = x;
		this.distanceZ = z;
		
		this.lastDeltaPitch = this.deltaPitch;
		this.lastDeltaYaw = this.deltaYaw;
		this.deltaPitch = Math.abs(to.getPitch() - from.getPitch());
		this.deltaYaw = Math.abs(Utilities.computeAngleDifference(to.getYaw(), from.getYaw()));

		// Account for standing on boat
		if (Utilities.couldBeOnBoat(player, 0.25, true) && !Utilities.isSubmersed(player)) {
			this.onGround = true;
		}

		// Handle 1.9+ potion effects
		if (MinecraftVersion.atOrAbove(MinecraftVersion.COMBAT_UPDATE)) {
			for (PotionEffect effect : player.getActivePotionEffects()) {
				if (!VersionLib.isLevitationEffect(effect)) {
					continue;
				}
				AntiCheatRevolutions.getManager().getBackend().logLevitating(player, 1);
			}
		}

		this.touchedGroundThisTick = false;
		this.halfMovement = false;
		if (!this.onGround) {
			this.groundTicks = 0;
			this.airTicks++;
		} else {
			if (this.airTicks > 0) {
				this.touchedGroundThisTick = true;
			}
			this.airTicksBeforeGrounded = this.airTicks;
			this.airTicks = 0;
			this.groundTicks++;
		}

		if (Utilities.couldBeOnIce(to)) {
			this.iceTicks++;
			this.iceInfluenceTicks = 60;
		} else {
			this.iceTicks = 0;
			if (this.iceInfluenceTicks > 0) {
				this.iceInfluenceTicks--;
			}
		}

		if (Utilities.couldBeOnSlime(to)) {
			this.slimeTicks++;
			this.slimeInfluenceTicks = 40;
		} else {
			this.slimeTicks = 0;
			if (this.slimeInfluenceTicks > 0) {
				this.slimeInfluenceTicks--;
			}
		}
		
		if (Utilities.couldBeOnSoil(to)) {
			this.soilInfluenceTicks = 6;
		} else {
			if (this.soilInfluenceTicks > 0) {
				this.soilInfluenceTicks--;
			}
		}

		if (VersionLib.isGliding(player)) {
			this.elytraEffectTicks = 30;
		} else {
			if (this.elytraEffectTicks > 0) {
				this.elytraEffectTicks--;
			}
		}

		if (player.isSneaking()) {
			this.sneakingTicks++;
		} else {
			this.sneakingTicks = 0;
		}

		if (player.isBlocking()) {
			this.blockingTicks++;
		} else {
			this.blockingTicks = 0;
		}

		if (Utilities.isNearWater(player)) {
			this.nearLiquidTicks = 8;
		} else {
			if (this.nearLiquidTicks > 0) {
				this.nearLiquidTicks--;
			} else {
				this.nearLiquidTicks = 0;
			}
		}

		if (VersionLib.isRiptiding(player)) {
			this.riptideTicks = 30;
		} else {
			if (this.riptideTicks > 0) {
				this.riptideTicks--;
			} else {
				this.riptideTicks = 0;
			}
		}

		this.hadSpeedEffect = this.hasSpeedEffect;
		this.hasSpeedEffect = player.hasPotionEffect(PotionEffectType.SPEED);

		final double lastDistanceSq = Math.sqrt(this.lastDistance.getXDifference() * this.lastDistance.getXDifference()
				+ this.lastDistance.getZDifference() * this.lastDistance.getZDifference());
		final double currentDistanceSq = Math.sqrt(distance.getXDifference() * distance.getXDifference()
				+ distance.getZDifference() * distance.getZDifference());
		this.lastAcceleration = this.acceleration;
		this.acceleration = currentDistanceSq - lastDistanceSq;

		this.lastMotionY = this.motionY;
		this.motionY = to.getY() - from.getY();

		final Location top = to.clone().add(0, 2, 0);
		this.topSolid = top.getBlock().getType().isSolid();
		final Location bottom = to.clone().add(0, -1, 0);
		this.bottomSolid = bottom.getBlock().getType().isSolid();

		if ((this.motionY >= 0.42f && this.motionY <= 0.5625f)
				&& (Utilities.couldBeOnHalfblock(to) || Utilities.isNearBed(to))) {
			this.halfMovement = true;
			this.halfMovementHistoryCounter = 30;
		} else {
			if (this.halfMovementHistoryCounter > 0) {
				this.halfMovementHistoryCounter--;
			}
		}

		this.hadBoxedIn = this.hasBoxedIn;
		this.hasBoxedIn = this.isTopSolid() && this.isBottomSolid();

		this.lastUpdate = System.currentTimeMillis();
		
		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(player.getUniqueId());
		// Tick velocity tracker
		user.getVelocityTracker().tick();
		// Update "good location"
		user.setGoodLocation(from);
	}

}
