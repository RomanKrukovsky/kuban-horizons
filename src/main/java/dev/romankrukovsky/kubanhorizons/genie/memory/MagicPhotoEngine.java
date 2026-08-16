package dev.romankrukovsky.kubanhorizons.genie.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Магическая фотография — застывший вид сцены перед игроком.
 *
 * <p>Джинния «фотографирует» область, на которую смотрит игрок: собирает
 * описания блоков и существ и записывает их в предмет-фото. Фото можно
 * назвать переименованием, а при желании «проявить» — показать, что было
 * запечатлено.</p>
 */
public final class MagicPhotoEngine {

    private static final String SCENE_KEY = "MagicPhotoScene";
    private static final int SNAPSHOT_RADIUS = 5;

    private MagicPhotoEngine() {
    }

    /** Делает снимок сцены перед игроком и кладёт фото в его руку. */
    public static ItemStack capture(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        List<String> lines = new java.util.ArrayList<>();

        int blockCount = 0;
        for (int dx = -SNAPSHOT_RADIUS; dx <= SNAPSHOT_RADIUS; dx++) {
            for (int dz = -SNAPSHOT_RADIUS; dz <= SNAPSHOT_RADIUS; dz++) {
                BlockPos pos = origin.offset(dx, 0, dz);
                var state = level.getBlockState(pos);
                if (!state.isAir()) {
                    blockCount++;
                }
            }
        }
        lines.add("blocks: " + blockCount);

        List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(SNAPSHOT_RADIUS),
                e -> e != player && e.isAlive());
        for (LivingEntity mob : mobs) {
            lines.add("creature: " + mob.getType().toString());
        }
        if (mobs.isEmpty()) {
            lines.add("creature: none");
        }

        ItemStack photo = new ItemStack(
                dev.romankrukovsky.kubanhorizons.registry.KHItems.MAGIC_PHOTO.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(SCENE_KEY, String.join("\n", lines));
        CustomData.set(DataComponents.CUSTOM_DATA, photo, tag);
        photo.set(DataComponents.CUSTOM_NAME,
                Component.literal("Фото: " + formatLocation(origin)));
        return photo;
    }

    /** Возвращает записанное описание сцены, или null если это не фото. */
    public static String sceneOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || !stack.is(dev.romankrukovsky.kubanhorizons.registry.KHItems.MAGIC_PHOTO.get())) {
            return null;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getStringOr(SCENE_KEY, null);
    }

    private static String formatLocation(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }
}