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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.citramc.anticheatrevolutions.util.Distance;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;

public class VelocityCheckTest {

    private ServerMock server;
    private PlayerMock player;
    private AntiCheatManager manager;
    private Backend backend;
    private UserManager userManager;
    private User user;
    private MovementManager movementManager;
    private Configuration configuration;
    private Checks checksConfig;

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
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testVelocityCheck_Pass() {
        when(movementManager.getVelocityExpectedMotionY()).thenReturn(0.0);
        when(movementManager.isOnGround()).thenReturn(true);

        CheckResult result = VelocityCheck.runCheck(player, new Distance());
        assertEquals(CheckResult.Result.PASSED, result.getResult(), "Check should pass when no velocity is expected.");
    }

    @Test
    public void testVelocityCheck_FailIgnoredVelocity() {
        when(movementManager.getVelocityExpectedMotionY()).thenReturn(2.0);
        when(movementManager.getMotionY()).thenReturn(0.5);
        when(movementManager.isOnGround()).thenReturn(false);
        when(checksConfig.getInteger(CheckType.VELOCITY, "minimumPercentage")).thenReturn(50);
        when(checksConfig.getInteger(CheckType.VELOCITY, "vlBeforeFlag")).thenReturn(1);

        CheckResult result = VelocityCheck.runCheck(player, new Distance());
        assertEquals(CheckResult.Result.FAILED, result.getResult(), "Check should fail if the player ignores server velocity.");
    }

    @Test
    public void testVelocityCheck_ViolationReset() {
        when(movementManager.getVelocityExpectedMotionY()).thenReturn(2.0);
        when(movementManager.getMotionY()).thenReturn(2.0); // Complies with expected motion
        when(movementManager.isOnGround()).thenReturn(false);
        when(checksConfig.getInteger(CheckType.VELOCITY, "minimumPercentage")).thenReturn(50);
        when(checksConfig.getInteger(CheckType.VELOCITY, "vlBeforeFlag")).thenReturn(2);

        // Assume previous violations
        VelocityCheck.VIOLATIONS.put(player.getUniqueId(), 1);

        VelocityCheck.runCheck(player, new Distance()); // Should not add violation
        int violations = VelocityCheck.VIOLATIONS.getOrDefault(player.getUniqueId(), 0);

        assertEquals(0, violations, "Violations should reset on compliance.");
    }
}
