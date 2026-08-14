package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.blockentity.OilPressBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Регистрация block entities мода.
 */
public final class KHBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KubanHorizons.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilPressBlockEntity>> OIL_PRESS =
            BLOCK_ENTITIES.register("oil_press",
                    () -> new BlockEntityType<>(OilPressBlockEntity::new, KHBlocks.OIL_PRESS.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.romankrukovsky.kubanhorizons.blockentity.HandMillBlockEntity>> HAND_MILL =
            BLOCK_ENTITIES.register("hand_mill",
                    () -> new BlockEntityType<>(dev.romankrukovsky.kubanhorizons.blockentity.HandMillBlockEntity::new,
                            KHBlocks.HAND_MILL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.romankrukovsky.kubanhorizons.blockentity.DryingRackBlockEntity>> DRYING_RACK =
            BLOCK_ENTITIES.register("drying_rack",
                    () -> new BlockEntityType<>(dev.romankrukovsky.kubanhorizons.blockentity.DryingRackBlockEntity::new,
                            KHBlocks.DRYING_RACK.get()));

<<<<<<< Updated upstream
=======
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.romankrukovsky.kubanhorizons.blockentity.CuttingBoardBlockEntity>> CUTTING_BOARD =
            BLOCK_ENTITIES.register("cutting_board",
                    () -> new BlockEntityType<>(dev.romankrukovsky.kubanhorizons.blockentity.CuttingBoardBlockEntity::new,
                            KHBlocks.CUTTING_BOARD.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.romankrukovsky.kubanhorizons.blockentity.SmokehouseBlockEntity>> SMOKEHOUSE =
            BLOCK_ENTITIES.register("smokehouse",
                    () -> new BlockEntityType<>(dev.romankrukovsky.kubanhorizons.blockentity.SmokehouseBlockEntity::new,
                            KHBlocks.SMOKEHOUSE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.romankrukovsky.kubanhorizons.blockentity.GrapePressBlockEntity>> GRAPE_PRESS =
            BLOCK_ENTITIES.register("grape_press",
                    () -> new BlockEntityType<>(dev.romankrukovsky.kubanhorizons.blockentity.GrapePressBlockEntity::new,
                            KHBlocks.GRAPE_PRESS.get()));

>>>>>>> Stashed changes
    private KHBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
