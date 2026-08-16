package genie.transaction;

import genie.GenieAnchor;
import genie.wish.WishIntent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Service for generating previews of wish effects.
 * Supports 9 types of previews for player confirmation.
 */
public class PreviewService {
    private final Map<String, PreviewGenerator> previewGenerators;

    public PreviewService() {
        this.previewGenerators = new HashMap<>();
        registerDefaultGenerators();
    }

    /**
     * Register default preview generators
     */
    private void registerDefaultGenerators() {
        previewGenerators.put("word", new WordPreviewGenerator());
        previewGenerators.put("drawing", new DrawingPreviewGenerator());
        previewGenerators.put("policy", new PolicyPreviewGenerator());
        previewGenerators.put("pocket_scene", new PocketScenePreviewGenerator());
        previewGenerators.put("block_whispers", new BlockWhispersPreviewGenerator());
        previewGenerators.put("structure", new StructurePreviewGenerator());
        previewGenerators.put("anchor", new AnchorPreviewGenerator());
        previewGenerators.put("effect", new EffectPreviewGenerator());
        previewGenerators.put("miniatura", new MiniaturePreviewGenerator());
    }

    /**
     * Generate a preview for wish execution
     */
    public Preview generatePreview(ServerLevel level, Player player, WishIntent intent, BlockPos position) {
        Preview.Builder builder = new Preview.Builder(intent, player.getUUID(), position);
        builder.setTimestamp(System.currentTimeMillis());

        // Determine preview type based on wish intent
        String previewType = determinePreviewType(intent);
        PreviewGenerator generator = previewGenerators.get(previewType);

        if (generator != null) {
            generator.generate(level, player, intent, position, builder);
        } else {
            // Default word preview
            new WordPreviewGenerator().generate(level, player, intent, position, builder);
        }

        return builder.build();
    }

    /**
     * Determine preview type from wish intent
     */
    private String determinePreviewType(WishIntent intent) {
        if (intent.isLiteral()) {
            return "structure";
        } else if (intent.hasDrawing()) {
            return "drawing";
        } else if (intent.hasPolicy()) {
            return "policy";
        } else {
            return "word";
        }
    }

    /**
     * Preview generator interface
     */
    private interface PreviewGenerator {
        void generate(ServerLevel level, Player player, WishIntent intent, BlockPos position, Preview.Builder builder);
    }

    /**
     * Word preview generator
     */
    private static class WordPreviewGenerator implements PreviewGenerator {
        @Override
        public void generate(ServerLevel level, Player player, WishIntent intent, BlockPos position, Preview.Builder builder) {
            builder.setType("word");
            builder.setTitle("Wish Preview");
            builder.setDescription(intent.getText());
            builder.setColor(0xFFAA00); // Gold color
            builder.setIcon("✨");
        }
    }

    /**
     * Drawing preview generator
     */
    private static class DrawingPreviewGenerator implements PreviewGenerator {
        @Override
        public void generate(ServerLevel level, Player player, WishIntent intent, BlockPos position, Preview.Builder builder) {
            builder.setType("drawing");
            builder.setTitle("Magic Drawing");
            builder.setDescription("Interpreted drawing command");
            builder.setColor(0x00AAFF); // Blue color
            builder.setIcon("🎨");
        }
    }

    /**
     * Policy preview generator
     */
    private static class PolicyPreviewGenerator implements PreviewGenerator {
        @Override
        public void generate(ServerLevel level, Player player, WishIntent intent, BlockPos position, Preview.Builder builder) {
            builder.setType("policy");
            builder.setTitle("Policy Change");
            builder.setDescription("World rule modification");
            builder.setColor(0x00AA00); // Green color
            builder.setIcon("📜");
        }
    }

    /**
     * Pocket scene preview generator
     */
    private static class PocketScenePreviewGenerator implements PreviewGenerator {
        @Override
        public void generate(ServerLevel level, Player player, WishIntent intent, BlockPos position, Preview.Builder builder) {
            builder.setType("pocket_scene");
            builder.setTitle("Pocket Scene");
            builder.setDescription("Miniature dimension creation");
            builder.setColor(0xAA00AA); // Purple color
            builder.setIcon("📦");
        }
    }

    /**
     * Block whispers preview generator
     */
    private static class BlockWhispersPreviewGenerator implements PreviewGenerator {
        @Override
        public void generate(ServerLevel level, Player player, WishIntent intent, BlockPos position, Preview.Builder builder) {
            builder.setType("block_whispers");
            builder.setTitle("Block Memory");
            builder.setDescription("Block state preservation");
            builder.setColor(0x555555); // Gray color
            builder.setIcon("🗣️");
        }
    }

    /**
     * Structure preview generator
     */
    private static class StructurePreviewGenerator implements PreviewGenerator {
        @Override
        public void generate(ServerLevel level, Player player, WishIntent intent, BlockPos position, Preview.Builder builder) {
            builder.setType("structure");
            builder.setTitle("Structure");
            builder.setDescription("Block arrangement");
            builder.setColor(0xFF5555); // Red color
            builder.setIcon("🏗️");
        }
    }

    /**
     * Anchor preview generator
     */
    private static class AnchorPreviewGenerator implements PreviewGenerator {
        @Override
        public void generate(ServerLevel level, Player player, WishIntent intent, BlockPos position, Preview.Builder builder) {
            builder.setType("anchor");
            builder.setTitle("Genie Anchor");
            builder.setDescription("Wish anchor state");
            builder.setColor(0xFFAA00); // Gold color
            builder.setIcon("🧭");
        }
    }

    /**
     * Effect preview generator
     */
    private static class EffectPreviewGenerator implements PreviewGenerator {
        @Override
        public void generate(ServerLevel level, Player player, WishIntent intent, BlockPos position, Preview.Builder builder) {
            builder.setType("effect");
            builder.setTitle("Visual Effects");
            builder.setDescription("Particle and animation effects");
            builder.setColor(0xAA00FF); // Magenta color
            builder.setIcon("🎇");
        }
    }

    /**
     * Miniature preview generator
     */
    private static class MiniaturePreviewGenerator implements PreviewGenerator {
        @Override
        public void generate(ServerLevel level, Player player, WishIntent intent, BlockPos position, Preview.Builder builder) {
            builder.setType("miniatura");
            builder.setTitle("Miniature World");
            builder.setDescription("Diminished reality");
            builder.setColor(0x00FFAA); // Cyan color
            builder.setIcon("🔳");
        }
    }
}
