package genie.animation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;

/**
 * Engine for cartoon-style anatomy deformations
 * Applies exaggerated, cartoon-like transformations to entities
 */
public class CartoonAnatomyEngine {

    // Animation states
    private float headBobAmount = 0.0f;
    private float headBobSpeed = 0.05f;
    private float limbStretchAmount = 0.0f;
    private float limbStretchSpeed = 0.03f;
    private float squashAmount = 0.0f;
    private float squashSpeed = 0.04f;
    private float stretchAmount = 0.0f;
    private float stretchSpeed = 0.035f;
    private float eyeWobbleAmount = 0.0f;
    private float eyeWobbleSpeed = 0.06f;

    private float animationTimer = 0.0f;

    /**
     * Update animation state
     */
    public void update() {
        animationTimer += 0.05f;

        // Update head bob
        headBobAmount = (float) Math.sin(animationTimer * headBobSpeed) * 0.1f;

        // Update limb stretch
        limbStretchAmount = (float) Math.sin(animationTimer * limbStretchSpeed) * 0.15f;

        // Update squash and stretch
        squashAmount = (float) Math.sin(animationTimer * squashSpeed) * 0.08f;
        stretchAmount = (float) Math.sin(animationTimer * stretchSpeed) * 0.07f;

        // Update eye wobble
        eyeWobbleAmount = (float) Math.sin(animationTimer * eyeWobbleSpeed) * 0.05f;
    }

    /**
     * Apply cartoon anatomy effects to an entity
     * Returns transformation values for rendering
     */
    public CartoonTransformations applyToEntity(LivingEntity entity) {
        CartoonTransformations transformations = new CartoonTransformations();

        // Apply head bob
        transformations.headBobY = headBobAmount;

        // Apply limb stretch to arms and legs
        float limbSide = entity.getRandom().nextFloat() > 0.5f ? 1.0f : -1.0f;
        transformations.leftArmStretch = limbStretchAmount * limbSide;
        transformations.rightArmStretch = limbStretchAmount * -limbSide;
        transformations.leftLegStretch = limbStretchAmount * limbSide;
        transformations.rightLegStretch = limbStretchAmount * -limbSide;

        // Apply squash and stretch based on movement
        float movementFactor = Mth.clamp(entity.walkAnimation.speed() * 0.5f, 0.0f, 1.0f);
        transformations.squashAmount = squashAmount * movementFactor;
        transformations.stretchAmount = stretchAmount * movementFactor;

        // Apply eye wobble
        transformations.eyeWobbleX = eyeWobbleAmount * 0.5f;
        transformations.eyeWobbleY = eyeWobbleAmount * 0.3f;

        // Apply breathing effect
        float breathCycle = animationTimer * 0.07f;
        transformations.bodyBreathY = (float) Math.sin(breathCycle) * 0.02f;

        return transformations;
    }

    /**
     * Reset all animations
     */
    public void reset() {
        headBobAmount = 0.0f;
        limbStretchAmount = 0.0f;
        squashAmount = 0.0f;
        stretchAmount = 0.0f;
        eyeWobbleAmount = 0.0f;
        animationTimer = 0.0f;
    }

    /**
     * Configuration methods
     */
    public void setHeadBobSpeed(float speed) {
        this.headBobSpeed = Math.max(0.01f, Math.min(0.2f, speed));
    }

    public void setLimbStretchSpeed(float speed) {
        this.limbStretchSpeed = Math.max(0.01f, Math.min(0.3f, speed));
    }

    public void setSquashSpeed(float speed) {
        this.squashSpeed = Math.max(0.01f, Math.min(0.2f, speed));
    }

    public void setStretchSpeed(float speed) {
        this.stretchSpeed = Math.max(0.01f, Math.min(0.2f, speed));
    }

    public void setEyeWobbleSpeed(float speed) {
        this.eyeWobbleSpeed = Math.max(0.01f, Math.min(0.3f, speed));
    }

    /**
     * Transformation values for cartoon anatomy
     */
    public static class CartoonTransformations {
        public float headBobY = 0.0f;
        public float leftArmStretch = 0.0f;
        public float rightArmStretch = 0.0f;
        public float leftLegStretch = 0.0f;
        public float rightLegStretch = 0.0f;
        public float squashAmount = 0.0f;
        public float stretchAmount = 0.0f;
        public float eyeWobbleX = 0.0f;
        public float eyeWobbleY = 0.0f;
        public float bodyBreathY = 0.0f;

        public boolean hasTransformations() {
            return Math.abs(headBobY) > 0.001f ||
                   Math.abs(leftArmStretch) > 0.001f ||
                   Math.abs(rightArmStretch) > 0.001f ||
                   Math.abs(squashAmount) > 0.001f ||
                   Math.abs(stretchAmount) > 0.001f;
        }
    }
}
