package dev.romankrukovsky.kubanhorizons.worldgen.palace;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;


import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Реестры дворца: тип структуры и тип её части.
 *
 * <p>Оба реестра — обычные игровые, а не datapack: типы описывают код, поэтому
 * регистрируются через {@link DeferredRegister}, а конкретный экземпляр
 * структуры уже приходит из JSON.</p>
 */
public final class KHPalaceStructures {
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, KubanHorizons.MOD_ID);

    private static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, KubanHorizons.MOD_ID);

    /** Тип структуры дворца. */
    public static final DeferredHolder<StructureType<?>, StructureType<GeniePalaceStructure>> GENIE_PALACE =
            STRUCTURE_TYPES.register("genie_palace", () -> () -> GeniePalaceStructure.CODEC);

    /**
     * Тип части дворца.
     *
     * <p>Регистрируется отложенно, как и всё остальное: прямая запись в
     * {@code BuiltInRegistries} падает с «Registry is already frozen», потому
     * что статический реестр замораживается раньше загрузки мода.</p>
     */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> GENIE_PALACE_PIECE =
            PIECE_TYPES.register("genie_palace",
                    () -> (StructurePieceType.ContextlessType) GeniePalacePiece::new);

    private KHPalaceStructures() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        PIECE_TYPES.register(modBus);
    }
}
