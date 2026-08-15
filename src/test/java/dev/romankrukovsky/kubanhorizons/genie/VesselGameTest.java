package dev.romankrukovsky.kubanhorizons.genie;

import dev.romankrukovsky.kubanhorizons.gametest.KHGameTests;
import dev.romankrukovsky.kubanhorizons.genie.vessel.KubanJugBlock;
import dev.romankrukovsky.kubanhorizons.genie.vessel.KubanJugBlockEntity;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("kubanhorizons")
public class VesselGameTest extends KHGameTests {

    @GameTest(template = "empty_5x5", timeoutTicks = 100)
    public void testJugPlacementAndEntity(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, KHBlocks.KUBAN_JUG.get().defaultBlockState());

        BlockEntity be = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(be instanceof KubanJugBlockEntity, "Expected KubanJugBlockEntity");

        helper.succeed();
    }
}
