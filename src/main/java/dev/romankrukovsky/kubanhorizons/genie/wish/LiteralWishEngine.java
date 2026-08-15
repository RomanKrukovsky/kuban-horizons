package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/** Движок режима «Исполнить буквально» (Literal Wish Execution Engine). */
public final class LiteralWishEngine {
    private static final Pattern LEADING_NUMBER = Pattern.compile("\\s*(\\d+)");

    private LiteralWishEngine() {
    }

    public static WishExecutor.Result executeLiteral(ServerLevel level, Player player, String rawText) {
        String text = rawText.toLowerCase(Locale.ROOT).trim();
        BlockPos playerPos = player.blockPosition();

        MagicalSignature.cast(level, player.position());

        // 1. «40 тысяч куриц» -> Буквально запрошенное число куриц с неба,
        //    ограниченное серверными лимитами безопасности (Закон буквальности).
        if (text.contains("куриц") || text.contains("chicken")) {
            int requested = Math.max(1, parseCount(text, 40));
            int maxTotal = KHServerConfig.genieLiteralMaxEntities();
            int maxPerChunk = KHServerConfig.genieLiteralMaxEntitiesPerChunk();
            int existingInChunk = countChickensInChunk(level, playerPos);
            int byTotal = Math.min(requested, maxTotal);
            int byChunk = Math.max(0, maxPerChunk - existingInChunk);
            int toSpawn = Math.min(byTotal, byChunk);
            if (toSpawn < requested) {
                player.sendSystemMessage(Component.translatable(
                        "wish.kubanhorizons.literal.truncated", toSpawn));
            }
            spawnChickens(level, playerPos, player.getYRot(), toSpawn);
            return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.literal_chickens");
        }

        // 2. «Хочу золото» -> Тжёлый слиток золота падающий прямо на голову
        if (text.contains("золот") || text.contains("gold")) {
            ItemEntity goldItem = new ItemEntity(level, playerPos.getX() + 0.5D, playerPos.getY() + 4.0D, playerPos.getZ() + 0.5D,
                    new ItemStack(Items.RAW_GOLD, 64));
            level.addFreshEntity(goldItem);
            level.sendParticles(ParticleTypes.CRIT, playerPos.getX() + 0.5D, playerPos.getY() + 2.0D, playerPos.getZ() + 0.5D,
                    20, 0.2D, 0.2D, 0.2D, 0.1D);
            return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.literal_gold");
        }

        // 3. Любое другое буквалистское желание
        level.sendParticles(ParticleTypes.WITCH, playerPos.getX() + 0.5D, playerPos.getY() + 1.0D, playerPos.getZ() + 0.5D,
                50, 0.8D, 0.8D, 0.8D, 0.1D);
        return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.literal_done");
    }

    private static int parseCount(String text, int fallback) {
        Matcher matcher = LEADING_NUMBER.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    private static int countChickensInChunk(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = ChunkPos.containing(pos);
        AABB box = new AABB(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(),
                chunk.getMinBlockX() + 15, level.getMaxY(), chunk.getMinBlockZ() + 15);
        List<Chicken> found = level.getEntitiesOfClass(Chicken.class, box);
        return found.size();
    }

    private static void spawnChickens(ServerLevel level, BlockPos playerPos, float yRot, int count) {
        for (int i = 0; i < count; i++) {
            double ox = level.getRandom().nextDouble() * 6.0D - 3.0D;
            double oz = level.getRandom().nextDouble() * 6.0D - 3.0D;
            Chicken chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
            if (chicken != null) {
                chicken.snapTo(playerPos.getX() + 0.5D + ox, playerPos.getY() + 10.0D + (i * 0.2D),
                        playerPos.getZ() + 0.5D + oz, yRot, 0.0F);
                level.addFreshEntity(chicken);
            }
        }
    }
}