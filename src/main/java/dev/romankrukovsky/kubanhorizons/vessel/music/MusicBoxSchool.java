package dev.romankrukovsky.kubanhorizons.vessel.music;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Locale;

/**
 * Школа эмоций и атмосферы музыкальной шкатулки.
 *
 * <p>Каждое использование шкатулки переключает настроение по кругу
 * ({@link Mood}), и каждое настроение меняет мир вокруг владельца:
 * успокаивает враждебных мобов, дарит спешку, гасит огонь или обжигает
 * холодом. Это не заклинание с метанием — это аура, следующая за игроком.</p>
 */
public final class MusicBoxSchool {

    private static final String MOOD_KEY = "MusicBoxMood";

    public enum Mood {
        CALM, JOY, SADNESS, AWE;

        public String translationKey() {
            return "mood.kubanhorizons.music." + name().toLowerCase(Locale.ROOT);
        }
    }

    private MusicBoxSchool() {
    }

    /** Следующее настроение по кругу; начальное — CALM. */
    public static Mood nextMood(ItemStack stack) {
        Mood current = currentMood(stack);
        return Mood.values()[(current.ordinal() + 1) % Mood.values().length];
    }

    public static Mood currentMood(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String name = tag.getStringOr(MOOD_KEY, "");
        if (name.isEmpty()) {
            return Mood.CALM;
        }
        try {
            return Mood.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Mood.CALM;
        }
    }

    public static void storeMood(ItemStack stack, Mood mood) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(MOOD_KEY, mood.name());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    /** Применяет настроение вокруг владельца. */
    public static void play(ServerLevel level, Player owner, Mood mood) {
        BlockPos center = owner.blockPosition();
        switch (mood) {
            case CALM -> calm(level, owner, center);
            case JOY -> joy(level, owner, center);
            case SADNESS -> sadness(level, owner, center);
            case AWE -> awe(level, owner, center);
        }
        owner.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.music_box.mood", Component.translatable(mood.translationKey())));
    }

    private static void calm(ServerLevel level, Player owner, BlockPos center) {
        level.playSound(null, center, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 0.9F, 0.8F);
        owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        for (Mob hostile : hostileNear(level, center, 16.0D)) {
            hostile.setTarget(null);
        }
        particles(level, center, ParticleTypes.END_ROD, 24, 0.6D);
    }

    private static void joy(ServerLevel level, Player owner, BlockPos center) {
        level.playSound(null, center, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.9F, 1.4F);
        owner.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0));
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(centerCenter(center), 12.0D, 12.0D, 12.0D), e -> e != owner)) {
            nearby.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        }
        particles(level, center, ParticleTypes.HAPPY_VILLAGER, 30, 0.7D);
    }

    private static void sadness(ServerLevel level, Player owner, BlockPos center) {
        level.playSound(null, center, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.9F, 0.6F);
        owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0));
        for (Mob hostile : hostileNear(level, center, 12.0D)) {
            hostile.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
        }
        particles(level, center, ParticleTypes.SMOKE, 20, 0.5D);
    }

    private static void awe(ServerLevel level, Player owner, BlockPos center) {
        level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.6F);
        owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(centerCenter(center), 16.0D, 16.0D, 16.0D), e -> e != owner)) {
            nearby.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        }
        particles(level, center, ParticleTypes.ENCHANT, 36, 0.8D);
    }

    private static List<Mob> hostileNear(ServerLevel level, BlockPos center, double radius) {
        return level.getEntitiesOfClass(Mob.class,
                AABB.ofSize(centerCenter(center), radius, radius, radius),
                mob -> mob.isAlive() && mob instanceof net.minecraft.world.entity.monster.Enemy);
    }

    private static net.minecraft.world.phys.Vec3 centerCenter(BlockPos center) {
        return new net.minecraft.world.phys.Vec3(
                center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
    }

    private static void particles(ServerLevel level, BlockPos center,
                                  net.minecraft.core.particles.ParticleOptions type,
                                  int count, double spread) {
        double x = center.getX() + 0.5D;
        double y = center.getY() + 1.0D;
        double z = center.getZ() + 0.5D;
        level.sendParticles(type, x, y, z, count, spread, spread, spread, 0.04D);
    }
}