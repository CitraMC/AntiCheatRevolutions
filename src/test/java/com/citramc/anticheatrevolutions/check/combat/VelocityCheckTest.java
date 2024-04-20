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

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckResult;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.config.Configuration;
import com.citramc.anticheatrevolutions.config.providers.Checks;
import com.citramc.anticheatrevolutions.manage.AntiCheatManager;
import com.citramc.anticheatrevolutions.manage.UserManager;
import com.citramc.anticheatrevolutions.util.Distance;
import com.citramc.anticheatrevolutions.util.MovementManager;
import com.citramc.anticheatrevolutions.util.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class VelocityCheckTest {

    private ServerMock server;
    private PlayerMock player;
    private AntiCheatManager mockManager;
    private Configuration mockConfiguration;
    private Checks mockChecks;
    private MovementManager mockMovementManager;
    private UserManager mockUserManager;
    private User mockUser;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        mockManager = mock(AntiCheatManager.class);
        mockChecks = mock(Checks.class);
        mockConfiguration = mock(Configuration.class);
        mockUserManager = mock(UserManager.class);
        mockUser = mock(User.class);
        mockMovementManager = mock(MovementManager.class);

        when(mockManager.getConfiguration()).thenReturn(mockConfiguration);
        when(mockConfiguration.getChecks()).thenReturn(mockChecks);
        when(mockManager.getUserManager()).thenReturn(mockUserManager);
        when(mockUserManager.getUser(player.getUniqueId())).thenReturn(mockUser);
        when(mockUser.getMovementManager()).thenReturn(mockMovementManager);

        AntiCheatRevolutions.setManager(mockManager);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testVelocityCompliance() {
        when(mockChecks.getInteger(CheckType.VELOCITY, "minimumPercentage")).thenReturn(80);
        when(mockChecks.getInteger(CheckType.VELOCITY, "vlBeforeFlag")).thenReturn(3);
        when(mockMovementManager.getVelocityExpectedMotionY()).thenReturn(5.0);
        when(mockMovementManager.isOnGround()).thenReturn(false);
        when(mockMovementManager.getMotionY()).thenReturn(5.0);

        CheckResult result = VelocityCheck.runCheck(player, new Distance());
        assertEquals(CheckResult.Result.PASSED, result.getResult());
    }

    @Test
    public void testVelocityNonCompliance() {
        when(mockChecks.getInteger(CheckType.VELOCITY, "minimumPercentage")).thenReturn(80);
        when(mockChecks.getInteger(CheckType.VELOCITY, "vlBeforeFlag")).thenReturn(3);
        when(mockMovementManager.getVelocityExpectedMotionY()).thenReturn(10.0);
        when(mockMovementManager.isOnGround()).thenReturn(false);
        when(mockMovementManager.getMotionY()).thenReturn(5.0); // 50% of expected, below the threshold of 80%

        CheckResult result = VelocityCheck.runCheck(player, new Distance());
        assertEquals(CheckResult.Result.PASSED, result.getResult()); // First time, only a pass, violation count
                                                                     // increases
        result = VelocityCheck.runCheck(player, new Distance()); // Second check
        result = VelocityCheck.runCheck(player, new Distance()); // Third check
        assertEquals(CheckResult.Result.FAILED, result.getResult()); // Should fail now
    }

    @Test
    public void testPlayerOnGround() {
        when(mockMovementManager.getVelocityExpectedMotionY()).thenReturn(5.0);
        when(mockMovementManager.isOnGround()).thenReturn(true);

        CheckResult result = VelocityCheck.runCheck(player, new Distance());
        assertEquals(CheckResult.Result.PASSED, result.getResult(),
                "Grounded players should not trigger velocity checks.");
    }
}
