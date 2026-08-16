package genie.visual;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Integration class for GeckoLib 5.5.3
 * Handles animations, glowmask effects, and cutout rendering
 */
public class GeckoLibIntegration implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final GenieTailState tailState;

    // Animation controllers
    private AnimationController<GeckoLibIntegration> tailController;
    private AnimationController<GeckoLibIntegration> glowController;

    public GeckoLibIntegration(GenieTailState tailState) {
        this.tailState = tailState;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Tail sway animation controller
        this.tailController = new AnimationController<>(this, "tailController", 0, state -> {
            // Play idle animation
            return state.setAndContinue(ANIMATION_IDLE);
        });

        controllers.add(tailController);

        // Glow effect controller
        this.glowController = new AnimationController<>(this, "glowController", 0, state -> {
            if (tailState.isGlowEnabled()) {
                return state.setAndContinue(ANIMATION_GLOW);
            }
            return state.setAndContinue(ANIMATION_IDLE);
        });

        controllers.add(glowController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /**
     * Animation definitions
     */
    public static final ResourceLocation ANIMATION_IDLE =
        new ResourceLocation("kubanhorizons", "animations/genie/tail.idle.json");

    public static final ResourceLocation ANIMATION_GLOW =
        new ResourceLocation("kubanhorizons", "animations/genie/tail.glow.json");

    public static final ResourceLocation ANIMATION_DISPERSE =
        new ResourceLocation("kubanhorizons", "animations/genie/tail.disperse.json");

    public static final ResourceLocation ANIMATION_MANIFEST =
        new ResourceLocation("kubanhorizons", "animations/genie/tail.manifest.json");

    /**
     * Get the tail state
     */
    public GenieTailState getTailState() {
        return tailState;
    }

    /**
     * Update animation controllers based on tail state
     */
    public void updateAnimations() {
        if (tailState.isGlowEnabled()) {
            glowController.setAnimation(ANIMATION_GLOW);
        } else {
            glowController.setAnimation(ANIMATION_IDLE);
        }
    }

    /**
     * Apply glowmask effect
     * This would be used in actual rendering to apply glow effects
     */
    public void applyGlowmaskEffect() {
        // In actual implementation, this would:
        // 1. Enable glowmask rendering
        // 2. Set glow color from tail state
        // 3. Apply glow intensity based on manifestation level
        // 4. Render with additive blending
    }

    /**
     * Apply cutout effect
     * This would be used in actual rendering to apply cutout effects
     */
    public void applyCutoutEffect() {
        // In actual implementation, this would:
        // 1. Enable cutout rendering mode
        // 2. Set transparency based on tail state
        // 3. Render with cutout blending
    }

    /**
     * Check if glowmask is enabled
     */
    public boolean isGlowmaskEnabled() {
        return tailState.isGlowEnabled();
    }

    /**
     * Check if cutout is enabled
     */
    public boolean isCutoutEnabled() {
        return tailState.isCutoutEnabled();
    }
}
