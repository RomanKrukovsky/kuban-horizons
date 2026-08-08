package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/** Движок создания летающих домов и перестройки деревень (Flying Structure & Village Wealth Engine). */
public final class FlyingStructureEngine {
    private FlyingStructureEngine() {
    }

    public static boolean makeHouseFly(ServerLevel level, BlockPos origin, Player player) {
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(origin));

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos target = origin.offset(x, 10, z);
                if (level.isEmptyBlock(target)) {
                    level.setBlock(target, Blocks.SMOOTH_QUARTZ.defaultBlockState(), 3);
                }
            }
        }

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.flying_house"));
        return true;
    }

    public static boolean makeVillageWealthy(ServerLevel level, BlockPos villageCenter, Player player) {
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(villageCenter));

        for (int x = -5; x <= 5; x += 2) {
            for (int z = -5; z <= 5; z += 2) {
                BlockPos chestPos = villageCenter.offset(x, 0, z);
                if (level.isEmptyBlock(chestPos)) {
                    level.setBlock(chestPos, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                }
            }
        }

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.wealthy_village"));
        return true;
    }
}
