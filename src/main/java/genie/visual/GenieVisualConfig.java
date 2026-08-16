package genie.visual;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for visual effects and animations
 */
public class GenieVisualConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Tail configuration
    public static final ForgeConfigSpec.ConfigValue<Boolean> TAIL_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Integer> MAX_TAIL_LENGTH;
    public static final ForgeConfigSpec.ConfigValue<Boolean> TAIL_GLOW_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Boolean> TAIL_CUTOUT_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Double> TAIL_GLOW_INTENSITY;

    // Particle configuration
    public static final ForgeConfigSpec.ConfigValue<Boolean> PARTICLES_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Double> PARTICLE_DENSITY;
    public static final ForgeConfigSpec.ConfigValue<Double> PARTICLE_SPEED;

    // Aura configuration
    public static final ForgeConfigSpec.ConfigValue<Boolean> AURA_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Double> AURA_INTENSITY;

    // Cartoon anatomy configuration
    public static final ForgeConfigSpec.ConfigValue<Boolean> CARTOON_ANATOMY_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Double> HEAD_BOUNCE_SPEED;
    public static final ForgeConfigSpec.ConfigValue<Double> LIMB_STRETCH_SPEED;

    static {
        // Tail configuration
        BUILDER.push("Tail Settings");
        TAIL_ENABLED = BUILDER.comment("Enable genie tail rendering")
            .define("tailEnabled", true);
        MAX_TAIL_LENGTH = BUILDER.comment("Maximum tail length (4-64)")
            .defineInRange("maxTailLength", 32, 4, 64);
        TAIL_GLOW_ENABLED = BUILDER.comment("Enable glow effects on tail")
            .define("tailGlowEnabled", true);
        TAIL_CUTOUT_ENABLED = BUILDER.comment("Enable cutout rendering for tail")
            .define("tailCutoutEnabled", false);
        TAIL_GLOW_INTENSITY = BUILDER.comment("Glow effect intensity (0.0-2.0)")
            .defineInRange("tailGlowIntensity", 1.0, 0.0, 2.0);
        BUILDER.pop();

        // Particle configuration
        BUILDER.push("Particle Settings");
        PARTICLES_ENABLED = BUILDER.comment("Enable particle effects")
            .define("particlesEnabled", true);
        PARTICLE_DENSITY = BUILDER.comment("Particle density multiplier (0.1-3.0)")
            .defineInRange("particleDensity", 1.0, 0.1, 3.0);
        PARTICLE_SPEED = BUILDER.comment("Particle speed multiplier (0.01-0.5)")
            .defineInRange("particleSpeed", 0.1, 0.01, 0.5);
        BUILDER.pop();

        // Aura configuration
        BUILDER.push("Aura Settings");
        AURA_ENABLED = BUILDER.comment("Enable manifestation aura")
            .define("auraEnabled", true);
        AURA_INTENSITY = BUILDER.comment("Aura intensity (0.0-2.0)")
            .defineInRange("auraIntensity", 0.5, 0.0, 2.0);
        BUILDER.pop();

        // Cartoon anatomy configuration
        BUILDER.push("Cartoon Anatomy Settings");
        CARTOON_ANATOMY_ENABLED = BUILDER.comment("Enable cartoon-style anatomy deformations")
            .define("cartoonAnatomyEnabled", true);
        HEAD_BOUNCE_SPEED = BUILDER.comment("Head bounce animation speed (0.01-0.2)")
            .defineInRange("headBounceSpeed", 0.05, 0.01, 0.2);
        LIMB_STRETCH_SPEED = BUILDER.comment("Limb stretch animation speed (0.01-0.3)")
            .defineInRange("limbStretchSpeed", 0.03, 0.01, 0.3);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /**
     * Get tail enabled configuration
     */
    public static boolean isTailEnabled() {
        return TAIL_ENABLED.get();
    }

    /**
     * Get maximum tail length
     */
    public static int getMaxTailLength() {
        return MAX_TAIL_LENGTH.get();
    }

    /**
     * Get tail glow enabled
     */
    public static boolean isTailGlowEnabled() {
        return TAIL_GLOW_ENABLED.get();
    }

    /**
     * Get tail cutout enabled
     */
    public static boolean isTailCutoutEnabled() {
        return TAIL_CUTOUT_ENABLED.get();
    }

    /**
     * Get tail glow intensity
     */
    public static double getTailGlowIntensity() {
        return TAIL_GLOW_INTENSITY.get();
    }

    /**
     * Get particles enabled
     */
    public static boolean areParticlesEnabled() {
        return PARTICLES_ENABLED.get();
    }

    /**
     * Get particle density
     */
    public static double getParticleDensity() {
        return PARTICLE_DENSITY.get();
    }

    /**
     * Get particle speed
     */
    public static double getParticleSpeed() {
        return PARTICLE_SPEED.get();
    }

    /**
     * Get aura enabled
     */
    public static boolean isAuraEnabled() {
        return AURA_ENABLED.get();
    }

    /**
     * Get aura intensity
     */
    public static double getAuraIntensity() {
        return AURA_INTENSITY.get();
    }

    /**
     * Get cartoon anatomy enabled
     */
    public static boolean isCartoonAnatomyEnabled() {
        return CARTOON_ANATOMY_ENABLED.get();
    }

    /**
     * Get head bounce speed
     */
    public static double getHeadBounceSpeed() {
        return HEAD_BOUNCE_SPEED.get();
    }

    /**
     * Get limb stretch speed
     */
    public static double getLimbStretchSpeed() {
        return LIMB_STRETCH_SPEED.get();
    }
}
