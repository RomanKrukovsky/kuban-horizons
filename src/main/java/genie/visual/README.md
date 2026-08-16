# Genie Visual Effects System

## Overview

This package contains all visual effects and animation systems for the Kuban Genie mod.

## Components

### Core Systems

1. **GenieTailEngine.java** - Main engine for managing genie tails
   - Handles tail creation, state management, and rendering
   - Manages tail states and models
   - Spawns particles and effects

2. **GenieTailState.java** - Tail state management
   - Tracks tail visibility, length, color, and animation state
   - Handles serialization/deserialization
   - Manages sway, curl, and glow effects

3. **GenieTailModel.java** - 3D model for the genie's tail
   - Uses GeckoLib 5.5.3 for animations
   - Handles model rendering and transformations
   - Supports multiple segments with smooth transitions

4. **GenieTailLayer.java** - Rendering layer system
   - Manages different rendering passes (base, glow, cutout)
   - Handles layer composition and blending
   - Supports dynamic layer switching

### Animation Systems

5. **CartoonAnatomyEngine.java** - Cartoon-style anatomy deformations
   - Applies exaggerated, cartoon-like transformations
   - Head bobbing, limb stretching, squash and stretch
   - Eye wobble and breathing effects

6. **GenieManifestationEffects.java** - Particle and visual effects
   - Spawns manifestation particles around genie
   - Creates aura effects when genie is manifested
   - Handles transformation particles
   - Supports wish completion effects

7. **GeckoLibIntegration.java** - GeckoLib 5.5.3 integration
   - Animation controllers and state management
   - Glowmask effect support
   - Cutout rendering support

### Configuration

8. **GenieVisualConfig.java** - Configuration settings
   - Tail rendering options
   - Particle effects settings
   - Aura intensity controls
   - Cartoon anatomy toggles

9. **ModelLayers.java** - Model layer management
   - Model layer location definitions
   - Model creation helpers

## GeckoLib 5.5.3 Integration

The system uses GeckoLib 5.5.3 for advanced animations:
- Animation controllers for tail sway, glow, and transformation effects
- Glowmask rendering for glowing tail effects
- Cutout rendering for stylized visuals

## Performance Considerations

- Particle effects are throttled based on configuration
- Tail segments are dynamically hidden when not visible
- Animation updates are optimized for performance
- Configuration allows disabling expensive effects

## Usage

### For Entity Rendering

```java
// Create tail engine instance
GenieTailEngine tailEngine = GenieTailEngine.getInstance();

// Get or create tail state for entity
GenieTailState tailState = tailEngine.getTailState(entity);

// Update tail from genie state
tailEngine.updateTailFromGenieState(entity, manifestationLevel, manifestationColor, manifested);

// Render tail
@SubscribeEvent
@OnlyIn(Dist.CLIENT)
public void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
    LivingEntity entity = event.getEntity();
    GenieTailEngine.getInstance().renderTail(entity, poseStack, bufferSource,
                                              packedLight, packedOverlay, partialTicks);
}
```

### For Animation Effects

```java
// Create cartoon anatomy engine
CartoonAnatomyEngine anatomyEngine = new CartoonAnatomyEngine();

// Apply to entity
CartoonAnatomyEngine.CartoonTransformations transformations =
    anatomyEngine.applyToEntity(entity);

// Use transformations in rendering
```

### For Particle Effects

```java
// Create manifestation effects engine
GenieManifestationEffects effects = new GenieManifestationEffects();

// Spawn particles
effects.spawnManifestationParticles(entity, manifestationLevel, manifestationColor);

// Create aura
effects.createManifestationAura(entity, manifestationLevel, manifestationColor);
```

## Animation Files

Expected animation JSON files (to be placed in resources):
- `assets/kubanhorizons/animations/genie/tail.idle.json`
- `assets/kubanhorizons/animations/genie/tail.glow.json`
- `assets/kubanhorizons/animations/genie/tail.disperse.json`
- `assets/kubanhorizons/animations/genie/tail.manifest.json`

## Texture Files

Expected texture files (to be placed in resources):
- `assets/kubanhorizons/textures/entity/genie/tail.png`
- `assets/kubanhorizons/textures/entity/genie/tail_glow.png`
- `assets/kubanhorizons/textures/entity/genie/tail_cutout.png`

## Configuration

All visual effects can be configured in the Forge config file:
- `config/kubanhorizons-server.toml`
- `config/kubanhorizons-client.toml`

## Dependencies

- Minecraft Forge 1.20.1+
- GeckoLib 5.5.3
- Java 17+

## Future Enhancements

- Dynamic tail length based on manifestation level
- Customizable tail colors and patterns
- Advanced shader effects for glow and aura
- Performance optimization for large numbers of entities
- Client-side configuration GUI
