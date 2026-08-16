package dev.romankrukovsky.kubanhorizons.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Желание, ставшее существом (GENIE_VISION §Существа).
 *
 * <p>Джинния материализует суть желания в виде маленького светящегося
 * спутника. Существо хранит формулировку желания и следует за создателем,
 * напоминая ему, что было пожелано. Пока оно живо — желание «помнится».</p>
 */
public class WishCreatureEntity extends PathfinderMob {

    private static final int SCHEMA_VERSION = 1;

    private String wishText = "";
    private java.util.UUID ownerId;

    public WishCreatureEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public void setWish(String text, java.util.UUID owner) {
        this.wishText = text == null ? "" : text;
        this.ownerId = owner;
        if (!level().isClientSide()) {
            this.setCustomName(Component.literal("§d" + (text.isBlank() ? "Желание" : text)));
        }
    }

    public String wishText() {
        return wishText;
    }

    public java.util.UUID ownerId() {
        return ownerId;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && tickCount % 10 == 0) {
            server.sendParticles(ParticleTypes.ENCHANT,
                    getX(), getY() + 0.5D, getZ(),
                    3, 0.2D, 0.3D, 0.2D, 0.05D);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false; // создание из желания неуязвимо — оно исчезает само
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putString("WishText", wishText);
        if (ownerId != null) {
            output.putString("OwnerId", ownerId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        wishText = input.getStringOr("WishText", "");
        String raw = input.getStringOr("OwnerId", "");
        try {
            ownerId = raw.isEmpty() ? null : java.util.UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            ownerId = null;
        }
    }
}