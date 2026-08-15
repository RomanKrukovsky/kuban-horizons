package dev.romankrukovsky.kubanhorizons.genie.music;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Магические песни кубанской джиннии.
 *
 * <p>Музыка и танец — язык изменения мира (GENIE_VISION): каждое движение
 * игрока, сложившееся в фигуру танца, вызывает песню, которая переписывает
 * реальность вокруг исполнителя. Песня звучит во время исполнения и держит
 * свой эффект всю длительность {@link #durationTicks()}.</p>
 */
public enum MusicSpell {
    RAIN_SONG(SoundEvents.WEATHER_RAIN, 600) {
        @Override
        public void apply(ServerLevel level, BlockPos center, ServerPlayer caster) {
            level.setRainLevel(1.0F);
            level.sendParticles(ParticleTypes.RAIN, center.getX() + 0.5D, center.getY() + 18.0D,
                    center.getZ() + 0.5D, 50, 8.0D, 6.0D, 8.0D, 0.0D);
        }
    },
    GROWTH_MELODY(SoundEvents.BONE_MEAL_USE, 200) {
        @Override
        public void apply(ServerLevel level, BlockPos center, ServerPlayer caster) {
            bonemealRandomCrop(level, center, caster);
        }
    },
    PEACE_LULLABY(SoundEvents.NOTE_BLOCK_HARP.value(), 300) {
        @Override
        public void apply(ServerLevel level, BlockPos center, ServerPlayer caster) {
            calmHostiles(level, center, caster);
        }
    },
    DANCE_OF_FIRE(SoundEvents.FIRECHARGE_USE, 300) {
        @Override
        public void apply(ServerLevel level, BlockPos center, ServerPlayer caster) {
            shieldFromFire(level, center, caster);
        }
    };

    private final SoundEvent sound;
    private final int durationTicks;

    MusicSpell(SoundEvent sound, int durationTicks) {
        this.sound = sound;
        this.durationTicks = durationTicks;
    }

    public SoundEvent sound() {
        return sound;
    }

    public int durationTicks() {
        return durationTicks;
    }

    /** Применяет мировой эффект песни; вызывается при касте и каждый тик длительности. */
    public abstract void apply(ServerLevel level, BlockPos center, ServerPlayer caster);

    private static void bonemealRandomCrop(ServerLevel level, BlockPos center, ServerPlayer caster) {
        List<BlockPos> crops = new ArrayList<>();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos candidate = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(candidate);
                    if (state.getBlock() instanceof CropBlock) {
                        crops.add(candidate);
                    }
                }
            }
        }
        if (crops.isEmpty()) {
            return;
        }
        BlockPos target = crops.get(level.getRandom().nextInt(crops.size()));
        BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, target, caster);
    }

    private static void calmHostiles(ServerLevel level, BlockPos center, ServerPlayer caster) {
        AABB area = new AABB(center).inflate(12.0D, 4.0D, 12.0D);
        for (LivingEntity hostile : level.getEntities(EntityTypeTest.forClass(LivingEntity.class), area,
                entity -> entity instanceof Enemy && entity.isAlive())) {
            if (hostile instanceof Mob mob) {
                mob.setTarget(null);
            }
            hostile.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), caster);
        }
    }

    private static void shieldFromFire(ServerLevel level, BlockPos center, ServerPlayer caster) {
        AABB area = new AABB(center).inflate(8.0D, 4.0D, 8.0D);
        for (LivingEntity nearby : level.getEntities(EntityTypeTest.forClass(LivingEntity.class), area,
                entity -> entity.isAlive() && entity != caster)) {
            nearby.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0), caster);
        }
    }
}
