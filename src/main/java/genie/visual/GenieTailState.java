package genie.visual;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * State management for the genie's tail
 * Tracks tail visibility, length, color, and animation state
 */
public class GenieTailState implements INBTSerializable<CompoundTag> {
    private boolean visible = true;
    private int length = 16;
    private float red = 1.0f;
    private float green = 0.8f;
    private float blue = 0.6f;
    private float alpha = 1.0f;
    private float swayAmount = 0.0f;
    private float swaySpeed = 0.05f;
    private float curlAmount = 0.0f;
    private boolean glowEnabled = true;
    private boolean cutoutEnabled = false;

    public GenieTailState() {
        // Default values
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = Math.max(4, Math.min(32, length));
    }

    public float getRed() {
        return red;
    }

    public void setRed(float red) {
        this.red = Math.max(0.0f, Math.min(1.0f, red));
    }

    public float getGreen() {
        return green;
    }

    public void setGreen(float green) {
        this.green = Math.max(0.0f, Math.min(1.0f, green));
    }

    public float getBlue() {
        return blue;
    }

    public void setBlue(float blue) {
        this.blue = Math.max(0.0f, Math.min(1.0f, blue));
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    public float getSwayAmount() {
        return swayAmount;
    }

    public void setSwayAmount(float swayAmount) {
        this.swayAmount = swayAmount;
    }

    public float getSwaySpeed() {
        return swaySpeed;
    }

    public void setSwaySpeed(float swaySpeed) {
        this.swaySpeed = Math.max(0.01f, Math.min(0.2f, swaySpeed));
    }

    public float getCurlAmount() {
        return curlAmount;
    }

    public void setCurlAmount(float curlAmount) {
        this.curlAmount = Math.max(0.0f, Math.min(1.0f, curlAmount));
    }

    public boolean isGlowEnabled() {
        return glowEnabled;
    }

    public void setGlowEnabled(boolean glowEnabled) {
        this.glowEnabled = glowEnabled;
    }

    public boolean isCutoutEnabled() {
        return cutoutEnabled;
    }

    public void setCutoutEnabled(boolean cutoutEnabled) {
        this.cutoutEnabled = cutoutEnabled;
    }

    public void update() {
        // Update sway animation
        this.swayAmount += swaySpeed;
        if (swayAmount > Math.PI * 2) {
            swayAmount -= (float) (Math.PI * 2);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Visible", visible);
        tag.putInt("Length", length);
        tag.putFloat("Red", red);
        tag.putFloat("Green", green);
        tag.putFloat("Blue", blue);
        tag.putFloat("Alpha", alpha);
        tag.putFloat("SwaySpeed", swaySpeed);
        tag.putFloat("CurlAmount", curlAmount);
        tag.putBoolean("GlowEnabled", glowEnabled);
        tag.putBoolean("CutoutEnabled", cutoutEnabled);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        visible = tag.getBoolean("Visible");
        length = tag.getInt("Length");
        red = tag.getFloat("Red");
        green = tag.getFloat("Green");
        blue = tag.getFloat("Blue");
        alpha = tag.getFloat("Alpha");
        swaySpeed = tag.getFloat("SwaySpeed");
        curlAmount = tag.getFloat("CurlAmount");
        glowEnabled = tag.getBoolean("GlowEnabled");
        cutoutEnabled = tag.getBoolean("CutoutEnabled");
    }
}
