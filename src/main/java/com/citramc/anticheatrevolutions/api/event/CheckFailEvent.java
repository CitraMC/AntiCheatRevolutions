/*
 * AntiCheatRevolutions for Bukkit and Spigot.
 * Copyright (c) 2012-2015 AntiCheat Team
 * Copyright (c) 2016-2022 Rammelkast
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

package com.citramc.anticheatrevolutions.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.util.User;

/**
 * Fired when a player fails an AntiCheatRevolutions check
 */
public final class CheckFailEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final User user;
    private final CheckType type;

    public CheckFailEvent(final User user, final CheckType type) {
        this.user = user;
        this.type = type;
    }

    /**
     * Get the {@link com.citramc.anticheatrevolutions.util.User} who failed the check
     *
     * @return a {@link com.citramc.anticheatrevolutions.util.User}
     */
    public User getUser() {
        return user;
    }

    /**
     * Get the {@link com.citramc.anticheatrevolutions.check.CheckType} failed
     *
     * @return a {@link com.citramc.anticheatrevolutions.check.CheckType}
     */
    public CheckType getCheck() {
        return type;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
