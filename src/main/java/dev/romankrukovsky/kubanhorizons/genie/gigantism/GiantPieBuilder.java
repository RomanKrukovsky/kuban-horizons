package dev.romankrukovsky.kubanhorizons.genie.gigantism;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Строитель гигантских предметов быта для желаний гигантизма. */
public final class GiantPieBuilder {
    private GiantPieBuilder() {
    }

    /** Гигантский пирог 5x2x5: блюдо из песчаника, белая корочка, оранжевая начинка и торт сверху. */
    public static boolean buildGiantPie(ServerLevel level, BlockPos origin) {
        if (!isAreaClear(level, origin, 5, 2, 5)) {
            return false;
        }
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.SANDSTONE.defaultBlockState());
                boolean rim = x == 0 || x == 4 || z == 0 || z == 4;
                level.setBlockAndUpdate(origin.offset(x, 1, z), rim
                        ? Blocks.DYED_TERRACOTTA.white().defaultBlockState()
                        : Blocks.DYED_TERRACOTTA.orange().defaultBlockState());
            }
        }
        level.setBlockAndUpdate(origin.offset(2, 1, 2), Blocks.CAKE.defaultBlockState());
        return true;
    }

    /** Гигантская кровать 4x1x6: каркас из досок, матрас, подушка и ножки-заборы по углам. */
    public static boolean buildGiantBed(ServerLevel level, BlockPos origin) {
        if (!isAreaClear(level, origin, 4, 1, 6)) {
            return false;
        }
        setLeg(level, origin, 0, 0);
        setLeg(level, origin, 3, 0);
        setLeg(level, origin, 0, 5);
        setLeg(level, origin, 3, 5);
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 6; z++) {
                if (x >= 1 && x <= 2 && z == 0) {
                    level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.WOOL.white().defaultBlockState());
                } else if (x >= 1 && x <= 2 && z >= 1 && z <= 4) {
                    level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.WOOL.red().defaultBlockState());
                } else {
                    level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
        }
        return true;
    }

    private static void setLeg(ServerLevel level, BlockPos origin, int x, int z) {
        level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.OAK_FENCE.defaultBlockState());
    }

    private static boolean isAreaClear(ServerLevel level, BlockPos origin, int width, int height, int depth) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    if (!level.isEmptyBlock(origin.offset(x, y, z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
