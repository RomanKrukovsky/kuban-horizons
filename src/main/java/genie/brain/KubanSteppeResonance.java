package genie.brain;

import genie.entity.GenieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Resonance system for Kuban Steppe biome interactions
 * Provides special effects when genie is in compatible biomes
 */
public class KubanSteppeResonance {
    private final GenieEntity genie;
    private final Random random;
    private boolean isActive = true;
    private int effectCooldown = 0;

    // Biome compatibility
    private static final ResourceKey<Biome> KUBAN_STEPPE = ResourceKey.create(
        Registries.BIOME,
        net.minecraft.resources.ResourceLocation.parse("minecraft:plains")
    );

    private static final ResourceKey<Biome> PLOVNI = ResourceKey.create(
        Registries.BIOME,
        net.minecraft.resources.ResourceLocation.parse("minecraft:swamp")
    );

    private static final ResourceKey<Biome> LIMAN = ResourceKey.create(
        Registries.BIOME,
        net.minecraft.resources.ResourceLocation.parse("minecraft:river")
    );

    private static final ResourceKey<Biome> POYMA = ResourceKey.create(
        Registries.BIOME,
        net.minecraft.resources.ResourceLocation.parse("minecraft:forest")
    );

    // Effect ranges
    private static final double PLANT_GROWTH_RANGE = 8.0;
    private static final double ANIMAL_CALM_RANGE = 12.0;
    private static final double BIOME_EFFECT_RANGE = 16.0;

    // Effect strengths
    private static final float PLANT_GROWTH_STRENGTH = 0.5f;
    private static final float ANIMAL_CALM_STRENGTH = 0.7f;
    private static final int EFFECT_COOLDOWN_TICKS = 100;

    public KubanSteppeResonance(GenieEntity genie) {
        this.genie = genie;
        this.random = new Random(genie.getUUID().getMostSignificantBits());
    }

    /**
     * Tick method called every game tick
     */
    public void tick() {
        if (!isActive || genie.level().isClientSide || effectCooldown > 0) {
            if (effectCooldown > 0) effectCooldown--;
            return;
        }

        // Check biome compatibility
        if (isInCompatibleBiome()) {
            applyBiomeEffects();
            effectCooldown = EFFECT_COOLDOWN_TICKS;
        }
    }

    /**
     * Check if genie is in a compatible biome
     */
    private boolean isInCompatibleBiome() {
        if (!(genie.level() instanceof ServerLevel serverLevel)) return false;

        Holder<Biome> biome = serverLevel.getBiome(genie.blockPosition());
        ResourceKey<Biome> biomeKey = biome.unwrapKey().orElse(null);

        if (biomeKey == null) return false;

        // Check for compatible biomes
        return biomeKey.equals(KUBAN_STEPPE) ||
               biomeKey.equals(PLOVNI) ||
               biomeKey.equals(LIMAN) ||
               biomeKey.equals(POYMA);
    }

    /**
     * Apply biome-specific effects
     */
    private void applyBiomeEffects() {
        // Apply plant growth effect
        growPlants();

        // Calm nearby animals
        calmAnimals();

        // Apply special aura
        applyBiomeAura();

        // Visual effects
        spawnParticles();
    }

