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
package com.citramc.anticheatrevolutions.check.movement;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.Utilities;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class BoatFlyCheckTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;
    private Boat boat;
    private MovementManager mockMovementManager;
    private MockedStatic<Utilities> mockUtilities;
    private Checks mockChecks;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        boat = (Boat) world.spawnEntity(new Location(world, 0, 10, 0), EntityType.BOAT);
        mockMovementManager = mock(MovementManager.class);
        mockUtilities = Mockito.mockStatic(Utilities.class);
        mockChecks = mock(Checks.class);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testNotInBoat() {
        assertFalse(player.isInsideVehicle(), "Player should not be in a vehicle initially.");
        CheckResult result = BoatFlyCheck.runCheck(player, mockMovementManager, player.getLocation());
        assertEquals(CheckResult.Result.PASSED, result.getResult(), "Should pass if player is not in a boat.");
    }

    @Test
    public void testInBoatButNotFlying() {
        boat.addPassenger(player); // Correct method to add player inside the boat
        assertTrue(player.isInsideVehicle(), "Player should be inside a vehicle.");

        when(mockMovementManager.getMotionY()).thenReturn(0.0001);

        CheckResult result = BoatFlyCheck.runCheck(player, mockMovementManager, player.getLocation());
        assertEquals(CheckResult.Result.PASSED, result.getResult(), "Should pass if the boat is not flying.");
    }

    @Test
    public void testFlyingInBoatWithWaterBelow() {
        boat.addPassenger(player);
        when(mockMovementManager.getMotionY()).thenReturn(0.02); // Higher than the threshold to be considered flying

        Location loc = player.getLocation();
        Block mockBlock = server.getWorld("world").getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        when(mockBlock.getType()).thenReturn(Material.WATER);
        mockUtilities.when(() -> Utilities.cantStandAt(mockBlock)).thenReturn(false);

        CheckResult result = BoatFlyCheck.runCheck(player, mockMovementManager, loc);
        assertEquals(CheckResult.Result.PASSED, result.getResult(),
                "Should pass if there is water directly below the boat.");
    }

    @Test
    public void testFlyingInBoatViolationFlagging() {
        boat.addPassenger(player);
        when(mockMovementManager.getMotionY()).thenReturn(0.02); // Higher than the threshold
        when(mockChecks.getInteger(CheckType.BOATFLY, "vlBeforeFlag")).thenReturn(3);

        Location loc = player.getLocation();
        Block mockBlock = server.getWorld("world").getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        when(mockBlock.getType()).thenReturn(Material.STONE);
        mockUtilities.when(() -> Utilities.cantStandAt(mockBlock)).thenReturn(true);

        // Simulate three checks to trigger a violation
        for (int i = 0; i < 3; i++) {
            BoatFlyCheck.runCheck(player, mockMovementManager, loc);
        }
        CheckResult result = BoatFlyCheck.runCheck(player, mockMovementManager, loc);
        assertEquals(CheckResult.Result.FAILED, result.getResult(),
                "Should fail after multiple violations with no valid block below.");
    }
}
