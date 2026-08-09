package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;

/**
 * Единственная часть дворца.
 *
 * <p>Дворец не делится на части: {@link HallGenerator} строит композицию целиком
 * и обрезает записи по текущему чанку. Разбиение на десятки piece'ов
 * потребовало бы синхронизировать anchors между ними, а выигрыша не дало бы —
 * обрезка уже гарантирует, что каждый чанк пишет только своё.</p>
 */
public final class GeniePalacePiece extends StructurePiece {
    private static final String TAG_ORIGIN_X = "OriginX";
    private static final String TAG_ORIGIN_Y = "OriginY";
    private static final String TAG_ORIGIN_Z = "OriginZ";

    private final BlockPos origin;

    public GeniePalacePiece(BlockPos origin, BoundingBox bounds) {
        super(KHPalaceStructures.GENIE_PALACE_PIECE.get(), 0, bounds);
        this.origin = origin;
    }

    public GeniePalacePiece(CompoundTag tag) {
        super(KHPalaceStructures.GENIE_PALACE_PIECE.get(), tag);
        this.origin = new BlockPos(
                tag.getIntOr(TAG_ORIGIN_X, 0),
                tag.getIntOr(TAG_ORIGIN_Y, 0),
                tag.getIntOr(TAG_ORIGIN_Z, 0));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        // Центр сохраняется явно: без него загруженная часть не знала бы, где
        // стоит дворец, и достроила бы его в другом месте.
        tag.putInt(TAG_ORIGIN_X, origin.getX());
        tag.putInt(TAG_ORIGIN_Y, origin.getY());
        tag.putInt(TAG_ORIGIN_Z, origin.getZ());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
            ChunkGenerator generator, RandomSource random, BoundingBox chunkBounds,
            ChunkPos chunkPos, BlockPos referencePos) {
        PalaceWriter writer = new PalaceWriter(level, chunkBounds,
                level.getMinY(), level.getMaxY());

        HallGenerator.generate(writer, origin.getX(), origin.getY(), origin.getZ());
    }
}
