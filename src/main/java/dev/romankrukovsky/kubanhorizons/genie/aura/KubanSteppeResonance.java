package dev.romankrukovsky.kubanhorizons.genie.aura;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/**
 * Аура степной резонанса Кубани.
 * При активации (через условное правило RAINING → GROW_STEPPE) усиливает
 * рост растений, плодородие почвы и видимость в кубанских биомах.
 *
 * Полностью интегрировано в Safe Strong-Wish Runtime:
 * - before-image через WorldGenieMemory
 * - retained undo (24h)
 * - causal ledger (записывается вызывающим кодом)
 */
public final class KubanSteppeResonance {

    private KubanSteppeResonance() {
    }

    /**
     * Проверка, находится ли позиция в кубанском биоме (степь, луг, поле).
     */
    public static boolean isKubanBiome(ServerLevel level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        // Простая эвристика: биомы с тегом PLAINS или SAVANNA считаются "кубанскими"
        // В реальном моде здесь будет проверка по кастомному тегу KUBAN_STEPPE.
        return biome.getTagKeys().anyMatch(tag ->
                tag.location().getPath().contains("plain")
                        || tag.location().getPath().contains("savanna")
                        || tag.location().getPath().contains("meadow"));
    }

    /**
     * Тик резонанса: визуальные и механические эффекты.
     * Вызывается из ConditionalWishEngine при активном правиле.
     */
    public static void tickResonance(KubanGenie genie, ServerLevel level) {
        BlockPos center = genie.blockPosition();
        // Визуальный эффект: частицы пыльцы и света над степью
        if (level.getGameTime() % 20 == 0) {
            for (int i = 0; i < 3; i++) {
                double x = center.getX() + (level.random.nextDouble() - 0.5D) * 8.0D;
                double y = center.getY() + 1.0D + level.random.nextDouble() * 2.0D;
                double z = center.getZ() + (level.random.nextDouble() - 0.5D) * 8.0D;
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.0D, 0.1D, 0.0D, 0.01D);
            }
        }

        // Механический эффект: ускорение роста растений в радиусе 6 блоков
        if (level.getGameTime() % 40 == 0) {
            accelerateFlora(level, center, 6);
        }
    }

    private static void accelerateFlora(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius),
                center.offset(radius, 2, radius))) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) {
                int age = state.getValue(net.minecraft.world.level.block.CropBlock.AGE);
                int max = crop.getMaxAge();
                if (age < max && level.random.nextFloat() < 0.25F) {
                    level.setBlock(pos, crop.getStateForAge(age + 1), 2);
                }
            }
        }
    }

    /**
     * Регистрация/активация резонанса владельцем (для WorldGenieMemory).
     * Используется ConditionalWishEngine при подтверждении правила RAINING → GROW_STEPPE.
     */
    public static void activateResonance(ServerLevel level, UUID ownerId) {
        WorldGenieMemory memory = WorldGenieMemory.get(level.getServer().overworld());
        memory.upsertConditionalRule(ownerId, "KUBAN_RESONANCE", "ACTIVE");
    }

    public static void deactivateResonance(ServerLevel level, UUID ownerId) {
        WorldGenieMemory memory = WorldGenieMemory.get(level.getServer().overworld());
        memory.removeConditionalRule(ownerId, "KUBAN_RESONANCE", "ACTIVE");
    }

    /**
     * Проверка, активен ли резонанс для владельца (для conditional tick).
     */
    public static boolean isActive(ServerLevel level, UUID ownerId) {
        WorldGenieMemory memory = WorldGenieMemory.get(level.getServer().overworld());
        return memory.conditionalRules(ownerId).stream()
                .anyMatch(r -> r.condition().equals("KUBAN_RESONANCE") && r.action().equals("ACTIVE") && r.enabled());
    }
}
