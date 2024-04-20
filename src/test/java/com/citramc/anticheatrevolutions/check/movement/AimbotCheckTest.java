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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.Backend;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.Configuration;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.manage.AntiCheatManager;
import com.citramc.anticheatrevolutions.manage.UserManager;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class AimbotCheckTest {

    private ServerMock server;
    private PlayerMock player;
    private WorldMock world;
    private AntiCheatManager mockManager;
    private Backend mockBackend;
    private UserManager mockUserManager;
    private Configuration mockConfiguration;
    private User mockUser;
    private MovementManager mockMovementManager;
    private Checks mockChecks;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world"); // Ensure a world is added and named.
        player = server.addPlayer();

        mockManager = mock(AntiCheatManager.class);
        mockBackend = mock(Backend.class);
        mockUserManager = mock(UserManager.class);
        mockConfiguration = mock(Configuration.class);
        mockUser = mock(User.class);
        mockMovementManager = mock(MovementManager.class);
        mockChecks = mock(Checks.class);

        AntiCheatRevolutions.setManager(mockManager);
        when(mockManager.getBackend()).thenReturn(mockBackend);
        when(mockManager.getUserManager()).thenReturn(mockUserManager);
        when(mockManager.getConfiguration()).thenReturn(mockConfiguration);
        when(mockUserManager.getUser(player.getUniqueId())).thenReturn(mockUser);
        when(mockUser.getMovementManager()).thenReturn(mockMovementManager);
        when(mockConfiguration.getChecks()).thenReturn(mockChecks);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testExemptPlayer() {
        when(mockBackend.isMovingExempt(player)).thenReturn(true);

        CheckResult result = AimbotCheck.runCheck(player, new EntityDamageByEntityEvent(player, player, null, 0));
        assertEquals(CheckResult.Result.PASSED, result.getResult(), "Exempt players should always pass the check.");
    }

    @Test
    public void testFailedAimbotCheck() {
        // Setup non-exempt conditions
        when(mockBackend.isMovingExempt(player)).thenReturn(false);
        when(mockChecks.getDouble(CheckType.AIMBOT, "minAcceleration")).thenReturn(0.2);
        when(mockChecks.getDouble(CheckType.AIMBOT, "maxMod")).thenReturn(0.5);

        // Mocking player movement for failing conditions
        player.teleport(new Location(world, 0, 64, 0, 0.0f, 30.0f)); // Set the player's location in the test world.
        when(mockMovementManager.getDeltaPitch()).thenReturn(16.0f);
        when(mockMovementManager.getLastDeltaPitch()).thenReturn(10.0f);

        // Mocking the static method within the test method
        MockedStatic<Utilities> mocked = Mockito.mockStatic(Utilities.class);
        long mockedGcd = 100;
        mocked.when(() -> Utilities.getGcd(anyLong(), anyLong())).thenReturn(mockedGcd);

        CheckResult result = AimbotCheck.runCheck(player, new EntityDamageByEntityEvent(player, player, null, 0));
        assertEquals(CheckResult.Result.FAILED, result.getResult(),
                "Players with suspicious pitch movements should fail the aimbot check.");
    }

    @Test
    public void testPassedAimbotCheck() {
        // Setup non-exempt conditions
        when(mockBackend.isMovingExempt(player)).thenReturn(false);
        when(mockChecks.getDouble(CheckType.AIMBOT, "minAcceleration")).thenReturn(0.2);
        when(mockChecks.getDouble(CheckType.AIMBOT, "maxMod")).thenReturn(0.5);

        // Mocking player movement for passing conditions
        player.teleport(new Location(world, 0, 64, 0, 0.0f, 10.0f)); // Set the player's location in the test world.
        when(mockMovementManager.getDeltaPitch()).thenReturn(10.0f);
        when(mockMovementManager.getLastDeltaPitch()).thenReturn(9.0f);

        CheckResult result = AimbotCheck.runCheck(player, new EntityDamageByEntityEvent(player, player, null, 0));
        assertEquals(CheckResult.Result.PASSED, result.getResult(),
                "Players with normal pitch movements should pass the aimbot check.");
    }
}
