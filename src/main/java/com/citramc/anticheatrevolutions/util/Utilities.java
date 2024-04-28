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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import com.cryptomorin.xseries.XMaterial;

public final class Utilities {
	private static final List<Material> INSTANT_BREAK = new ArrayList<Material>();
	private static final List<Material> FOOD = new ArrayList<Material>();
	private static final List<Material> CLIMBABLE = new ArrayList<Material>();
	private static final Map<Material, Material> COMBO = new HashMap<Material, Material>();

	public static final double JUMP_MOTION_Y = 0.41999998688697815;

	/**
	 * Check if the block beneath is standable (only air check currently)
	 * 
	 * @param block the block to check underneath
	 * @return true if the block beneath is not standable (i.e., is air)
	 */
	public static boolean cantStandAtSingle(final Block block) {
		// Get the block directly beneath the current block
		Block beneathBlock = block.getRelative(BlockFace.DOWN);
		// Check if the block beneath is air
		return beneathBlock.getType() == Material.AIR;
	}

	/**
	 * Determine whether a player cannot stand on or around the given block.
	 * This method checks the standability of the block itself, immediately
	 * surrounding blocks,
	 * and blocks slightly further away to ensure a player truly cannot stand here.
	 *
	 * @param block the block to check
	 * @return true if the player should be unable to stand here based on several
	 *         checks
	 */
	public static boolean cantStandAt(final Block block) {
		// Check if the block itself is not standable
		boolean standable = canStand(block);
		// If standable, no need to check further
		if (standable) {
			return false;
		}
		// Check immediate surroundings and farther areas only if the block itself is
		// not standable
		boolean closeStandable = cantStandClose(block);
		boolean farStandable = cantStandFar(block);
		return closeStandable && farStandable;
	}

	/**
	 * Determine whether a player should be unable to stand at a given location
	 * by checking a slightly lower position than the actual location. This method
	 * adjusts the Y-coordinate by a small amount to ensure the check is accurate
	 * for the block directly under the player's feet, which helps in cases where
	 * the player might be on the edge of the block.
	 *
	 * @param location the location to check
	 * @return true if the player should be unable to stand here
	 */
	public static boolean cantStandAtExp(final Location location) {
		// Adjust the Y-coordinate slightly to ensure the block checked is where the
		// player's feet would be
		Location adjustedLocation = new Location(location.getWorld(), location.getX(), location.getY() - 0.01D,
				location.getZ());
		return cantStandAt(adjustedLocation.getBlock());
	}

	/**
	 * Determine whether a player cannot stand in the immediate vicinity of the
	 * given block.
	 * This checks the standability of blocks directly adjacent to the given block
	 * in all four
	 * cardinal directions (North, East, South, and West). The player cannot stand
	 * close if all
	 * adjacent blocks are not standable.
	 *
	 * @param block the block to check around
	 * @return true if a player cannot stand in any of the adjacent blocks
	 */
	public static boolean cantStandClose(final Block block) {
		// Check each cardinal direction; if any block is standable, return false
		return !canStand(block.getRelative(BlockFace.NORTH)) &&
				!canStand(block.getRelative(BlockFace.EAST)) &&
				!canStand(block.getRelative(BlockFace.SOUTH)) &&
				!canStand(block.getRelative(BlockFace.WEST));
	}

	/**
	 * Determine whether a player cannot stand on the block's outer surroundings.
	 * This method checks the blocks in all diagonal directions from the given
	 * block:
	 * North-West, North-East, South-West, and South-East. The player cannot stand
	 * far
	 * if all these blocks are not standable.
	 *
	 * @param block the block to check around
	 * @return true if a player cannot stand in any of the diagonally adjacent
	 *         blocks
	 */
	public static boolean cantStandFar(final Block block) {
		// Check each diagonal direction; if any block is standable, return false
		return !canStand(block.getRelative(BlockFace.NORTH_WEST)) &&
				!canStand(block.getRelative(BlockFace.NORTH_EAST)) &&
				!canStand(block.getRelative(BlockFace.SOUTH_WEST)) &&
				!canStand(block.getRelative(BlockFace.SOUTH_EAST));
	}

	/**
	 * Determine whether a block is standable.
	 * A block is considered standable if it is solid, meaning it's not a liquid
	 * like water or lava,
	 * and it's not air. This method checks if the block meets these criteria.
	 *
	 * @param block the block to check
	 * @return true if the block is standable, false otherwise
	 */
	public static boolean canStand(final Block block) {
		// Check if the block is neither a liquid nor air
		return !(block.isLiquid() || block.getType() == Material.AIR);
	}