    /**
     * Grow plants in a radius around genie
     */
    private void growPlants() {
        if (!(genie.level() instanceof ServerLevel serverLevel)) return;

        BlockPos center = genie.blockPosition();
        int radius = (int) PLANT_GROWTH_RANGE;

        // Check blocks in a spherical area
        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-radius, -radius/2, -radius),
            center.offset(radius, radius/2, radius)
        )) {
            BlockState state = serverLevel.getBlockState(pos);
            Block block = state.getBlock();

            // Grow compatible plants
            if (canGrowPlant(block)) {
                BlockState newState = getGrownPlantState(block, state);
                if (newState != null && newState != state) {
                    serverLevel.setBlock(pos, newState, 3);
                }
            }

            // Spread grass
            if (block == Blocks.DIRT && serverLevel.getBlockState(pos.above()).isAir()) {
                if (random.nextFloat() < 0.1f) {
                    serverLevel.setBlock(pos.above(), Blocks.GRASS.defaultBlockState(), 3);
                }
            }
        }
    }

    /**
     * Check if block can be grown
     */
    private boolean canGrowPlant(Block block) {
        return block == Blocks.WHEAT ||
               block == Blocks.CARROTS ||
               block == Blocks.POTATOES ||
               block == Blocks.BEETROOTS ||
               block == Blocks.PUMPKIN_STEM ||
               block == Blocks.MELON_STEM ||
               block == Blocks.SWEET_BERRY_BUSH ||
               block == Blocks.COCOA ||
               block == Blocks.GRASS_BLOCK ||
               block == Blocks.FARMLAND ||
               block == Blocks.DIRT;
    }

    /**
     * Get grown plant state
     */
    @Nullable
    private BlockState getGrownPlantState(Block block, BlockState current) {
        if (block == Blocks.WHEAT) {
            int age = current.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7);
            if (age < 7) return current.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7, 7);
        }
        else if (block == Blocks.CARROTS) {
            int age = current.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7);
            if (age < 7) return current.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7, 7);
        }
        else if (block == Blocks.POTATOES) {
            int age = current.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7);
            if (age < 7) return current.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7, 7);
        }
        else if (block == Blocks.BEETROOTS) {
            int age = current.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_3);
            if (age < 3) return current.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_3, 3);
        }
        else if (block == Blocks.SWEET_BERRY_BUSH) {
            int age = current.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7);
            if (age < 3) return current.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7, Math.min(3, age + 2));
        }
        else if (block == Blocks.GRASS_BLOCK && current.getBlock() == Blocks.DIRT) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        else if (block == Blocks.DIRT) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }

        return null;
    }

    /**
     * Calm nearby animals
     */
    private void calmAnimals() {
        if (!(genie.level() instanceof ServerLevel serverLevel)) return;

        AABB area = genie.getBoundingBox().inflate(ANIMAL_CALM_RANGE);
        List<Animal> animals = serverLevel.getEntitiesOfClass(Animal.class, area);

        for (Animal animal : animals) {
            // Skip hostile animals
            if (animal instanceof net.minecraft.world.entity.monster.Monster) continue;

            // Calm effect
            if (!animal.hasEffect(MobEffects.REGENERATION)) {
                animal.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    200,
                    0,
                    false,
                    false
                ));
            }

            // Make animals friendly to owner
            if (genie.getOwner() != null) {
                // Animals naturally follow genie
                if (animal.getTarget() == genie.getOwner()) {
                    animal.setTarget(null);
                }
            }
        }

        // Special handling for horses
        List<Horse> horses = serverLevel.getEntitiesOfClass(Horse.class, area);
        for (Horse horse : horses) {
            if (horse instanceof SkeletonHorse || horse instanceof ZombieHorse) {
                // Don't calm undead horses
                continue;
            }

            // Make horses calm and rideable
            if (horse.isVehicle() || horse.isTamed()) {
                // Already calm
                continue;
            }

            // Chance to tame nearby horses
            if (random.nextFloat() < 0.01f) {
                horse.tame(genie.getOwner() instanceof net.minecraft.world.entity.player.Player ?
                    (net.minecraft.world.entity.player.Player) genie.getOwner() : null);
            }
        }
    }

    /**
     * Apply special aura effect to genie and nearby entities
     */
    private void applyBiomeAura() {
        // Give genie regeneration
        if (!genie.hasEffect(MobEffects.REGENERATION)) {
            genie.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                300,
                1,
                false,
                true
            ));
        }

        // Give owner regeneration if nearby
        if (genie.getOwner() instanceof LivingEntity owner) {
            if (!owner.hasEffect(MobEffects.REGENERATION)) {
                owner.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    300,
                    0,
                    false,
                    true
                ));
            }
        }

        // Slow falling effect
        if (!genie.hasEffect(MobEffects.SLOW_FALLING)) {
            genie.addEffect(new MobEffectInstance(
                MobEffects.SLOW_FALLING,
                200,
                0,
                false,
                true
            ));
        }
    }

    /**
     * Spawn decorative particles
     */
    private void spawnParticles() {
        if (genie.level().isClientSide) {
            // Client-side particle effects
            double x = genie.getX() + (random.nextDouble() - 0.5) * 2.0;
            double y = genie.getY() + random.nextDouble() * 2.0;
            double z = genie.getZ() + (random.nextDouble() - 0.5) * 2.0;

            genie.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                x, y, z,
                0.0, 0.1, 0.0
            );
        }
    }

    /**
     * Check if genie is in a specific biome type
     */
    public boolean isInBiome(ResourceKey<Biome> biomeKey) {
        if (!(genie.level() instanceof ServerLevel serverLevel)) return false;

        Holder<Biome> biome = serverLevel.getBiome(genie.blockPosition());
        return biome.unwrapKey().map(key -> key.equals(biomeKey)).orElse(false);
    }

    /**
     * Get current biome resonance strength (0.0-1.0)
     */
    public float getResonanceStrength() {
        if (!isInCompatibleBiome()) return 0.0f;
        return 1.0f;
    }

    /**
     * Get resonance description
     */
    public String getResonanceDescription() {
        if (!isActive) return "Resonance inactive";
        if (!isInCompatibleBiome()) return "Not in compatible biome";

        return "Kuban Steppe resonance active: Plants grow, animals calm, aura protects";
    }

    /**
     * Set active state
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Get plant growth range
     */
    public double getPlantGrowthRange() {
        return PLANT_GROWTH_RANGE;
    }

    /**
     * Get animal calm range
     */
    public double getAnimalCalmRange() {
        return ANIMAL_CALM_RANGE;
    }
}