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
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

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

public class AimbotCheckTest {

    private ServerMock server;
    private PlayerMock player;
    private AntiCheatManager manager;
    private Backend backend;
    private UserManager userManager;
    private User user;
    private MovementManager movementManager;
    private Configuration configuration;
    private Checks checksConfig;
    private MockedStatic<Utilities> mockedUtilities;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        manager = mock(AntiCheatManager.class);
        backend = mock(Backend.class);
        userManager = mock(UserManager.class);
        user = mock(User.class);
        movementManager = mock(MovementManager.class);
        configuration = mock(Configuration.class);
        checksConfig = mock(Checks.class);
        

        // Setup the AntiCheatRevolutions manager
        AntiCheatRevolutions.setManager(manager);
        when(manager.getBackend()).thenReturn(backend);
        when(manager.getUserManager()).thenReturn(userManager);
        when(userManager.getUser(player.getUniqueId())).thenReturn(user);
        when(manager.getConfiguration()).thenReturn(configuration);
        when(configuration.getChecks()).thenReturn(checksConfig);
        when(user.getMovementManager()).thenReturn(movementManager);

        mockedUtilities = Mockito.mockStatic(Utilities.class);
    }

    @AfterEach
    public void tearDown() {
        mockedUtilities.close();
        MockBukkit.unmock();
    }

    @Test
    public void testAimbotCheck_Pass() {
        when(backend.isMovingExempt(player)).thenReturn(false);
        when(movementManager.getDeltaPitch()).thenReturn(10f);
        when(movementManager.getLastDeltaPitch()).thenReturn(5f);
        when(checksConfig.getDouble(CheckType.AIMBOT, "minAcceleration")).thenReturn(2.0);
        when(checksConfig.getDouble(CheckType.AIMBOT, "maxMod")).thenReturn(0.05);

        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, null, null, 0);
        CheckResult result = AimbotCheck.runCheck(player, event);
        assertEquals(CheckResult.Result.PASSED, result.getResult(), "Aimbot check should pass under normal conditions.");
    }

    @Test
    public void testAimbotCheck_Fail() {
        long gcd = 1L; // Simulate a perfect GCD
        mockedUtilities.when(() -> Utilities.getGcd(anyLong(), anyLong())).thenReturn(gcd);
        
        when(backend.isMovingExempt(player)).thenReturn(false);
        when(movementManager.getDeltaPitch()).thenReturn(10f);
        when(movementManager.getLastDeltaPitch()).thenReturn(5f);
        when(checksConfig.getDouble(CheckType.AIMBOT, "minAcceleration")).thenReturn(1.0);
        when(checksConfig.getDouble(CheckType.AIMBOT, "maxMod")).thenReturn(0.01);

        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, null, null, 0);
        CheckResult result = AimbotCheck.runCheck(player, event);
        assertEquals(CheckResult.Result.FAILED, result.getResult(), "Aimbot check should fail when suspicious conditions are met.");
    }
}
