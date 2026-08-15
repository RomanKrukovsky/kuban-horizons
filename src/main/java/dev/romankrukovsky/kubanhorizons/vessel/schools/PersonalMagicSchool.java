package dev.romankrukovsky.kubanhorizons.vessel.schools;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Школа кольца — личная магия владельца (PERSONAL_MAGIC).
 *
 * <p>Кольцо усиливает владельца: переключается по кругу между стойкостью,
 * стремительностью и могуществом. В отличие от зеркала и шкатулки, эффект
 * привязан только к игроку и не влияет на окружение.</p>
 */
public final class PersonalMagicSchool implements VesselSchool {

    private static final String BOON_KEY = "RingBoon";

    private enum Boon {
        RESILIENCE, SWIFTNESS, MIGHT
    }

    @Override
    public String cast(ServerLevel level, ServerPlayer owner, ItemStack stack) {
        Boon next = nextBoon(stack);
        switch (next) {
            case RESILIENCE -> {
                owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 0));
                owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 0));
            }
            case SWIFTNESS -> {
                owner.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 1));
                owner.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 400, 1));
            }
            case MIGHT -> {
                owner.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 400, 1));
                owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 0));
            }
        }
        level.playSound(null, owner.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.7F, 1.5F);
        storeBoon(stack, next);
        return null;
    }

    private static Boon nextBoon(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String name = tag.getStringOr(BOON_KEY, "");
        Boon current;
        try {
            current = name.isEmpty() ? Boon.RESILIENCE : Boon.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            current = Boon.RESILIENCE;
        }
        return Boon.values()[(current.ordinal() + 1) % Boon.values().length];
    }

    private static void storeBoon(ItemStack stack, Boon boon) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(BOON_KEY, boon.name());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
}