	/**
	 * Determine whether none of the adjacent blocks, including the block directly
	 * below,
	 * are of type slime. This method helps in ensuring that the surroundings of a
	 * given block
	 * do not contain any slime blocks, which might affect gameplay mechanics like
	 * movement.
	 *
	 * @param block the block to check around
	 * @return true if no adjacent blocks are slime blocks, false otherwise
	 */
	public static boolean isNotNearSlime(final Block block) {
		BlockFace[] facesToCheck = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN };
		for (BlockFace face : facesToCheck) {
			if (isSlime(block.getRelative(face))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determine whether a block is a slime block. This method utilizes the
	 * XMaterial
	 * enumeration to provide cross-version compatibility, ensuring that the slime
	 * block
	 * is correctly identified regardless of server version changes or
	 * discrepancies.
	 *
	 * @param block the block to check
	 * @return true if the block is a slime block, false otherwise
	 */
	public static boolean isSlime(final Block block) {
		final Material slimeMaterial = XMaterial.SLIME_BLOCK.parseMaterial();
		return block.getType() == slimeMaterial;
	}

	/**
	 * Check if the player could potentially be on a boat. This method is a
	 * simplified
	 * version that uses default parameters suitable for most checks.
	 *
	 * @param player the player to check
	 * @return true if the player could be on a boat, false otherwise
	 */
	public static boolean couldBeOnBoat(final Player player) {
		return couldBeOnBoat(player, 0.35, false);
	}

	/**
	 * Check if the player could potentially be on a boat considering a specified
	 * range
	 * and whether to include a Y-coordinate check. This method checks all nearby
	 * entities
	 * within a cube defined by the range parameter to see if any are boats that the
	 * player
	 * could be standing on or near.
	 *
	 * @param player the player to check
	 * @param range  the range within which to look for boats
	 * @param checkY if true, checks the boat's Y-coordinate to be below the
	 *               player's
	 * @return true if a boat is within the range and meets the Y-coordinate
	 *         condition (if checked)
	 */
	public static boolean couldBeOnBoat(final Player player, final double range, final boolean checkY) {
		Location playerLocation = player.getLocation();
		double playerY = playerLocation.getY();
		List<Entity> nearbyEntities = player.getNearbyEntities(range, range, range);
		for (Entity entity : nearbyEntities) {
			if (entity instanceof Boat) {
				double boatY = entity.getLocation().getY();
				if (!checkY || boatY < playerY + 0.35) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check if the player could potentially be standing on ice by checking multiple
	 * positions just below the player's current Y-coordinate. This method helps
	 * determine ice presence in cases where the player might be on the edge of a
	 * block
	 * or standing directly on ice.
	 *
	 * @param location the player's current location
	 * @return true if any of the checked locations indicate the presence of ice,
	 *         false otherwise
	 */
	public static boolean couldBeOnIce(final Location location) {
		double[] yAdjustments = { -0.01, -0.26, -0.51 }; // Positions to check relative to the player's current Y
		World world = location.getWorld();
		double x = location.getX();
		double z = location.getZ();

		for (double adjustment : yAdjustments) {
			Location adjustedLocation = new Location(world, x, location.getY() + adjustment, z);
			if (isNearIce(adjustedLocation)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether the block at a specified location is made of any type of
	 * ice.
	 * This method checks if the material of the block ends with "ICE", allowing it
	 * to
	 * identify various ice types such as regular ice, packed ice, or blue ice. It
	 * uses
	 * a predicate to perform this check efficiently across different versions of
	 * Minecraft.
	 *
	 * @param location the location to check
	 * @return true if the block at the location is any type of ice, false otherwise
	 */
	public static boolean isNearIce(final Location location) {
		return isCollisionPoint(location, material -> material.name().toUpperCase().endsWith("ICE"));
	}

	/**
	 * Determine whether there is a shulker box near the given location. This method
	 * checks
	 * if any blocks around the specified location are shulker boxes, which are
	 * available
	 * starting from Minecraft version 1.11. The method ensures that the check is
	 * only performed
	 * in versions that support Shulker Boxes, enhancing compatibility and
	 * preventing errors
	 * in earlier versions.
	 *
	 * @param location the location to check around
	 * @return true if a shulker box is near the specified location, false otherwise
	 */
	public static boolean isNearShulkerBox(final Location location) {
		// Check for Shulker Boxes in Minecraft versions 1.11 or newer
		if (MinecraftVersion.getCurrentVersion().isAtLeast(MinecraftVersion.EXPLORATION_UPDATE)) {
			return isCollisionPoint(location, material -> material.name().toUpperCase().endsWith("SHULKER_BOX"));
		}
		return false;
	}

	/**
	 * Determine whether a player could potentially be standing on a half-block by
	 * checking
	 * multiple positions just below the player's current Y-coordinate. This method
	 * helps
	 * determine half-block presence in cases where the player might be on the edge
	 * of a block
	 * or standing directly on a half-block like slabs or stairs.
	 *
	 * @param location the player's current location
	 * @return true if any of the checked locations indicate the presence of a
	 *         half-block, false otherwise
	 */
	public static boolean couldBeOnHalfblock(final Location location) {
		double[] yAdjustments = { -0.01, -0.51 }; // Y adjustments to check for half-blocks slightly below the actual
													// position
		for (double adjustment : yAdjustments) {
			Location adjustedLocation = location.clone().add(0, adjustment, 0);
			if (isNearHalfblock(adjustedLocation)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a block or any of its immediate neighbors is a halfblock,
	 * such as a slab or stair. This method checks the block at the given location
	 * and its surrounding blocks in all cardinal and intercardinal directions.
	 *
	 * @param location the location to check
	 * @return true if the block or any adjacent block is a halfblock, false
	 *         otherwise
	 */
	public static boolean isNearHalfblock(final Location location) {
		BlockFace[] facesToCheck = {
				BlockFace.SELF,
				BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
				BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST
		};
		Block centerBlock = location.getBlock();
		for (BlockFace face : facesToCheck) {
			if (isHalfblock(centerBlock.getRelative(face))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if a given block is considered a half-block, such as slabs, stairs,
	 * walls, or other similar types. This method supports both newer and older
	 * versions
	 * of Minecraft by checking bounding box dimensions where available and falling
	 * back
	 * to type checks in earlier versions.
	 *
	 * @param block the block to evaluate
	 * @return true if the block is a half-block, false otherwise
	 */
	public static boolean isHalfblock(final Block block) {
		if (MinecraftVersion.getCurrentVersion().isAtLeast(MinecraftVersion.VILLAGE_UPDATE)) {
			final BoundingBox box = block.getBoundingBox();
			final double height = box.getMaxY() - box.getMinY();
			if( height > 0.42 && height <= 0.6 && block.getType().isSolid())
			{
				return true;
			}
		}
		return isSlab(block) || isStair(block) || isWall(block) || block.getType() == XMaterial.SNOW.parseMaterial()
				|| block.getType().name().endsWith("HEAD");
	}

	/**
	 * Determine whether a player could potentially be standing on a slime block by
	 * checking
	 * several Y-coordinates just below the player's current location. This
	 * accommodates edge cases
	 * where the player might be on the edge or just above a slime block.
	 *
	 * @param location the player's current location
	 * @return true if any checked location contains a slime block, false otherwise
	 */
	public static boolean couldBeOnSlime(final Location location) {
		double[] offsets = { -0.01, -0.51 }; // Check just above and half a block below
		for (double offset : offsets) {
			if (isNearSlime(location.clone().add(0, offset, 0))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the block at the specified location or any blocks immediately
	 * surrounding it
	 * are slime blocks. This method uses a generic collision check to determine if
	 * any of the blocks
	 * meet the criteria for being a slime block, important for game mechanics
	 * involving bouncing.
	 *
	 * @param location the location to check around
	 * @return true if the location or adjacent blocks are slime blocks, false
	 *         otherwise
	 */
	public static boolean isNearSlime(final Location location) {
		return isCollisionPoint(location, material -> material == XMaterial.SLIME_BLOCK.parseMaterial());
	}

	/**
	 * Determine whether a player could potentially be standing on soil by checking
	 * at slightly adjusted Y-coordinates below the player's current location. This
	 * helps
	 * ensure that the detection covers cases where the player might be on the edge
	 * of the soil.
	 *
	 * @param location the player's current location
	 * @return true if soil is detected below at any of the checked positions, false
	 *         otherwise
	 */
	public static boolean couldBeOnSoil(final Location location) {
		double[] offsets = { -0.01, -0.51 }; // Check just above and half a block below
		for (double offset : offsets) {
			if (isNearSoil(location.clone().add(0, offset, 0))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the block at the specified location or any blocks immediately
	 * surrounding it
	 * are soil types, specifically Soul Soil or Soul Sand. This method is crucial
	 * for gameplay
	 * mechanics involving soil, such as plant growth or specific mob interactions.
	 *
	 * @param location the location to check around
	 * @return true if the location or adjacent blocks are soil types, false
	 *         otherwise
	 */
	public static boolean isNearSoil(final Location location) {
		return isCollisionPoint(location, material -> material == XMaterial.SOUL_SOIL.parseMaterial()
				|| material == XMaterial.SOUL_SAND.parseMaterial());
	}

	/**
	 * Determine if a player is fully submerged in water. This method checks both
	 * the player's current and adjusted Y-levels to ensure they are completely in
	 * water,
	 * taking into account potential edge cases near the water surface.
	 *
	 * @param player the player's location
	 * @return true if the player is fully submerged in water, false otherwise
	 */
	public static boolean isFullyInWater(Location player) {
		double touchedX = fixXAxis(player.getX());
		Location baseLocation = new Location(player.getWorld(), touchedX, player.getY(), player.getBlockZ());
		Location roundedLocation = new Location(player.getWorld(), touchedX, Math.round(player.getY()),
				player.getBlockZ());

		return baseLocation.getBlock().isLiquid() && roundedLocation.getBlock().isLiquid();
	}

	/**
	 * Adjusts the player's X-axis position to accurately reflect the block
	 * position,
	 * particularly useful when the player is near the edge of a block. This method
	 * ensures that the player's X coordinate is adjusted to the nearest whole
	 * number
	 * block position, reducing errors in position-sensitive calculations.
	 *
	 * @param x the original X-axis value
	 * @return the adjusted X-axis value
	 */
	public static double fixXAxis(double x) {
		// Adjust x to be the nearest block boundary by rounding to the nearest whole
		// number
		return Math.round(x);
	}

	/**
	 * Determine if a player is hovering over water within a specified number of
	 * blocks below them.
	 * This method checks vertically downwards until it either hits water or reaches
	 * the limit of
	 * the specified block depth, which is useful for determining interactions like
	 * fishing or fall
	 * damage prevention.
	 *
	 * @param player the player's location
	 * @param blocks the maximum number of blocks to check below the player
	 * @return true if there is water directly below the player within the specified
	 *         range, false otherwise
	 */
	public static boolean isHoveringOverWater(Location player, int blocks) {
		int startY = player.getBlockY();
		World world = player.getWorld();
		int x = player.getBlockX();
		int z = player.getBlockZ();

		for (int y = startY; y > startY - blocks; y--) {
			Material type = new Location(world, x, y, z).getBlock().getType();
			if (type != Material.AIR) { // If it's not air, check if it's water
				return type.name().toUpperCase().endsWith("WATER");
			}
		}
		return false;
	}

	/**
	 * Determine if the player is hovering over water with a hard limit of 25 blocks
	 *
	 * @param player the player's location
	 * @return true if the player is hovering over water
	 */
	public static boolean isHoveringOverWater(Location player) {
		return isHoveringOverWater(player, 25);
	}

	/**
	 * Determine whether a material will break instantly when hit
	 *
	 * @param m the material to check
	 * @return true if the material is instant break
	 */
	public static boolean isInstantBreak(Material m) {
		return INSTANT_BREAK.contains(m);
	}

	/**
	 * Determine whether a material is edible
	 *
	 * @param m the material to check
	 * @return true if the material is food
	 */
	public static boolean isFood(Material m) {
		return FOOD.contains(m);
	}

	// General helper method for checking material names
	private static boolean endsWithAny(Material material, String... suffixes) {
		String typeName = material.name().toUpperCase();
		for (String suffix : suffixes) {
			if (typeName.endsWith(suffix.toUpperCase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a block is a slab
	 *
	 * @param block block to check
	 * @return true if slab
	 */
	public static boolean isSlab(final Block block) {
		return endsWithAny(block.getType(), "SLAB", "STEP");
	}

	public static boolean isNearBed(final Location location) {
		return isCollisionPoint(location, material -> endsWithAny(material, "BED"));
	}

	/**
	 * Determine whether a block is a stair
	 *
	 * @param block block to check
	 * @return true if stair
	 */
	public static boolean isStair(final Block block) {
		return endsWithAny(block.getType(), "STAIRS");
	}

	/**
	 * Determine whether a block is a wall
	 *
	 * @param block block to check
	 * @return true if wall
	 */
	public static boolean isWall(final Block block) {
		return endsWithAny(block.getType(), "WALL", "FENCE", "FENCE_GATE");
	}

	/**
	 * Determine if any wall, fence, or fence gate is near the specified location.
	 * This method
	 * checks the block at the location and its immediate neighbors in all cardinal
	 * and intercardinal
	 * directions for wall-like structures.
	 *
	 * @param location the location to check around
	 * @return true if a wall-like structure is near the location, false otherwise
	 */
	public static boolean isNearWall(final Location location) {
		BlockFace[] facesToCheck = {
				BlockFace.SELF,
				BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
				BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST
		};
		for (BlockFace face : facesToCheck) {
			if (isWall(location.getBlock().getRelative(face))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if a player is standing on a lily pad or any adjacent blocks are
	 * lily pads.
	 * This is used to check environmental interactions such as fishing or movement
	 * across water.
	 *
	 * @param player the player whose location to check
	 * @return true if the player is on or immediately next to a lily pad, false
	 *         otherwise
	 */
	public static boolean isOnLilyPad(final Player player) {
		BlockFace[] facesToCheck = {
				BlockFace.SELF,
				BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
		};
		Block centerBlock = player.getLocation().getBlock();
		for (BlockFace face : facesToCheck) {
			if (centerBlock.getRelative(face).getType() == XMaterial.LILY_PAD.parseMaterial()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if a player is standing on a carpet or immediately next to one in
	 * any cardinal direction.
	 * This method is crucial for gameplay features that depend on specific floor
	 * types, such as carpeted areas
	 * affecting movement speed or aesthetics.
	 *
	 * @param player the player whose location to check
	 * @return true if the player is on or next to a carpet, false otherwise
	 */
	public static boolean isOnCarpet(final Player player) {
		BlockFace[] facesToCheck = {
				BlockFace.SELF,
				BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
		};
		Block centerBlock = player.getLocation().getBlock();
		for (BlockFace face : facesToCheck) {
			if (centerBlock.getRelative(face).getType().name().endsWith("CARPET")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if a player is fully submerged in liquid, which is crucial for
	 * determining
	 * whether a player is swimming or under the effect of being underwater. This
	 * method checks
	 * the block at the player's location and the block directly above to verify
	 * both are liquids,
	 * indicating full submersion.
	 *
	 * @param player the player whose submersion status to check
	 * @return true if the player is fully submerged in a liquid, false otherwise
	 */
	public static boolean isSubmersed(final Player player) {
		Block currentBlock = player.getLocation().getBlock();
		Block blockAbove = currentBlock.getRelative(BlockFace.UP);
		// Check if both the current block and the block above are liquid
		return currentBlock.isLiquid() && blockAbove.isLiquid();
	}

	/**
	 * Determine if a player is in water, considering both the current and adjacent
	 * vertical blocks.
	 * This method also checks for kelp, which is part of the newer water mechanics
	 * introduced in
	 * Minecraft 1.13 (Aquatic Update).
	 *
	 * @param player the player whose water immersion status to check
	 * @return true if the player is in water or affected by kelp, false otherwise
	 */
	public static boolean isInWater(Player player) {
		Block currentBlock = player.getLocation().getBlock();
		boolean isNearWater = currentBlock.isLiquid() ||
				currentBlock.getRelative(BlockFace.UP).isLiquid() ||
				currentBlock.getRelative(BlockFace.DOWN).isLiquid();

		if (!isNearWater && MinecraftVersion.getCurrentVersion().isAtLeast(MinecraftVersion.AQUATIC_UPDATE)) {
			isNearWater = isNearKelp(currentBlock); // Check for kelp only if no water is detected and version is 1.13+
		}

		return isNearWater;
	}

	private static boolean isNearKelp(Block block) {
		return block.getType() == XMaterial.KELP_PLANT.parseMaterial() ||
				block.getRelative(BlockFace.UP).getType() == XMaterial.KELP_PLANT.parseMaterial() ||
				block.getRelative(BlockFace.DOWN).getType() == XMaterial.KELP_PLANT.parseMaterial();
	}

	/**
	 * Determine whether a player is near a liquid block
	 *
	 * @param player player to check
	 * @return true if near liquid block
	 */
	public static boolean isNearWater(Player player) {
		return isNearWater(player.getLocation());
	}

	/**
	 * Determine if water is present at or near the specified location. This method
	 * checks
	 * the block itself and all surrounding blocks including those at the same
	 * level, above, below,
	 * and diagonally adjacent. It's useful for interactions that depend on water
	 * presence, such
	 * as crop hydration or player movement effects.
	 *
	 * @param location the location to check for nearby water
	 * @return true if water is present in any of the checked locations, false
	 *         otherwise
	 */
	public static boolean isNearWater(Location location) {
		BlockFace[] facesToCheck = {
				BlockFace.SELF,
				BlockFace.UP, BlockFace.DOWN,
				BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
				BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST
		};
		Block centerBlock = location.getBlock();
		for (BlockFace face : facesToCheck) {
			if (centerBlock.getRelative(face).isLiquid()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if a player is entirely surrounded by water. This method checks all
	 * horizontal
	 * and diagonal directions around the player's current block to ensure they are
	 * fully enclosed
	 * by water, crucial for gameplay mechanics like underwater breathing or
	 * movement.
	 *
	 * @param player the player whose surrounding to check
	 * @return true if all adjacent blocks around the player are water, false
	 *         otherwise
	 */
	public static boolean isSurroundedByWater(Player player) {
		Location location = player.getLocation().clone().subtract(0, 0.1, 0);
		BlockFace[] facesToCheck = {
				BlockFace.SELF,
				BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
				BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST
		};
		Block centerBlock = location.getBlock();
		for (BlockFace face : facesToCheck) {
			if (!centerBlock.getRelative(face).isLiquid()) {
				return false; // If any of the checked blocks is not water, return false immediately
			}
		}
		return true; // Only return true if all checked blocks are water
	}

	/**
	 * Determine whether a player is near a web
	 *
	 * @param player player to check
	 * @return true if near a web
	 */
	public static boolean isNearWeb(final Player player) {
		return isCollisionPoint(player.getLocation(), material -> material == XMaterial.COBWEB.parseMaterial());
	}

	/**
	 * Determine if a player is in a web, affecting their movement. This method
	 * checks the block
	 * at the player's current location as well as directly above and below to see
	 * if any are cobwebs,
	 * crucial for gameplay mechanics that handle movement restriction.
	 *
	 * @param player the player whose web status to check
	 * @return true if the player is in a cobweb, false otherwise
	 */
	public static boolean isInWeb(Player player) {
		BlockFace[] relevantFaces = { BlockFace.SELF, BlockFace.UP, BlockFace.DOWN };
		Block currentBlock = player.getLocation().getBlock();
		for (BlockFace face : relevantFaces) {
			if (currentBlock.getRelative(face).getType() == XMaterial.COBWEB.parseMaterial()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a player is near a climbable block
	 *
	 * @param player player to check
	 * @return true if near climbable block
	 */
	public static boolean isNearClimbable(final Player player) {
		return isCollisionPoint(player.getLocation(), material -> CLIMBABLE.contains(material));
	}

	/**
	 * Determine whether a location is near a climbable block
	 *
	 * @param location location to check
	 * @return true if near climbable block
	 */
	public static boolean isNearClimbable(final Location location) {
		return isCollisionPoint(location, material -> CLIMBABLE.contains(material));
	}

	/**
	 * Determine whether a location is near a climbable block
	 *
	 * @param location location to check
	 * @param expand   expansion factor
	 * @return true if near climbable block
	 */
	public static boolean isNearClimbable(final Location location, final double expand) {
		return isCollisionPoint(location, expand, material -> CLIMBABLE.contains(material));
	}

	/**
	 * Determine whether a block is climbable
	 *
	 * @param block block to check
	 * @return true if climbable
	 */
	public static boolean isClimbableBlock(final Block block) {
		return CLIMBABLE.contains(block.getType());
	}

	/**
	 * Determine whether a player is on a vine (can be free hanging)
	 *
	 * @param player to check
	 * @return true if on vine
	 */
	public static boolean isOnVine(final Player player) {
		return player.getLocation().getBlock().getType() == XMaterial.VINE.parseMaterial();
	}

	/**
	 * Checks if a string can be parsed as an integer without throwing an exception.
	 *
	 * @param string the string to check
	 * @return true if the string is a valid integer format, false otherwise
	 */
	public static boolean isInt(String string) {
		if (string == null) {
			return false;
		}
		return string.matches("-?\\d+"); // Matches a number with optional '-' and digits
	}

	/**
	 * Checks if a string can be parsed as a double without throwing an exception.
	 *
	 * @param string the string to check
	 * @return true if the string is a valid double format, false otherwise
	 */
	public static boolean isDouble(String string) {
		if (string == null) {
			return false;
		}
		return string.matches("-?\\d+(\\.\\d+)?"); // Matches a number with optional '-' and decimals
	}

	/**
	 * Determines if the type of a block does not match any materials in the given
	 * array.
	 *
	 * @param block     the block to check
	 * @param materials array of materials to compare against
	 * @return true if the block type is not any of the given materials, false
	 *         otherwise
	 */
	public static boolean blockIsnt(Block block, Material[] materials) {
		Material type = block.getType();
		return Arrays.stream(materials).noneMatch(material -> material == type);
	}

	/**
	 * Determines if the block type's name does not end with any of the specified
	 * strings.
	 *
	 * @param block    the block to check
	 * @param endTypes array of string suffixes to check against
	 * @return true if the block type's name does not end with any of the given
	 *         suffixes, false otherwise
	 */
	public static boolean blockIsnt(Block block, String[] endTypes) {
		String typeName = block.getType().name();
		return Arrays.stream(endTypes).noneMatch(typeName::endsWith);
	}

	/**
	 * Parse a COMMAND[] input to a set of commands to execute
	 *
	 * @param command input string
	 * @return parsed commands
	 */
	public static String[] getCommands(String command) {
		return command.replaceAll("COMMAND\\[", "").replaceAll("]", "").split(";");
	}

	/**
	 * Remove all whitespace from the given string to ready it for parsing
	 *
	 * @param string the string to parse
	 * @return string with whitespace removed
	 */
	public static String removeWhitespace(String string) {
		return string.replaceAll(" ", "");
	}

	/**
	 * Checks if any piece of a player's armor is enchanted with the specified
	 * enchantment.
	 * This is useful for determining if a player has specific enhancements that
	 * affect gameplay,
	 * such as increased durability, damage reduction, or special abilities like
	 * underwater breathing.
	 *
	 * @param player      the player whose armor to check
	 * @param enchantment the enchantment to look for
	 * @return true if any armor piece has the specified enchantment, false
	 *         otherwise
	 */
	public static boolean hasArmorEnchantment(Player player, Enchantment enchantment) {
		return Arrays.stream(player.getInventory().getArmorContents())
				.filter(Objects::nonNull) // Filter out null items
				.anyMatch(item -> item.containsEnchantment(enchantment));
	}

	/**
	 * Create a list with the given string for execution
	 *
	 * @param string the string to parse
	 * @return ArrayList with string
	 */
	public static ArrayList<String> stringToList(final String string) {
		return new ArrayList<String>() {
			private static final long serialVersionUID = 364115444874638230L;
			{
				add(string);
			}
		};
	}

	/**
	 * Converts a list of strings into a single comma-separated string. This method
	 * is useful
	 * for creating compact, human-readable representations of collections for
	 * display or logging.
	 *
	 * @param list the list of strings to convert
	 * @return a comma-separated string representing all elements of the list
	 */
	public static String listToCommaString(List<String> list) {
		return String.join(",", list);
	}

	/**
	 * Converts a formatted duration string into the total number of seconds.
	 * The input string is expected to be in the format "XdXhXmXs" (e.g.,
	 * "2d3h4m5s"),
	 * where X can be any number representing the quantity of days, hours, minutes,
	 * or seconds.
	 *
	 * @param duration the formatted duration string
	 * @return the total number of seconds represented by the input string
	 */
	public static long lifeToSeconds(String duration) {
		if (duration == null || duration.isEmpty() || duration.equals("0")) {
			return 0;
		}

		long seconds = 0;
		Matcher matcher = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?").matcher(duration);
		if (matcher.matches()) {
			if (matcher.group(1) != null)
				seconds += Integer.parseInt(matcher.group(1)) * 86400;
			if (matcher.group(2) != null)
				seconds += Integer.parseInt(matcher.group(2)) * 3600;
			if (matcher.group(3) != null)
				seconds += Integer.parseInt(matcher.group(3)) * 60;
			if (matcher.group(4) != null)
				seconds += Integer.parseInt(matcher.group(4));
		}

		return seconds;
	}

	/**
	 * Rounds a float value to a scale
	 * 
	 * @param value Value to round
	 * @param scale Scale
	 * @return rounded value
	 */
	public static float roundFloat(float value, int scale) {
		return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).floatValue();
	}

	/**
	 * Rounds a double value to a scale
	 * 
	 * @param value Value to round
	 * @param scale Scale
	 * @return rounded value
	 */
	public static double roundDouble(double value, int scale) {
		return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
	}

	public static int floor(double value) {
		int rounded = (int) value;
		return value < rounded ? rounded - 1 : rounded;
	}

	/**
	 * Computes the minimum difference in degrees between two angles. This method
	 * handles angle wrap-around at 360 degrees to ensure the shortest angular
	 * difference
	 * is always returned, which is particularly useful for applications needing
	 * precise
	 * angle comparisons such as in navigational algorithms or game mechanics.
	 *
	 * @param a the first angle in degrees
	 * @param b the second angle in degrees
	 * @return the minimum angle difference in degrees
	 */
	public static float computeAngleDifference(final float a, final float b) {
		float diff = Math.abs(a - b) % 360; // Direct difference modulo 360
		return diff > 180 ? 360 - diff : diff; // If the difference is greater than 180 degrees, take the shorter route
	}

	/**
	 * Calculates the greatest common divider
	 * 
	 * @param current  - The current value
	 * @param previous - The previous value
	 * @return - The GCD of those two values
	 */
	public static long getGcd(final long current, final long previous) {
		return (previous <= 16384L) ? current : getGcd(previous, current % previous);
	}

	public static boolean isCollisionPoint(final Location location, final Predicate<Material> predicate) {
		return isCollisionPoint(location, 0.3, predicate);
	}

	public static boolean isCollisionPoint(final Location location, final double expand,
			final Predicate<Material> predicate) {
		final ArrayList<Material> materials = new ArrayList<>();
		for (double x = -expand; x <= expand; x += expand) {
			for (double y = -expand; y <= expand; y += expand) {
				for (double z = -expand; z <= expand; z += expand) {
					final Material material = location.clone().add(x, y, z).getBlock().getType();
					if (material != null) {
						materials.add(material);
					}
				}
			}
		}
		return materials.stream().anyMatch(predicate);
	}

	/**
	 * @author Elevated
	 * @param data - The set of data you want to find the variance from
	 * @return - The variance of the numbers.
	 *
	 * @See - https://en.wikipedia.org/wiki/Variance
	 */
	public static double getVariance(final Collection<? extends Number> data) {
		int count = 0;

		double sum = 0.0;
		double variance = 0.0;

		double average;

		// Increase the sum and the count to find the average and the standard deviation
		for (final Number number : data) {
			sum += number.doubleValue();
			++count;
		}

		average = sum / count;

		// Run the standard deviation formula
		for (final Number number : data) {
			variance += Math.pow(number.doubleValue() - average, 2.0);
		}

		return variance;
	}

	/**
	 * Gets the allowed player movement speed through the "GENERIC_MOVEMENT_SPEED"
	 * attribute
	 * 
	 * @return the allowed player movement speed
	 */
	public static double getMovementSpeed(final Player player) {
		final org.bukkit.attribute.AttributeInstance attribute = player
				.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED);
		return attribute.getValue();
	}

	/**
	 * Returns the fractional part of the Y coordinate.
	 * 
	 * @param location The location from which to extract the Y fraction.
	 * @return Fractional part of the Y coordinate.
	 */
	public static double getYFraction(Location location) {
		return location.getY() - Math.floor(location.getY());
	}

	/**
	 * Checks if the block at the given location is solid.
	 * 
	 * @param location The location of the block to check.
	 * @return true if the block is solid, false otherwise.
	 */
	public static boolean isSolidBlock(Location location) {
		Block block = location.getBlock();
		return block.getType().isSolid();
	}

	static {
		MinecraftVersion currentVersion = MinecraftVersion.getCurrentVersion();

		// Start instant break materials
		INSTANT_BREAK.add(XMaterial.COMPARATOR.parseMaterial());
		INSTANT_BREAK.add(XMaterial.REPEATER.parseMaterial());
		INSTANT_BREAK.add(XMaterial.TORCH.parseMaterial());
		INSTANT_BREAK.add(XMaterial.REDSTONE_TORCH.parseMaterial());
		INSTANT_BREAK.add(XMaterial.REDSTONE_WIRE.parseMaterial());
		INSTANT_BREAK.add(XMaterial.TRIPWIRE.parseMaterial());
		INSTANT_BREAK.add(XMaterial.TRIPWIRE_HOOK.parseMaterial());
		INSTANT_BREAK.add(XMaterial.FIRE.parseMaterial());
		INSTANT_BREAK.add(XMaterial.FLOWER_POT.parseMaterial());
		INSTANT_BREAK.add(XMaterial.INFESTED_CHISELED_STONE_BRICKS.parseMaterial());
		INSTANT_BREAK.add(XMaterial.INFESTED_COBBLESTONE.parseMaterial());
		INSTANT_BREAK.add(XMaterial.INFESTED_CRACKED_STONE_BRICKS.parseMaterial());
		INSTANT_BREAK.add(XMaterial.INFESTED_MOSSY_STONE_BRICKS.parseMaterial());
		INSTANT_BREAK.add(XMaterial.INFESTED_STONE.parseMaterial());
		INSTANT_BREAK.add(XMaterial.INFESTED_STONE_BRICKS.parseMaterial());
		INSTANT_BREAK.add(XMaterial.TNT.parseMaterial());
		INSTANT_BREAK.add(XMaterial.SLIME_BLOCK.parseMaterial());
		INSTANT_BREAK.add(XMaterial.CARROTS.parseMaterial());
		INSTANT_BREAK.add(XMaterial.DEAD_BUSH.parseMaterial());
		INSTANT_BREAK.add(XMaterial.FERN.parseMaterial());
		INSTANT_BREAK.add(XMaterial.LARGE_FERN.parseMaterial());
		INSTANT_BREAK.add(XMaterial.CHORUS_FLOWER.parseMaterial());
		INSTANT_BREAK.add(XMaterial.SUNFLOWER.parseMaterial());
		INSTANT_BREAK.add(XMaterial.LILY_PAD.parseMaterial());
		INSTANT_BREAK.add(XMaterial.MELON_STEM.parseMaterial());
		INSTANT_BREAK.add(XMaterial.ATTACHED_MELON_STEM.parseMaterial());
		INSTANT_BREAK.add(XMaterial.BROWN_MUSHROOM.parseMaterial());
		INSTANT_BREAK.add(XMaterial.RED_MUSHROOM.parseMaterial());
		INSTANT_BREAK.add(XMaterial.NETHER_WART.parseMaterial());
		INSTANT_BREAK.add(XMaterial.POTATOES.parseMaterial());
		INSTANT_BREAK.add(XMaterial.PUMPKIN_STEM.parseMaterial());
		INSTANT_BREAK.add(XMaterial.ATTACHED_PUMPKIN_STEM.parseMaterial());
		INSTANT_BREAK.add(XMaterial.ACACIA_SAPLING.parseMaterial());
		INSTANT_BREAK.add(XMaterial.BIRCH_SAPLING.parseMaterial());
		INSTANT_BREAK.add(XMaterial.DARK_OAK_SAPLING.parseMaterial());
		INSTANT_BREAK.add(XMaterial.JUNGLE_SAPLING.parseMaterial());
		INSTANT_BREAK.add(XMaterial.OAK_SAPLING.parseMaterial());
		INSTANT_BREAK.add(XMaterial.SPRUCE_SAPLING.parseMaterial());
		INSTANT_BREAK.add(XMaterial.SUGAR_CANE.parseMaterial());
		INSTANT_BREAK.add(XMaterial.TALL_GRASS.parseMaterial());
		INSTANT_BREAK.add(XMaterial.TALL_SEAGRASS.parseMaterial());
		INSTANT_BREAK.add(XMaterial.WHEAT.parseMaterial());
		// Start 1.14 objects
		if (currentVersion.isAtLeast(MinecraftVersion.VILLAGE_UPDATE)) {
			INSTANT_BREAK.add(XMaterial.BAMBOO_SAPLING.parseMaterial());
			INSTANT_BREAK.add(XMaterial.CORNFLOWER.parseMaterial());
		}
		// End 1.14 objects
		// Start 1.15 objects
		if (currentVersion.isAtLeast(MinecraftVersion.BEE_UPDATE)) {
			INSTANT_BREAK.add(XMaterial.HONEY_BLOCK.parseMaterial());
		}
		// End 1.15 objects
		// End instant break materials

		// Start food
		FOOD.add(XMaterial.APPLE.parseMaterial());
		FOOD.add(XMaterial.BAKED_POTATO.parseMaterial());
		FOOD.add(XMaterial.BEETROOT.parseMaterial());
		FOOD.add(XMaterial.BEETROOT_SOUP.parseMaterial());
		FOOD.add(XMaterial.BREAD.parseMaterial());
		FOOD.add(XMaterial.CAKE.parseMaterial());
		FOOD.add(XMaterial.CARROT.parseMaterial());
		FOOD.add(XMaterial.CHORUS_FRUIT.parseMaterial());
		FOOD.add(XMaterial.COOKED_BEEF.parseMaterial());
		FOOD.add(XMaterial.COOKED_CHICKEN.parseMaterial());
		FOOD.add(XMaterial.COOKED_COD.parseMaterial());
		FOOD.add(XMaterial.COOKED_MUTTON.parseMaterial());
		FOOD.add(XMaterial.COOKED_PORKCHOP.parseMaterial());
		FOOD.add(XMaterial.COOKED_RABBIT.parseMaterial());
		FOOD.add(XMaterial.COOKED_SALMON.parseMaterial());
		FOOD.add(XMaterial.COOKIE.parseMaterial());
		FOOD.add(XMaterial.DRIED_KELP.parseMaterial());
		FOOD.add(XMaterial.ENCHANTED_GOLDEN_APPLE.parseMaterial());
		FOOD.add(XMaterial.GOLDEN_APPLE.parseMaterial());
		FOOD.add(XMaterial.GOLDEN_CARROT.parseMaterial());
		FOOD.add(XMaterial.MELON_SLICE.parseMaterial());
		FOOD.add(XMaterial.MUSHROOM_STEW.parseMaterial());
		FOOD.add(XMaterial.POISONOUS_POTATO.parseMaterial());
		FOOD.add(XMaterial.POTATO.parseMaterial());
		FOOD.add(XMaterial.PUFFERFISH.parseMaterial());
		FOOD.add(XMaterial.PUMPKIN_PIE.parseMaterial());
		FOOD.add(XMaterial.RABBIT_STEW.parseMaterial());
		FOOD.add(XMaterial.BEEF.parseMaterial());
		FOOD.add(XMaterial.CHICKEN.parseMaterial());
		FOOD.add(XMaterial.COD.parseMaterial());
		FOOD.add(XMaterial.MUTTON.parseMaterial());
		FOOD.add(XMaterial.PORKCHOP.parseMaterial());
		FOOD.add(XMaterial.RABBIT.parseMaterial());
		FOOD.add(XMaterial.SALMON.parseMaterial());
		FOOD.add(XMaterial.ROTTEN_FLESH.parseMaterial());
		FOOD.add(XMaterial.SPIDER_EYE.parseMaterial());
		FOOD.add(XMaterial.TROPICAL_FISH.parseMaterial());
		// Start 1.14 objects
		if (currentVersion.isAtLeast(MinecraftVersion.VILLAGE_UPDATE)) {
			FOOD.add(XMaterial.SUSPICIOUS_STEW.parseMaterial());
			FOOD.add(XMaterial.SWEET_BERRIES.parseMaterial());
		}
		// End 1.14 objects
		// Start 1.15 objects
		if (currentVersion.isAtLeast(MinecraftVersion.BEE_UPDATE)) {
			FOOD.add(XMaterial.HONEY_BOTTLE.parseMaterial());
		}
		// End 1.15 objects
		// End food

		// Start combos
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.BLACK_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.BLUE_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.BROWN_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.CYAN_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.GRAY_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.GREEN_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.LIGHT_BLUE_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.LIGHT_GRAY_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.LIME_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.MAGENTA_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.MAGENTA_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.ORANGE_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.PINK_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.PURPLE_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.RED_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.WHITE_WOOL.parseMaterial());
		COMBO.put(XMaterial.SHEARS.parseMaterial(), XMaterial.YELLOW_WOOL.parseMaterial());

		COMBO.put(XMaterial.IRON_SWORD.parseMaterial(), XMaterial.COBWEB.parseMaterial());
		COMBO.put(XMaterial.DIAMOND_SWORD.parseMaterial(), XMaterial.COBWEB.parseMaterial());
		COMBO.put(XMaterial.STONE_SWORD.parseMaterial(), XMaterial.COBWEB.parseMaterial());
		COMBO.put(XMaterial.WOODEN_SWORD.parseMaterial(), XMaterial.COBWEB.parseMaterial());
		// End combos

		// Start climbable
		CLIMBABLE.add(XMaterial.VINE.parseMaterial());
		CLIMBABLE.add(XMaterial.LADDER.parseMaterial());
		CLIMBABLE.add(XMaterial.WATER.parseMaterial());
		// Start 1.13 objects
		if (currentVersion.isAtLeast(MinecraftVersion.AQUATIC_UPDATE)) {
			CLIMBABLE.add(XMaterial.KELP.parseMaterial());
			CLIMBABLE.add(XMaterial.KELP_PLANT.parseMaterial());
		}
		// End 1.13 objects

		// Start 1.14 objects
		if (currentVersion.isAtLeast(MinecraftVersion.VILLAGE_UPDATE)) {
			CLIMBABLE.add(XMaterial.SCAFFOLDING.parseMaterial());
			CLIMBABLE.add(XMaterial.SWEET_BERRY_BUSH.parseMaterial());
		}
		// End 1.14 objects

		// Start 1.15 objects
		if (currentVersion.isAtLeast(MinecraftVersion.BEE_UPDATE)) {
			CLIMBABLE.add(XMaterial.HONEY_BLOCK.parseMaterial());
		}
		// End 1.15 objects

		// Start 1.16 objects
		if (currentVersion.isAtLeast(MinecraftVersion.NETHER_UPDATE)) {
			CLIMBABLE.add(XMaterial.TWISTING_VINES.parseMaterial());
			CLIMBABLE.add(XMaterial.TWISTING_VINES_PLANT.parseMaterial());
			CLIMBABLE.add(XMaterial.WEEPING_VINES.parseMaterial());
			CLIMBABLE.add(XMaterial.WEEPING_VINES_PLANT.parseMaterial());
		}
		// End 1.16 objects

		// Start 1.17 objects
		if (currentVersion.isAtLeast(MinecraftVersion.CAVES_CLIFFS_1)) {
			CLIMBABLE.add(XMaterial.CAVE_VINES.parseMaterial());
			CLIMBABLE.add(XMaterial.CAVE_VINES_PLANT.parseMaterial());
			CLIMBABLE.add(XMaterial.GLOW_BERRIES.parseMaterial());
			CLIMBABLE.add(XMaterial.POWDER_SNOW.parseMaterial());
		}
		// End 1.17 objects
		// End climbable
	}
}
