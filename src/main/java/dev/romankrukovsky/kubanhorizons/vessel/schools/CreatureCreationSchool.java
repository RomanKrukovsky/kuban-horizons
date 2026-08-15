package dev.romankrukovsky.kubanhorizons.vessel.schools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Школа кувшина — создание существ (CREATURE_CREATION).
 *
 * <p>Переключается по кругу между спутниками: ручной волк, светлячок-эллай
 * и овца-компаньон. Созданные существа не враждебны и следуют за владельцем.</p>
 */
public final class CreatureCreationSchool implements VesselSchool {

    private static final String COMPANION_KEY = "JugCompanion";

    private enum Companion {
        WOLF, ALLAY, SHEEP
    }

    @Override
    public String cast(ServerLevel level, ServerPlayer owner, ItemStack stack) {
        Companion next = nextCompanion(stack);
        BlockPos pos = owner.blockPosition().above();
        switch (next) {
            case WOLF -> {
                Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
                if (wolf != null) {
                    wolf.setTame(true, true);
                    wolf.setOwner(owner);
                    spawn(level, wolf, pos);
                }
            }
            case ALLAY -> {
                Allay allay = EntityTypes.ALLAY.create(level, EntitySpawnReason.COMMAND);
                if (allay != null) {
                    spawn(level, allay, pos);
                }
            }
            case SHEEP -> {
                Sheep sheep = EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND);
                if (sheep != null) {
                    spawn(level, sheep, pos);
                }
            }
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.3F);
        storeCompanion(stack, next);
        return null;
    }

    private static void spawn(ServerLevel level, net.minecraft.world.entity.Mob mob, BlockPos pos) {
        mob.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        level.addFreshEntity(mob);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                20, 0.5D, 0.5D, 0.5D, 0.05D);
    }

    private static Companion nextCompanion(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String name = tag.getStringOr(COMPANION_KEY, "");
        Companion current;
        try {
            current = name.isEmpty() ? Companion.WOLF : Companion.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            current = Companion.WOLF;
        }
        return Companion.values()[(current.ordinal() + 1) % Companion.values().length];
    }

    private static void storeCompanion(ItemStack stack, Companion companion) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(COMPANION_KEY, companion.name());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
}