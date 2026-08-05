package dev.romankrukovsky.kubanhorizons.soil;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;

/**
 * Компактные коды культур для истории севооборота.
 *
 * <p>Коды стабильны и не переиспользуются (AD-006): удаление культуры
 * оставляет её код зарезервированным.</p>
 */
public enum CropKind {
    NONE((byte) 0),
    WHEAT((byte) 1),
    CARROTS((byte) 2),
    POTATOES((byte) 3),
    BEETROOTS((byte) 4),
    SUNFLOWER((byte) 5),
    CORN((byte) 6),
    TEA((byte) 7),
    OTHER((byte) 127);

    private final byte code;

    CropKind(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    /** Определяет культуру по блоку. */
    public static CropKind ofBlock(Block block) {
        if (block == Blocks.WHEAT) {
            return WHEAT;
        }
        if (block == Blocks.CARROTS) {
            return CARROTS;
        }
        if (block == Blocks.POTATOES) {
            return POTATOES;
        }
        if (block == Blocks.BEETROOTS) {
            return BEETROOTS;
        }
        if (block == KHBlocks.SUNFLOWER_CROP.get()) {
            return SUNFLOWER;
        }
        if (block == KHBlocks.CORN_CROP.get()) {
            return CORN;
        }
        if (block == KHBlocks.TEA_BUSH.get()) {
            return TEA;
        }
        if (block instanceof CropBlock) {
            return OTHER;
        }
        return NONE;
    }
}
