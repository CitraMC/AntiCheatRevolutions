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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CriticalsCheckTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testNonPlayerDamager() {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(null, player,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, 10);
        CriticalsCheck.doDamageEvent(event, player);
        assertFalse(event.isCancelled(), "Event should not be cancelled if damager is not a player");
    }

    @Test
    public void testNonEntityAttackCause() {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, player,
                EntityDamageEvent.DamageCause.FIRE, 10);
        CriticalsCheck.doDamageEvent(event, player);
        assertFalse(event.isCancelled(), "Event should not be cancelled for non-ENTITY_ATTACK causes");
    }

    @Test
    public void testNonCriticalHit() {
        player.setFallDistance(0); // Not falling
        player.setSprinting(false); // Not sprinting
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, player,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, 10);
        CriticalsCheck.doDamageEvent(event, player);
        assertFalse(event.isCancelled(), "Event should not be cancelled if conditions for critical hit are not met");
    }

    @Test
    public void testCriticalHit() {
        player.setFallDistance(1.0f); // Simulate falling
        player.setSprinting(true); // Sprinting
        simulatePlayerAirborne(); // Ensures player is not on the ground or any other invalid block

        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, player,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, 10);
        CriticalsCheck.doDamageEvent(event, player);
        assertTrue(event.isCancelled(), "Event should be cancelled if critical hit conditions are met");
    }

    private void simulatePlayerAirborne() {
        Location loc = player.getLocation();
        loc.getBlock().setType(Material.AIR); // Under the player is air
        player.teleport(loc.clone().add(0, -1, 0)); // Move player up
    }
}
