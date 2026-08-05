package dev.romankrukovsky.kubanhorizons.crop;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * Саженец плодового дерева.
 *
 * <p>Не использует {@code SaplingBlock}/{@code TreeGrower} (те требуют
 * datapack configured feature): дерево строится программно — ствол из
 * 4 дубовых брёвен и крона 3×3×2 из плодовой листвы с
 * {@code PERSISTENT=true} (не осыпается).</p>
 */
public class FruitSaplingBlock extends VegetationBlock implements BonemealableBlock {
    /** Кодек восстанавливает блок листвы по registry-id. */
    public static final MapCodec<FruitSaplingBlock> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    BuiltInRegistries.BLOCK.byNameCodec().fieldOf("leaves").forGetter(b -> b.leaves.get()),
                    propertiesCodec()
            ).apply(i, (leaves, props) -> new FruitSaplingBlock(() -> leaves, props)));

    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 12.0);
    /** Высота ствола (дубовые брёвна). */
    private static final int TRUNK_HEIGHT = 4;

    private final Supplier<? extends Block> leaves;

    public FruitSaplingBlock(Supplier<? extends Block> leaves, BlockBehaviour.Properties properties) {
        super(properties);
        this.leaves = leaves;
    }

    @Override
    public MapCodec<? extends FruitSaplingBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return; // не грузим чанки ради проверки света
        }
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) {
            this.growTree(level, pos);
        }
    }

    /**
     * Программная постройка маленького дерева: ствол {@value TRUNK_HEIGHT}
     * дубовых брёвен + крона 3×3×2 из плодовой листвы вокруг вершины.
     */
    public void growTree(ServerLevel level, BlockPos pos) {
        if (!level.isInsideBuildHeight(pos.getY() + TRUNK_HEIGHT + 1)) {
            return;
        }
        // Проверяем, что место под ствол свободно (кроме самого саженца).
        for (int y = 1; y < TRUNK_HEIGHT; y++) {
            BlockState occupant = level.getBlockState(pos.above(y));
            if (!occupant.isAir() && !occupant.is(this.leaves.get())) {
                return;
            }
        }

        // Ствол.
        for (int y = 0; y < TRUNK_HEIGHT; y++) {
            level.setBlock(pos.above(y), Blocks.OAK_LOG.defaultBlockState(), 3);
        }

        // Крона: два слоя 3×3 вокруг верхних блоков ствола + верхушка.
        BlockState leavesState = this.leaves.get().defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true)
                .setValue(LeavesBlock.DISTANCE, 1);
        for (int layer = 0; layer < 2; layer++) {
            BlockPos center = pos.above(TRUNK_HEIGHT - 2 + layer);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos leafPos = center.offset(dx, 0, dz);
                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlock(leafPos, leavesState, 3);
                    }
                }
            }
        }
        BlockPos top = pos.above(TRUNK_HEIGHT);
        if (level.getBlockState(top).isAir()) {
            level.setBlock(top, leavesState, 3);
        }
    }

    // --- Костная мука ---

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.isInsideBuildHeight(pos.getY() + TRUNK_HEIGHT + 1);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return level.getRandom().nextFloat() < 0.45F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        this.growTree(level, pos);
    }
}
