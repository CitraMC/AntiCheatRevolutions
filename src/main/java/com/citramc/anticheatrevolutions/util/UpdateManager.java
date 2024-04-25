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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;

public class UpdateManager {

	public static final int RESOURCE_ID = 116270;
	public static final String SPIGOT_VERSION_URL = "https://api.spigotmc.org/legacy/update.php?resource="
			+ RESOURCE_ID;

	private String latestVersion;
	private boolean isLatest;
	private boolean isAhead;

	public UpdateManager() {
		update();
	}

	public void update() {
		this.latestVersion = getOnlineData(SPIGOT_VERSION_URL);
		if (this.latestVersion == null) {
			this.isLatest = true;
			this.isAhead = false;
			return;
		}

		try {
			VersionSplit currentSplit = new VersionSplit(AntiCheatRevolutions.getVersion());
			VersionSplit newSplit = new VersionSplit(this.latestVersion);
			int splitCompare = currentSplit.compareTo(newSplit);
			this.isLatest = splitCompare >= 0;
			this.isAhead = splitCompare > 0;
		} catch (Exception e) {
			AntiCheatRevolutions.getPlugin().getLogger().severe("Failed to compare versions: " + e.getMessage());
		}
	}

	private String getOnlineData(final String url) {
		try (InputStream stream = new URL(url).openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			return builder.toString();
		} catch (final IOException exception) {
			AntiCheatRevolutions.getPlugin().getLogger().warning("Error reading from URL: " + exception.getMessage());
			return null;
		}
	}

	public boolean isLatest() {
		return this.isLatest;
	}

	public boolean isAhead() {
		return this.isAhead;
	}

	public String getCurrentVersion() {
		return AntiCheatRevolutions.getVersion();
	}

	public String getLatestVersion() {
		return this.latestVersion;
	}

	public class VersionSplit implements Comparable<VersionSplit> {
		private final int major, minor, build;
		private boolean prerelease = false;

		public VersionSplit(String version) throws IllegalArgumentException {
			String[] parts = version.split("-");
			if (parts.length > 1 && parts[1].matches("RC|PRE|SNAPSHOT|BETA|ALPHA")) {
				prerelease = true;
			}

			String[] versionNumbers = parts[0].split("\\.");
			if (versionNumbers.length != 3) {
				throw new IllegalArgumentException("Version format must be major.minor.build");
			}

			this.major = Integer.parseInt(versionNumbers[0]);
			this.minor = Integer.parseInt(versionNumbers[1]);
			this.build = Integer.parseInt(versionNumbers[2]);
		}

		@Override
		public int compareTo(final VersionSplit other) {
			int result = Integer.compare(this.major, other.major);
			if (result == 0) {
				result = Integer.compare(this.minor, other.minor);
				if (result == 0) {
					result = Integer.compare(this.build, other.build);
					if (result == 0) {
						return Boolean.compare(other.prerelease, this.prerelease);
					}
				}
			}
			return result;
		}

		public int getMajor() {
			return major;
		}

		public int getMinor() {
			return minor;
		}

		public int getBuild() {
			return build;
		}
	}
}
