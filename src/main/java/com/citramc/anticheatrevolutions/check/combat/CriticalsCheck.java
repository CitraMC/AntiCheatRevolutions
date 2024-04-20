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

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;

import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.event.EventListener;
import com.citramc.anticheatrevolutions.util.Utilities;

public final class CriticalsCheck {

	public static void doDamageEvent(final EntityDamageByEntityEvent event, final Player damager) {
		if (!(event.getDamager() instanceof Player) || event.getCause() != DamageCause.ENTITY_ATTACK) {
			return;
		}

		if (isCriticalHit(damager)) {
			event.setCancelled(true);
			logCriticalAttempt(damager);
		}
	}

	private static boolean isCriticalHit(Player player) {
		if (player.getFallDistance() <= 0.0f || player.isOnGround() || player.isInsideVehicle()
				|| player.hasPotionEffect(PotionEffectType.BLINDNESS)
				|| Utilities.isHoveringOverWater(player.getLocation())
				|| player.getEyeLocation().getBlock().getType() == Material.LADDER) {
			return false;
		}
		return isInvalidCritical(player);
	}

	private static boolean isInvalidCritical(Player player) {
		double yFraction = Utilities.getYFraction(player.getLocation());
		if (yFraction != 0 && yFraction != 0.5) {
			return false;
		}
		return Utilities.isSolidBlock(player.getLocation().clone().subtract(0, 1.0, 0));
	}

	private static void logCriticalAttempt(Player player) {
		CheckResult result = new CheckResult(CheckResult.Result.FAILED,
				"Tried to perform a critical hit without meeting necessary conditions");
		EventListener.log(result.getMessage(), player, CheckType.CRITICALS, null);
	}
}
