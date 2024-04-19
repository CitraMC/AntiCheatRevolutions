/*
 * AntiCheatRevolutions for Bukkit and Spigot.
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

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CriticalsCheckTest {

    private ServerMock server;
    private PlayerMock player;
    private World world;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        player.teleport(new Location(world, 0, 10, 0));  // Ensure the player is not on the ground
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testNonPlayerDamager() {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(null, player, DamageCause.ENTITY_ATTACK, 10);
        CriticalsCheck.doDamageEvent(event, player);
        assertFalse(event.isCancelled(), "Event should not be cancelled if damager is not a player");
    }

    @Test
    public void testNonCriticalHit() {
        player.setFallDistance(0f); // Player is not falling
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, player, DamageCause.ENTITY_ATTACK, 10);
        CriticalsCheck.doDamageEvent(event, player);
        assertFalse(event.isCancelled(), "Event should not be cancelled if hit is not critical");
    }

    @Test
    public void testCriticalHitBlocked() {
        player.setFallDistance(1f); // Player is falling
        player.setSprinting(true);
        Location blockLocation = player.getLocation().clone().subtract(0, 1, 0);
        blockLocation.getBlock().setType(Material.STONE); // The block below the player is solid
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, player, DamageCause.ENTITY_ATTACK, 10);
        CriticalsCheck.doDamageEvent(event, player);
        assertTrue(event.isCancelled(), "Event should be cancelled if critical conditions are not met");
    }

    @Test
    public void testCriticalHitAllowed() {
        player.setFallDistance(1f); // Player is falling
        player.setSprinting(true);
        Location blockLocation = player.getLocation().clone().subtract(0, 1, 0);
        blockLocation.getBlock().setType(Material.AIR); // The block below the player is not solid
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, player, DamageCause.ENTITY_ATTACK, 10);
        CriticalsCheck.doDamageEvent(event, player);
        assertFalse(event.isCancelled(), "Event should not be cancelled if critical hit is allowed");
    }
}
