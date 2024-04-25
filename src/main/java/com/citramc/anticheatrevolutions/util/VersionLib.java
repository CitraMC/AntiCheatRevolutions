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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class VersionLib {

	public static final MinecraftVersion CURRENT_VERSION;
	private static final List<String> SUPPORTED_VERSIONS;

	public static String getVersion() {
		return Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
	}

	public static boolean isSupported() {
		String version = getVersion();
		return SUPPORTED_VERSIONS.stream().anyMatch(version::startsWith);
	}

	public static boolean isOfVersion(final String versionId) {
		return getVersion().startsWith(versionId);
	}

	public static boolean isFlying(final Player player) {
		boolean hasLevitationEffect = false;
		if (CURRENT_VERSION.isAtLeast(MinecraftVersion.COMBAT_UPDATE)) {
			for (PotionEffect effect : player.getActivePotionEffects()) {
				if (effect.getType().equals(PotionEffectType.LEVITATION)) {
					hasLevitationEffect = true;
					break;
				}
			}
		}
		return player.isFlying() || player.getGameMode() == GameMode.SPECTATOR || isGliding(player)
				|| hasLevitationEffect;
	}

	public static boolean isSlowFalling(final Player player) {
		if (CURRENT_VERSION.isAtLeast(MinecraftVersion.AQUATIC_UPDATE)) {
			return player.hasPotionEffect(PotionEffectType.SLOW_FALLING);
		}
		return false;
	}

	public static boolean isRiptiding(final Player player) {
		if (CURRENT_VERSION.isAtLeast(MinecraftVersion.AQUATIC_UPDATE)) {
			return player.isRiptiding();
		}
		return false;
	}

	public static boolean isFrostWalk(final Player player) {
		if (CURRENT_VERSION.isAtLeast(MinecraftVersion.COMBAT_UPDATE) || player.getInventory().getBoots() == null) {
			return player.getInventory().getBoots().containsEnchantment(Enchantment.FROST_WALKER);
		}
		return false;
	}

	public static boolean isSoulSpeed(final ItemStack boots) {
		if (boots != null && CURRENT_VERSION.isAtLeast(MinecraftVersion.NETHER_UPDATE)) {
			return boots.containsEnchantment(Enchantment.getByName("SOUL_SPEED"));
		}
		return false;
	}

	public static ItemStack getItemInHand(final Player player) {
		if (CURRENT_VERSION.isAtLeast(MinecraftVersion.COMBAT_UPDATE)) {
			return player.getInventory().getItemInMainHand();
		}
		return player.getInventory().getItemInHand();
	}

	public static int getPlayerPing(final Player player) {
		try {
			if (player == null)
				return -1;
			final Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
			return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
		} catch (Exception e) {
			return -1;
		}
	}

	public static Block getTargetBlock(final Player player, final int distance) {
		if (CURRENT_VERSION.isAtLeast(MinecraftVersion.AQUATIC_UPDATE)) {
			return player.getTargetBlockExact(distance);
		}
		return player.getTargetBlock((Set<Material>) null, distance);
	}

	public static boolean isGliding(final Player player) {
		return CURRENT_VERSION.isAtLeast(MinecraftVersion.COMBAT_UPDATE) && player.isGliding();

	}

	public static boolean isLevitationEffect(final PotionEffect effect) {
		return CURRENT_VERSION.isAtLeast(MinecraftVersion.COMBAT_UPDATE)
				&& effect.getType().equals(PotionEffectType.LEVITATION);
	}

	public static int getPotionLevel(final Player player, final PotionEffectType type) {
		for (PotionEffect effect : player.getActivePotionEffects()) {
			if (effect.getType().equals(type)) {
				return effect.getAmplifier() + 1;
			}
		}
		return 0;
	}

	public static boolean isSwimming(final Player player) {
		return CURRENT_VERSION.isAtLeast(MinecraftVersion.AQUATIC_UPDATE) && player.isSwimming();
	}

	public static PotionEffectType getJumpEffectType() {
		try {
			if (CURRENT_VERSION.isAtLeast(MinecraftVersion.ARMORED_PAWS)) {
				return PotionEffectType.getByName("JUMP_BOOST");
			} else {
				Field field = PotionEffectType.class.getField("JUMP");
				return (PotionEffectType) field.get(null);
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	static {
		SUPPORTED_VERSIONS = Arrays.asList(new String[] {
				"v1_20", "v1_19",
				"v1_18", "v1_17", "v1_16", "v1_15", "v1_14", "v1_13", "v1_12",
				"v1_11", "v1_10", "v1_9", "v1_8"
		});
		CURRENT_VERSION = MinecraftVersion.getCurrentVersion();
	}
}
