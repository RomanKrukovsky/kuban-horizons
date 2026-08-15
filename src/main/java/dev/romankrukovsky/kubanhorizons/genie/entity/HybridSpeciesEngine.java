package dev.romankrukovsky.kubanhorizons.genie.entity;

import com.mojang.serialization.Codec;
import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.ecology.Genome;
import dev.romankrukovsky.kubanhorizons.genie.ecology.PopulationControl;
import dev.romankrukovsky.kubanhorizons.genie.ecology.Trait;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Синтез гибридных видов существ и эволюционный дрейф (Hybrid Speciation & Evolution Engine). */
public final class HybridSpeciesEngine {
    public static final String GENOME_TAG = "GenomeTag";
    private static final String REPRODUCE_COOLDOWN_TAG = "KubanReproduceCooldown";
    private static final long REPRODUCE_COOLDOWN_TICKS = 6000L;

    private HybridSpeciesEngine() {
    }

    public static LivingEntity synthesizeHybrid(ServerLevel level, Vec3 pos, String hybridType) {
        LivingEntity entity = EntityTypes.FOX.create(level, EntitySpawnReason.COMMAND);
        if (entity != null) {
            entity.snapTo(pos.x(), pos.y(), pos.z(), 0.0F, 0.0F);
            String normalized = hybridType == null ? "" : hybridType.toLowerCase(java.util.Locale.ROOT);
            boolean flying = normalized.contains("fly") || normalized.contains("лет");
            boolean glowing = normalized.contains("glow") || normalized.contains("свет");
            entity.setNoGravity(flying);
            entity.setGlowingTag(glowing);
            entity.getPersistentData().putBoolean("KubanHybrid", true);
            entity.getPersistentData().putString("KubanHybridTraits", normalized);
            var movement = entity.getAttribute(Attributes.FLYING_SPEED);
            if (flying && movement != null) {
                movement.setBaseValue(0.25D);
            }
            level.addFreshEntity(entity);
            MagicalSignature.cast(level, pos);
        }
        return entity;
    }

    /**
     * Размножение двух гибридов: менделевское скрещивание геномов и рождение
     * потомка с применёнными признаками.
     *
     * <p>Возвращает {@code null}, если родители не являются гибридами (нет генома),
     * ещё не остыли после предыдущего размножения или популяционный контроль чанка
     * достиг лимита {@link KHServerConfig#hybridPopulationCapPerChunk()}.</p>
     */
    public static Mob tryReproduce(ServerLevel level, Mob parentA, Mob parentB) {
        Genome genomeA = getGenome(parentA);
        Genome genomeB = getGenome(parentB);
        if (genomeA == null || genomeB == null) {
            return null;
        }
        if (level.getGameTime() < Math.max(
                parentA.getPersistentData().getLongOr(REPRODUCE_COOLDOWN_TAG, 0L),
                parentB.getPersistentData().getLongOr(REPRODUCE_COOLDOWN_TAG, 0L))) {
            return null;
        }
        ChunkPos chunk = ChunkPos.containing(parentA.blockPosition());
        if (!PopulationControl.get(level).canSpawn(chunk, KHServerConfig.hybridPopulationCapPerChunk())) {
            return null;
        }

        Genome offspringGenome = Genome.combine(genomeA, genomeB, level.getRandom());
        LivingEntity created = EntityTypes.FOX.create(level, EntitySpawnReason.BREEDING);
        if (!(created instanceof Mob offspring)) {
            return null;
        }
        Vec3 pos = parentA.position().add(0.0D, 1.0D, 0.0D);
        offspring.snapTo(pos.x(), pos.y(), pos.z(), 0.0F, 0.0F);
        setGenome(offspring, offspringGenome);
        offspring.getPersistentData().putBoolean("KubanHybrid", true);
        applyTraits(offspring, offspringGenome);
        level.addFreshEntity(offspring);
        PopulationControl.get(level).registerSpawn(chunk);

        long cooldownUntil = level.getGameTime() + REPRODUCE_COOLDOWN_TICKS;
        parentA.getPersistentData().putLong(REPRODUCE_COOLDOWN_TAG, cooldownUntil);
        parentB.getPersistentData().putLong(REPRODUCE_COOLDOWN_TAG, cooldownUntil);
        MagicalSignature.cast(level, pos);
        return offspring;
    }

    /** Геном гибрида из NBT сущности; {@code null}, если генома нет. */
    public static Genome getGenome(Mob entity) {
        var tag = entity.getPersistentData().getCompound(GENOME_TAG);
        if (tag.isEmpty()) {
            return null;
        }
        return Genome.CODEC.parse(NbtOps.INSTANCE, tag.get()).result().orElse(null);
    }

    public static void setGenome(Mob entity, Genome genome) {
        Genome.CODEC.encodeStart(NbtOps.INSTANCE, genome).result()
                .ifPresent(tag -> entity.getPersistentData().put(GENOME_TAG, tag));
    }

    /** Применяет игровые проявления признаков к сущности потомка. */
    private static void applyTraits(Mob entity, Genome genome) {
        boolean flying = genome.has(Trait.FLIGHT);
        entity.setNoGravity(flying);
        if (flying) {
            var flyingSpeed = entity.getAttribute(Attributes.FLYING_SPEED);
            if (flyingSpeed != null) {
                flyingSpeed.setBaseValue(0.25D);
            }
        }
        if (genome.has(Trait.GLOWING)) {
            entity.setGlowingTag(true);
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0));
        }
        if (genome.has(Trait.FIRE_RESISTANT)) {
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
        }
        if (genome.has(Trait.AQUATIC)) {
            entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, Integer.MAX_VALUE, 0));
        }
        if (genome.has(Trait.FAST)) {
            var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * 1.6D);
            }
        }
        if (genome.has(Trait.STRONG)) {
            var damage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(damage.getBaseValue() * 1.6D);
            }
        }
        if (genome.has(Trait.SIZE_LARGE)) {
            var scale = entity.getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.setBaseValue(scale.getBaseValue() * 1.4D);
            }
        }
    }
}
