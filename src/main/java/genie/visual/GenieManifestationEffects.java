package genie.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Particle and visual effects system for genie manifestations
 * Handles all visual effects when genie is manifested
 */
public class GenieManifestationEffects {

    private static final int MAX_PARTICLES = 100;
    private static final float PARTICLE_DISTANCE = 2.0f;

    // Effect configuration
    private float manifestationParticleDensity = 1.0f;
    private float manifestationParticleSpeed = 0.1f;
    private float auraIntensity = 0.5f;
    private boolean enableAura = true;
    private boolean enableParticles = true;
    private boolean enableGlow = true;

    /**
     * Spawn manifestation particles around an entity
     */
    @OnlyIn(Dist.CLIENT)
    public void spawnManifestationParticles(LivingEntity entity, float manifestationLevel,
                                           float[] manifestationColor) {
        if (!enableParticles) return;

        Level level = entity.level();
        if (level.isClientSide && Minecraft.getInstance().player != null) {
            ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;

            // Calculate number of particles based on manifestation level
            int particleCount = (int) (manifestationLevel * manifestationParticleDensity * 2);
            particleCount = Math.min(particleCount, MAX_PARTICLES);

            // Spawn particles in a sphere around the entity
            for (int i = 0; i < particleCount; i++) {
                // Calculate position on sphere
                float theta = entity.getRandom().nextFloat() * (float) Math.PI * 2;
                float phi = (float) Math.acos(2 * entity.getRandom().nextFloat() - 1);
                float radius = entity.getBbWidth() * 0.5f + entity.getRandom().nextFloat() * PARTICLE_DISTANCE;

                float x = (float) (radius * Math.sin(phi) * Math.cos(theta));
                float y = (float) (radius * Math.sin(phi) * Math.sin(theta));
                float z = (float) (radius * Math.cos(phi));

                // Add entity position
                x += entity.getX();
                y += entity.getY() + entity.getBbHeight() * 0.5f;
                z += entity.getZ();

                // Create particle
                Particle particle = particleEngine.createParticle(
                    ParticleTypes.ENTITY_EFFECT,
                    x, y, z,
                    manifestationColor[0],
                    manifestationColor[1],
                    manifestationColor[2]
                );

                if (particle != null) {
                    // Set particle properties
                    particle.setPower(manifestationParticleSpeed);
                    particle.setLifetime(40 + entity.getRandom().nextInt(20));
                    particle.scale(0.5f + entity.getRandom().nextFloat() * 0.3f);
                }
            }
        }
    }

    /**
     * Spawn tail particles
     */
    @OnlyIn(Dist.CLIENT)
    public void spawnTailParticles(LivingEntity entity, GenieTailState tailState) {
        if (!enableParticles || tailState == null || !tailState.isVisible()) return;

        Level level = entity.level();
        if (level.isClientSide && Minecraft.getInstance().player != null) {
            ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;

            int segments = tailState.getLength();
            float particleCount = segments * tailState.getSwaySpeed() * manifestationParticleDensity;

            for (int i = 0; i < particleCount; i++) {
                float progress = (float) i / particleCount;
                float x = (float) (entity.getX() + (entity.getRandom().nextFloat() - 0.5f) * 0.5f);
                float y = (float) (entity.getY() + entity.getRandom().nextFloat() * entity.getBbHeight());
                float z = (float) (entity.getZ() + (entity.getRandom().nextFloat() - 0.5f) * 0.5f);

                Particle particle = particleEngine.createParticle(
                    ParticleTypes.ENTITY_EFFECT,
                    x, y, z,
                    tailState.getRed(),
                    tailState.getGreen(),
                    tailState.getBlue()
                );

                if (particle != null) {
                    particle.setPower(0.05f);
                    particle.setLifetime(30 + entity.getRandom().nextInt(20));
                    particle.scale(0.3f + entity.getRandom().nextFloat() * 0.2f);
                }
            }
        }
    }

    /**
     * Create manifestation aura around entity
     */
    @OnlyIn(Dist.CLIENT)
    public void createManifestationAura(LivingEntity entity, float manifestationLevel,
                                       float[] manifestationColor) {
        if (!enableAura) return;

        Level level = entity.level();
        if (level.isClientSide && Minecraft.getInstance().player != null) {
            // Calculate aura size based on manifestation level
            float auraSize = 0.5f + manifestationLevel * 0.2f;
            float auraAlpha = Mth.clamp(manifestationLevel * 0.02f * auraIntensity, 0.1f, 0.8f);

            // In actual implementation, this would render a glowing aura
            // using custom rendering or shader effects
            // For now, we'll simulate with particles

            ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
            int particleCount = (int) (20 * manifestationLevel * auraIntensity);

            for (int i = 0; i < particleCount; i++) {
                float angle = entity.getRandom().nextFloat() * (float) Math.PI * 2;
                float distance = entity.getBbWidth() * 0.7f + entity.getRandom().nextFloat() * auraSize;

                float x = (float) (entity.getX() + Math.cos(angle) * distance);
                float y = (float) (entity.getY() + entity.getBbHeight() * 0.5f +
                          Math.sin(angle * 2) * distance * 0.3f);
                float z = (float) (entity.getZ() + Math.sin(angle) * distance);

                Particle particle = particleEngine.createParticle(
                    ParticleTypes.ENTITY_EFFECT,
                    x, y, z,
                    manifestationColor[0] * 0.7f,
                    manifestationColor[1] * 0.7f,
                    manifestationColor[2] * 0.7f
                );

                if (particle != null) {
                    particle.setPower(0.02f);
                    particle.setLifetime(60);
                    particle.scale(0.8f + entity.getRandom().nextFloat() * 0.4f);
                }
            }
        }
    }

