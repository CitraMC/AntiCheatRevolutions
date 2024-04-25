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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class FileFormatter extends Formatter {
	private static final ThreadLocal<DateFormat> DATE_FORMAT = ThreadLocal
			.withInitial(() -> new SimpleDateFormat("h:mm:ss"));
	private static final String LINE_SEP = System.getProperty("line.separator");

	@Override
	public String format(LogRecord record) {
		String timestamp = DATE_FORMAT.get().format(new Date(record.getMillis()));

		StringBuilder output = new StringBuilder()
				.append('[')
				.append(record.getLevel())
				.append('|')
				.append(timestamp)
				.append("]: ")
				.append(record.getMessage())
				.append(LINE_SEP);

		return output.toString();
	}
}
