package dev.romankrukovsky.kubanhorizons.genie.world;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Снимок региона блоков: единственная точка мода, где область мира
 * сохраняется и возвращается назад.
 *
 * <p>Используется карманными сценами, сжатием мира, театром прошлого и
 * гигантизмом. Объём ограничен конфигурацией — это Закон сохранности из
 * {@code GENIE_VISION.md}: ни одна операция джиннии не может незаметно
 * повесить сервер.</p>
 */
public final class RegionSnapshot {
    /** Версия схемы сериализации (AD-006). */
    public static final int SCHEMA_VERSION = 1;

    private static final String TAG_TEMPLATE = "Template";
    private static final String TAG_SCHEMA = "SchemaVersion";

    private final StructureTemplate template;

    private RegionSnapshot(StructureTemplate template) {
        this.template = template;
    }

    /**
     * Захватывает регион между двумя углами включительно.
     *
     * @return пустое значение, если объём превышает лимит конфигурации
     */
    public static Optional<RegionSnapshot> capture(ServerLevel level, BlockPos from, BlockPos to) {
        BlockPos min = new BlockPos(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()));
        BlockPos max = new BlockPos(
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ()));

        Vec3i size = new Vec3i(
                max.getX() - min.getX() + 1,
                max.getY() - min.getY() + 1,
                max.getZ() - min.getZ() + 1);

        long volume = (long) size.getX() * size.getY() * size.getZ();
        if (volume > KHServerConfig.genieMaxRegionVolume()) {
            return Optional.empty();
        }

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, min, size, true, List.of());
        return Optional.of(new RegionSnapshot(template));
    }

    /** Возвращает блоки на место относительно указанного начала региона. */
    public boolean restore(ServerLevel level, BlockPos origin) {
        return template.placeInWorld(level, origin, origin,
                new StructurePlaceSettings(), level.getRandom(), 3);
    }

    /** Заполняет захваченный объём воздухом. */
    public void clear(ServerLevel level, BlockPos origin) {
        Vec3i size = template.getSize();
        for (BlockPos pos : BlockPos.betweenClosed(origin,
                origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** Размер захваченного региона в блоках. */
    public Vec3i size() {
        return template.getSize();
    }

    /** Сериализация для хранения внутри предмета или SavedData. */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_SCHEMA, SCHEMA_VERSION);
        tag.put(TAG_TEMPLATE, template.save(new CompoundTag()));
        return tag;
    }

    /** Десериализация ранее сохранённого снимка. */
    public static RegionSnapshot fromTag(ServerLevel level, CompoundTag tag) {
        StructureTemplate template = new StructureTemplate();
        template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK),
                tag.getCompoundOrEmpty(TAG_TEMPLATE));
        return new RegionSnapshot(template);
    }
}
