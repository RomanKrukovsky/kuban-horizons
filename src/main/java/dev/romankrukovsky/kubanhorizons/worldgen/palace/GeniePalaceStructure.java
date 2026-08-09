package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Дворец джиннии как структура worldgen.
 *
 * <p>Позиция строго детерминирована: дворец всегда стоит в начале координат
 * измерения на фиксированной высоте. Это landmark, а не разбросанная структура,
 * поэтому у него нет ни поиска места, ни разброса по чанкам — иначе точка входа
 * игрока перестала бы быть предсказуемой.</p>
 */
public final class GeniePalaceStructure extends Structure {
    public static final MapCodec<GeniePalaceStructure> CODEC =
            simpleCodec(GeniePalaceStructure::new);

    public GeniePalaceStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        // Дворец существует только в единственном экземпляре в начале координат.
        // Любой другой чанк-кандидат отклоняется, поэтому копий не появится.
        if (context.chunkPos().x() != 0 || context.chunkPos().z() != 0) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(0,
                dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHDimensions.PALACE_FLOOR_Y, 0);
        return Optional.of(new Structure.GenerationStub(origin, builder -> addPieces(builder, origin)));
    }

    private static void addPieces(StructurePiecesBuilder builder, BlockPos origin) {
        builder.addPiece(new GeniePalacePiece(origin, palaceBounds(origin)));
    }

    /**
     * Габаритный бокс дворца.
     *
     * <p>Немного шире самого зала: в запас попадают лестница к арке, площадка
     * входа и слои основания под полом. Если бокс окажется меньше композиции,
     * движок обрежет дальние чанки и дворец сгенерируется частично.</p>
     */
    static BoundingBox palaceBounds(BlockPos origin) {
        int padX = PalaceGrid.HALL_RADIUS_X + 8;
        int padZ = PalaceGrid.HALL_RADIUS_Z + 12;
        return new BoundingBox(
                origin.getX() - padX,
                origin.getY() - 8,
                origin.getZ() - padZ,
                origin.getX() + padX,
                origin.getY() + PalaceGrid.HALL_HEIGHT + 2,
                origin.getZ() + padZ);
    }

    @Override
    public StructureType<?> type() {
        return KHPalaceStructures.GENIE_PALACE.get();
    }
}
