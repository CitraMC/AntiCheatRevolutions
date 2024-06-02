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

package com.citramc.anticheatrevolutions.check.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.event.EventListener;
import com.citramc.anticheatrevolutions.util.User;

/**
 * @author Rammelkast
 */
public final class MorePacketsCheck {

	public static final Map<UUID, Long> LAST_PACKET_TIME = new HashMap<UUID, Long>();
	public static final Map<UUID, Long> EXEMPT_TIMINGS = new HashMap<UUID, Long>();
	public static final Map<UUID, Double> PACKET_BALANCE = new HashMap<UUID, Double>();

	public static void runCheck(final Player player, final PlayerMoveEvent event) {
		if (event.isCancelled()) {
			// Do not check if cancelled
			return;
		}

		// Confirm if we should even check for MorePackets
		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();
		final double tps = AntiCheatRevolutions.getPlugin().getTPS();
		if (!AntiCheatRevolutions.getManager().getCheckManager().willCheck(player, CheckType.MOREPACKETS)
				|| tps < checksConfig.getDouble(CheckType.MOREPACKETS, "minimumTps")) {
			return;
		}

		final UUID uuid = player.getUniqueId();
		final User user = AntiCheatRevolutions.getManager().getUserManager().getUser(uuid);
		final int maxPing = checksConfig.getInteger(CheckType.MOREPACKETS, "maxPing");
		final boolean disableForLagging = checksConfig.getBoolean(CheckType.MOREPACKETS, "disableForLagging");
		if (AntiCheatRevolutions.getManager().getBackend().isDoing(player, EXEMPT_TIMINGS, -1)
				|| (maxPing > 0 && user.getPing() > maxPing) || (disableForLagging && user.isLagging())) {
			return;
		}

		final long packetTimeNow = System.currentTimeMillis();
		final long lastPacketTime = LAST_PACKET_TIME.getOrDefault(uuid, packetTimeNow - 50L);
		double packetBalance = PACKET_BALANCE.getOrDefault(uuid, 0D);

		final long rate = packetTimeNow - lastPacketTime;
		packetBalance += 50;
		packetBalance -= rate;
		final int triggerBalance = checksConfig.getInteger(CheckType.MOREPACKETS, "triggerBalance");
		final int minimumClamp = checksConfig.getInteger(CheckType.MOREPACKETS, "minimumClamp");
		if (packetBalance >= triggerBalance) {
			final int ticks = (int) Math.round(packetBalance / 50);
			packetBalance = -1 * (triggerBalance / 2);
			flag(player, event, "overshot timer by " + ticks + " tick(s)");
		} else if (packetBalance < -1 * minimumClamp) {
			// Clamp minimum, 50ms=1tick of lag leniency
			packetBalance = -1 * minimumClamp;
		}

		LAST_PACKET_TIME.put(uuid, packetTimeNow);
		PACKET_BALANCE.put(uuid, packetBalance);
	}

	private static void flag(final Player player, final PlayerMoveEvent event, final String message) {
		event.setCancelled(true);
		// We are currently not in the main server thread, so switch
		AntiCheatRevolutions.sendToMainThread(new Runnable() {
			@Override
			public void run() {
				EventListener.log(new CheckResult(CheckResult.Result.FAILED, message).getMessage(), player,
						CheckType.MOREPACKETS, null);
				player.teleport(player.getLocation());
			}
		});
	}

	public static void compensate(final Player player) {
		final UUID uuid = player.getUniqueId();
		final Checks checksConfig = AntiCheatRevolutions.getManager().getConfiguration().getChecks();
		final double packetBalance = PACKET_BALANCE.getOrDefault(uuid, 0D);
		PACKET_BALANCE.put(uuid,
				packetBalance - checksConfig.getInteger(CheckType.MOREPACKETS, "teleportCompensation"));
	}

}
