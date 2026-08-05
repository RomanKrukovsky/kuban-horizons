package dev.romankrukovsky.kubanhorizons.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Расчёт скорости роста культур по ванильной формуле.
 *
 * <p>Копия логики {@code CropBlock.getGrowthSpeed} (protected в ванили):
 * учитывает пригодность и увлажнённость почвы в области 3×3 под растением
 * и штраф за плотную посадку той же культуры. Используется всеми
 * культурами мода, чтобы поведение фермы совпадало с ожиданиями игрока.</p>
 */
public final class CropGrowth {
    private CropGrowth() {
    }

    public static float growthSpeed(BlockState cropState, BlockGetter level, BlockPos pos) {
        float speed = 1.0F;
        BlockPos below = pos.below();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                float bonus = 0.0F;
                BlockPos soilPos = below.offset(dx, 0, dz);
                BlockState soil = level.getBlockState(soilPos);
                var decision = soil.canSustainPlant(level, soilPos, Direction.UP, cropState);
                boolean sustains = decision.isDefault()
                        ? soil.is(BlockTags.GROWS_CROPS)
                        : decision.isTrue();
                if (sustains) {
                    bonus = 1.0F;
                    if (soil.isFertile(level, soilPos)) {
                        bonus = 3.0F;
                    }
                }
                if (dx != 0 || dz != 0) {
                    bonus /= 4.0F;
                }
                speed += bonus;
            }
        }

        // Штраф за посадку той же культуры вплотную (диагональ или крест).
        BlockPos north = pos.north();
        BlockPos south = pos.south();
        BlockPos west = pos.west();
        BlockPos east = pos.east();
        boolean sameRow = level.getBlockState(west).is(cropState.getBlock())
                || level.getBlockState(east).is(cropState.getBlock());
        boolean sameColumn = level.getBlockState(north).is(cropState.getBlock())
                || level.getBlockState(south).is(cropState.getBlock());
        if (sameRow && sameColumn) {
            speed /= 2.0F;
        } else {
            boolean diagonal = level.getBlockState(north.west()).is(cropState.getBlock())
                    || level.getBlockState(north.east()).is(cropState.getBlock())
                    || level.getBlockState(south.west()).is(cropState.getBlock())
                    || level.getBlockState(south.east()).is(cropState.getBlock());
            if (diagonal) {
                speed /= 2.0F;
            }
        }
        return speed;
    }
}
