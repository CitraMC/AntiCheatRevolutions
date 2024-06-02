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

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.event.EventListener;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;

public final class NoSlowCheck {

	public static final Map<UUID, Long> LAST_RELEASE = new HashMap<UUID, Long>();
	public static final Map<UUID, Integer> VIOLATIONS = new HashMap<UUID, Integer>();

	public static void runCheck(final Player player, final PlayerInteractEvent event) {
		if (event.isCancelled()) {
			// Do not check if cancelled
			return;
		}

		if (!AntiCheatRevolutions.getManager().getCheckManager().willCheck(player, CheckType.NOSLOW)) {
			return;
		}

		final UUID uuid = player.getUniqueId();
		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(uuid);
		final MovementManager movementManager = user.getMovementManager();
		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();
		final long time = System.currentTimeMillis();
		final long lastRelease = LAST_RELEASE.getOrDefault(uuid, 0L);
		LAST_RELEASE.put(uuid, time);
		if (lastRelease == 0L) {
			return;
		}

		final long difference = time - lastRelease;
		final long minimumDifference = checksConfig.getInteger(CheckType.NOSLOW, "minimumDifference");
		if (difference < minimumDifference
				&& movementManager.getDistanceXZ() >= checksConfig.getDouble(CheckType.NOSLOW, "minimumDistXZ")) {
			int violations = VIOLATIONS.getOrDefault(uuid, 1);
			if (violations++ >= checksConfig.getInteger(CheckType.NOSLOW, "vlBeforeFlag")) {
				violations = 0;
				flag(player, event,
						"toggled use item too fast (diff=" + difference + ", min=" + minimumDifference + ")");
			}
			VIOLATIONS.put(uuid, violations);
		}
	}

	private static void flag(final Player player, final PlayerInteractEvent event, final String message) {
		event.setCancelled(true);
		// We are currently not in the main server thread, so switch
		AntiCheatRevolutions.sendToMainThread(new Runnable() {
			@Override
			public void run() {
				EventListener.log(new CheckResult(CheckResult.Result.FAILED, message).getMessage(), player,
						CheckType.NOSLOW, null);
				player.teleport(player.getLocation());
			}
		});
	}

}