    /**
     * Spawn transformation particles when switching states
     */
    @OnlyIn(Dist.CLIENT)
    public void spawnTransformationParticles(LivingEntity entity, float[] fromColor,
                                             float[] toColor, boolean manifested) {
        if (!enableParticles) return;

        Level level = entity.level();
        if (level.isClientSide && Minecraft.getInstance().player != null) {
            ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;

            int particleCount = 50 + entity.getRandom().nextInt(30);

            for (int i = 0; i < particleCount; i++) {
                float x = (float) (entity.getX() + (entity.getRandom().nextFloat() - 0.5f) * 2.0f);
                float y = (float) (entity.getY() + entity.getRandom().nextFloat() * entity.getBbHeight());
                float z = (float) (entity.getZ() + (entity.getRandom().nextFloat() - 0.5f) * 2.0f);

                // Interpolate color
                float progress = (float) i / particleCount;
                float r = Mth.lerp(progress, fromColor[0], toColor[0]);
                float g = Mth.lerp(progress, fromColor[1], toColor[1]);
                float b = Mth.lerp(progress, fromColor[2], toColor[2]);

                Particle particle = particleEngine.createParticle(
                    ParticleTypes.ENTITY_EFFECT,
                    x, y, z,
                    r, g, b
                );

                if (particle != null) {
                    particle.setPower(0.1f + entity.getRandom().nextFloat() * 0.1f);
                    particle.setLifetime(40 + entity.getRandom().nextInt(30));
                    particle.scale(0.4f + entity.getRandom().nextFloat() * 0.3f);
                }
            }
        }
    }

    /**
     * Spawn wish completion particles
     */
    @OnlyIn(Dist.CLIENT)
    public void spawnWishCompletionParticles(LivingEntity entity, float[] wishColor) {
        if (!enableParticles) return;

        Level level = entity.level();
        if (level.isClientSide && Minecraft.getInstance().player != null) {
            ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;

            int particleCount = 80 + entity.getRandom().nextInt(40);

            for (int i = 0; i < particleCount; i++) {
                float angle = entity.getRandom().nextFloat() * (float) Math.PI * 2;
                float distance = entity.getBbWidth() * 0.5f + entity.getRandom().nextFloat() * 2.0f;

                float x = (float) (entity.getX() + Math.cos(angle) * distance);
                float y = (float) (entity.getY() + entity.getBbHeight() * 0.5f);
                float z = (float) (entity.getZ() + Math.sin(angle) * distance);

                Particle particle = particleEngine.createParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    x, y, z,
                    wishColor[0], wishColor[1], wishColor[2]
                );

                if (particle != null) {
                    particle.setPower(0.15f);
                    particle.setLifetime(60);
                    particle.scale(0.6f + entity.getRandom().nextFloat() * 0.4f);
                }
            }
        }
    }

    /**
     * Update effect configuration
     */
    public void setManifestationParticleDensity(float density) {
        this.manifestationParticleDensity = Math.max(0.1f, Math.min(3.0f, density));
    }

    public void setManifestationParticleSpeed(float speed) {
        this.manifestationParticleSpeed = Math.max(0.01f, Math.min(0.5f, speed));
    }

    public void setAuraIntensity(float intensity) {
        this.auraIntensity = Math.max(0.0f, Math.min(2.0f, intensity));
    }

    public void setEnableAura(boolean enabled) {
        this.enableAura = enabled;
    }

    public void setEnableParticles(boolean enabled) {
        this.enableParticles = enabled;
    }

    public void setEnableGlow(boolean enabled) {
        this.enableGlow = enabled;
    }

    /**
     * Get effect configuration
     */
    public float getManifestationParticleDensity() {
        return manifestationParticleDensity;
    }

    public float getAuraIntensity() {
        return auraIntensity;
    }

    public boolean isAuraEnabled() {
        return enableAura;
    }

    public boolean areParticlesEnabled() {
        return enableParticles;
    }

    public boolean isGlowEnabled() {
        return enableGlow;
    }

    /**
     * Update all effects for an entity
     */
    @OnlyIn(Dist.CLIENT)
    public void updateEntityEffects(LivingEntity entity, float manifestationLevel,
                                   float[] manifestationColor, boolean manifested) {
        if (manifested) {
            spawnManifestationParticles(entity, manifestationLevel, manifestationColor);
            createManifestationAura(entity, manifestationLevel, manifestationColor);
        }
    }
}
