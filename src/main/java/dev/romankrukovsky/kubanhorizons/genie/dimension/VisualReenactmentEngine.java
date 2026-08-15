package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Театр реальности — визуальная реконструкция прошлых событий.
 *
 * <p>Джинния находит ближайшее запомненное событие из {@link WorldGenieMemory}
 * и «переигрывает» его: магическая подпись, частицы по типу события и краткая
 * реплика о том, что здесь произошло. Реконструкция не меняет мир — это
 * голограмма прошлого, а не восстановление.</p>
 */
public final class VisualReenactmentEngine {
    private VisualReenactmentEngine() {
    }

    public static boolean reenactPastEvent(ServerLevel level, BlockPos origin, Player player) {
        WorldGenieMemory memory = WorldGenieMemory.get(level);
        Optional<WorldGenieMemory.MemoryEntry> event = memory.findNearbyMemory(origin, 24.0D);

        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(origin));

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, origin.getX() + 0.5D, origin.getY() + 1.2D, origin.getZ() + 0.5D,
                50, 0.8D, 1.2D, 0.8D, 0.05D);
        level.sendParticles(ParticleTypes.PORTAL, origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 0.5D,
                30, 0.6D, 0.8D, 0.6D, 0.03D);

        if (event.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.theater_empty"));
            return false;
        }

        WorldGenieMemory.MemoryEntry entry = event.get();
        BlockPos scenePos = entry.pos();
        reenactByType(level, scenePos, entry.type(), player);
        return true;
    }

    private static void reenactByType(ServerLevel level, BlockPos pos, String type, Player player) {
        switch (type == null ? "" : type) {
            case "wish" -> {
                level.sendParticles(ParticleTypes.ENCHANT,
                        pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        60, 0.9D, 1.0D, 0.9D, 0.08D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.theater_wish"));
            }
            case "rescue" -> {
                level.sendParticles(ParticleTypes.HEART,
                        pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                        40, 0.6D, 0.8D, 0.6D, 0.06D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.theater_rescue"));
            }
            case "village" -> {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        50, 1.2D, 0.8D, 1.2D, 0.05D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.theater_village"));
            }
            default -> {
                level.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        30, 0.5D, 0.8D, 0.5D, 0.03D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.theater_reenactment"));
            }
        }
    }
}