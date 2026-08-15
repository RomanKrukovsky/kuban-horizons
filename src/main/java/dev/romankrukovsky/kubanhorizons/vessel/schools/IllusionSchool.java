package dev.romankrukovsky.kubanhorizons.vessel.schools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

/**
 * Школа зеркала — иллюзии и альтернативные миры (ILLUSION_ALTERNATE).
 *
 * <p>Каждое использование переключает иллюзию по кругу: призрачный двойник,
 * невидимость владельца, спокойствие враждебных мобов. Иллюзии не трогают
 * настоящий мир — только восприятие вокруг владельца.</p>
 */
public final class IllusionSchool implements VesselSchool {

    private static final String ILLUSION_KEY = "MirrorIllusion";

    private enum Illusion {
        MIRAGE, INVISIBLE, PACIFY
    }

    @Override
    public String cast(ServerLevel level, ServerPlayer owner, ItemStack stack) {
        Illusion next = nextIllusion(stack);
        switch (next) {
            case MIRAGE -> {
                BlockPos pos = owner.blockPosition();
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
                level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        40, 0.5D, 0.8D, 0.5D, 0.03D);
            }
            case INVISIBLE -> {
                owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
                level.playSound(null, owner.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.7F, 1.2F);
            }
            case PACIFY -> {
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                        AABB.ofSize(owner.position(), 16.0D, 16.0D, 16.0D),
                        e -> e instanceof net.minecraft.world.entity.monster.Enemy)) {
                    nearby.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                }
                level.playSound(null, owner.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(),
                        SoundSource.PLAYERS, 0.8F, 0.9F);
            }
        }
        storeIllusion(stack, next);
        return null;
    }

    private static Illusion nextIllusion(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String name = tag.getStringOr(ILLUSION_KEY, "");
        Illusion current;
        try {
            current = name.isEmpty() ? Illusion.MIRAGE : Illusion.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            current = Illusion.MIRAGE;
        }
        return Illusion.values()[(current.ordinal() + 1) % Illusion.values().length];
    }

    private static void storeIllusion(ItemStack stack, Illusion illusion) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(ILLUSION_KEY, illusion.name());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
}