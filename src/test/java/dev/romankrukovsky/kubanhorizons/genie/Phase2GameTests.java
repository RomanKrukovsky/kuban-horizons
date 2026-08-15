package dev.romankrukovsky.kubanhorizons.genie;

import dev.romankrukovsky.kubanhorizons.gametest.KHGameTests;
import dev.romankrukovsky.kubanhorizons.genie.wish.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("kubanhorizons")
public class Phase2GameTests extends KHGameTests {

    @GameTest(template = "empty_5x5", timeoutTicks = 200)
    public void testBudgetEnforcement(GameTestHelper helper) {
        // TODO: Spawn genie with low relationship, request huge wish, assert truncation
        helper.succeed();
    }

    @GameTest(template = "empty_5x5", timeoutTicks = 200)
    public void testConfirmationFlow(GameTestHelper helper) {
        // TODO: High-risk wish should trigger ConfirmationAuthority
        helper.succeed();
    }

    @GameTest(template = "empty_5x5", timeoutTicks = 200)
    public void testTemperamentReactions(GameTestHelper helper) {
        // TODO: Different personality parameters → different reaction
        helper.succeed();
    }
}
