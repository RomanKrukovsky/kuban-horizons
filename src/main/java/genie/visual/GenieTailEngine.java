package genie.visual;

import genie.KubanGenie;
import genie.state.GenieStateSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Main engine for managing genie tails
 * Handles tail creation, state management, and rendering
 */
@Mod.EventBusSubscriber(modid = KubanGenie.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GenieTailEngine {

    // Singleton instance
    private static GenieTailEngine instance;

    // Tail state cache
    private final Map<UUID, GenieTailState> tailStates = new HashMap<>();
    private final Map<UUID, GenieTailModel> tailModels = new HashMap<>();

    // Configuration
    private boolean particlesEnabled = true;
    private boolean glowEffectsEnabled = true;
    private boolean cutoutEffectsEnabled = true;
    private int maxTailLength = 32;
    private float particleDensity = 1.0f;

    private GenieTailEngine() {
        // Private constructor for singleton
    }

    public static synchronized GenieTailEngine getInstance() {
        if (instance == null) {
            instance = new GenieTailEngine();
        }
        return instance;
    }

    /**
     * Create a new tail for an entity
     */
    public GenieTailState createTail(Entity entity) {
        UUID entityId = entity.getUUID();

        // Create new tail state
        GenieTailState tailState = new GenieTailState();
        tailState.setVisible(true);
        tailState.setLength(16);
        tailState.setRed(1.0f);
        tailState.setGreen(0.8f);
        tailState.setBlue(0.6f);

        // Store state
        tailStates.put(entityId, tailState);

        return tailState;
    }

    /**
     * Get tail state for an entity
     */
    public GenieTailState getTailState(Entity entity) {
        return tailStates.get(entity.getUUID());
    }

    /**
     * Update tail state from genie memory
     */
    public void updateTailFromMemory(Entity entity, GenieStateSnapshot snapshot) {
        UUID entityId = entity.getUUID();
        GenieTailState tailState = tailStates.get(entityId);

        if (tailState != null && snapshot != null) {
            // Update tail based on genie state
            tailState.setVisible(snapshot.isManifested());
            tailState.setLength(Math.min(maxTailLength, snapshot.getManifestationLevel()));
            tailState.setRed(snapshot.getManifestationColor()[0]);
            tailState.setGreen(snapshot.getManifestationColor()[1]);
            tailState.setBlue(snapshot.getManifestationColor()[2]);
        }
    }

    /**
     * Update tail state from genie state
     */
    public void updateTailFromGenieState(Entity entity, float manifestationLevel,
                                        float[] manifestationColor, boolean manifested) {
        UUID entityId = entity.getUUID();
        GenieTailState tailState = tailStates.computeIfAbsent(entityId, this::createTail);

        tailState.setVisible(manifested);
        tailState.setLength(Math.min(maxTailLength, (int) manifestationLevel));
        tailState.setRed(manifestationColor[0]);
        tailState.setGreen(manifestationColor[1]);
        tailState.setBlue(manifestationColor[2]);
    }

    /**
     * Update tail models
     */
    @OnlyIn(Dist.CLIENT)
    public void updateClientModels() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            // Update all tail models
            for (Map.Entry<UUID, GenieTailState> entry : tailStates.entrySet()) {
                Entity entity = minecraft.level.getEntity(entry.getKey().hashCode());
                if (entity instanceof LivingEntity) {
                    updateTailModel((LivingEntity) entity);
                }
            }
        }
    }

    /**
     * Update tail model for an entity
     */
    @OnlyIn(Dist.CLIENT)
    public void updateTailModel(LivingEntity entity) {
        UUID entityId = entity.getUUID();
        GenieTailState tailState = tailStates.get(entityId);

        if (tailState != null) {
            // Create or update model
            if (!tailModels.containsKey(entityId)) {
                // In actual implementation, this would load the model from resources
                // For now, we'll just track the state
                tailModels.put(entityId, null);
            }
        }
    }

    /**
     * Render tail for an entity
     */
    @OnlyIn(Dist.CLIENT)
    public void renderTail(LivingEntity entity, PoseStack poseStack,
                          MultiBufferSource bufferSource, int packedLight,
                          int packedOverlay, float partialTicks) {

        UUID entityId = entity.getUUID();
        GenieTailState tailState = tailStates.get(entityId);

        if (tailState != null && tailState.isVisible()) {
            // Get or create model
            GenieTailModel tailModel = tailModels.computeIfAbsent(entityId,
                id -> new GenieTailModel(ModelLayers.createGenieTailLayer()));

            // Update model visibility
            tailModel.updateVisibility(tailState);

            // Setup animation
            float limbSwing = entity.walkAnimation.position();
            float limbSwingAmount = entity.walkAnimation.speed();
            float ageInTicks = entity.tickCount + partialTicks;
            float netHeadYaw = entity.yHeadRot;
            float headPitch = entity.xRot;

            // Update and render model
            tailModel.setupAnim(tailState, limbSwing, limbSwingAmount, ageInTicks,
                              netHeadYaw, headPitch);
            tailModel.renderToBuffer(poseStack, bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(new ResourceLocation(KubanGenie.MOD_ID, "textures/entity/genie/tail.png"))),
                packedLight, packedOverlay, tailState.getRed(), tailState.getGreen(),
                tailState.getBlue(), tailState.getAlpha());
        }
    }

    /**
     * Spawn particles around the tail
     */
    public void spawnTailParticles(Entity entity) {
        if (!particlesEnabled) return;

        UUID entityId = entity.getUUID();
        GenieTailState tailState = tailStates.get(entityId);

        if (tailState != null && tailState.isVisible()) {
            // Calculate particle positions based on tail segments
            int segments = tailState.getLength();
            float segmentLength = 0.2f;

            for (int i = 0; i < segments * particleDensity; i++) {
                float progress = (float) i / (segments * particleDensity);
                float x = entity.getX() + (entity.getRandom().nextFloat() - 0.5f) * 0.5f;
                float y = entity.getY() + entity.getRandom().nextFloat() * entity.getBbHeight();
                float z = entity.getZ() + (entity.getRandom().nextFloat() - 0.5f) * 0.5f;

                // Spawn particle (placeholder - actual implementation would use level.addParticle)
                // Example: entity.level.addParticle(ParticleTypes.ENTITY_EFFECT, x, y, z,
                //     tailState.getRed(), tailState.getGreen(), tailState.getBlue());
            }
        }
    }

    /**
     * Configuration methods
     */
    public void setParticlesEnabled(boolean enabled) {
        this.particlesEnabled = enabled;
    }

    public void setGlowEffectsEnabled(boolean enabled) {
        this.glowEffectsEnabled = enabled;
        if (enabled) {
            this.cutoutEffectsEnabled = false;
        }
    }

    public void setCutoutEffectsEnabled(boolean enabled) {
        this.cutoutEffectsEnabled = enabled;
        if (enabled) {
            this.glowEffectsEnabled = false;
        }
    }

    public void setMaxTailLength(int length) {
        this.maxTailLength = Math.max(4, Math.min(64, length));
    }

    public void setParticleDensity(float density) {
        this.particleDensity = Math.max(0.1f, Math.min(2.0f, density));
    }

    /**
     * Client-side tick handler
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            updateClientModels();
        }
    }

    /**
     * Living entity render handler
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        UUID entityId = entity.getUUID();

        if (tailStates.containsKey(entityId)) {
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource bufferSource = event.getMultiBufferSource();
            int packedLight = event.getPackedLight();
            int packedOverlay = event.getPackedOverlay();
            float partialTicks = event.getPartialTick();

            renderTail(entity, poseStack, bufferSource, packedLight, packedOverlay, partialTicks);
        }
    }
}
