package dev.romankrukovsky.kubanhorizons.entity;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Магический клон-двойник (Magic Player Doppelgänger).
 *
 * <p>Джиннию можно размножить только так. Клон намеренно лишён личности:
 * у него нет {@code GeniePersonality}, {@code GenieBrain},
 * {@code WishborneState} и памяти мира. Он лишь помнит, чьим отражением
 * является, и любое обращение к нему — обращение к настоящей джиннии.
 * Поэтому клонов может быть сколько угодно, а привязанная сущность
 * по-прежнему одна.</p>
 *
 * <p>Клон не переживает урон: он рассеивается, а не защищается. Иначе
 * неуязвимость джиннии копировалась бы вместе с внешностью, и клон стал бы
 * бессмертным щитом.</p>
 */
public class MagicDoppelgangerEntity extends PathfinderMob {
    private static final int SCHEMA_VERSION = 1;

    private UUID sourceGenieId;

    public MagicDoppelgangerEntity(EntityType<? extends PathfinderMob> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    /** Отмечает клона отражением конкретной джиннии. */
    public void reflect(KubanGenie genie) {
        sourceGenieId = genie.getUUID();
    }

    /** UUID джиннии, отражением которой является клон, либо null у обычного двойника игрока. */
    public UUID sourceGenieId() {
        return sourceGenieId;
    }

    /** Является ли клон отражением джиннии, а не двойником игрока. */
    public boolean reflectsGenie() {
        return sourceGenieId != null;
    }

    /** Настоящая джинния, к которой следует адресовать любое взаимодействие с клоном. */
    public KubanGenie source(ServerLevel level) {
        if (sourceGenieId == null) {
            return null;
        }
        return level.getEntityInAnyDimension(sourceGenieId) instanceof KubanGenie genie ? genie : null;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!reflectsGenie()) {
            return super.hurtServer(level, source, amount);
        }
        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.0D, getZ(),
                30, 0.4D, 0.8D, 0.4D, 0.1D);
        discard();
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        if (sourceGenieId != null) {
            output.putString("SourceGenie", sourceGenieId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String raw = input.getStringOr("SourceGenie", "");
        try {
            sourceGenieId = raw.isEmpty() ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            sourceGenieId = null;
        }
    }
}
