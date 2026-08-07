package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Осётр: крупная речная рыба поймы и лиманов.
 *
 * <p>Роль в экосистеме — заметная добыча рыболовства и сырьё коптильни. Ходит
 * малым косяком: крупная рыба не собирается в плотные стаи, поэтому
 * {@link #getMaxSchoolSize()} меньше ванильного, и встреча с осетром читается
 * как находка, а не как проплывающая мимо треска.</p>
 */
public final class Sturgeon extends AbstractSchoolingFish {
    public Sturgeon(EntityType<? extends Sturgeon> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSchoolingFish.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.9D);
    }

    /** Малый косяк: осётр — крупная рыба, а не стайная мелочь. */
    @Override
    public int getMaxSchoolSize() {
        return 4;
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(KHItems.STURGEON_BUCKET.get());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KHSounds.STURGEON_FLOP.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KHSounds.STURGEON_FLOP.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KHSounds.STURGEON_FLOP.get();
    }

    @Override
    protected SoundEvent getFlopSound() {
        return KHSounds.STURGEON_FLOP.get();
    }
}
