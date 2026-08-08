package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Движок режима «Исполнить буквально» (Literal Wish Execution Engine). */
public final class LiteralWishEngine {
    private LiteralWishEngine() {
    }

    public static WishExecutor.Result executeLiteral(ServerLevel level, Player player, String rawText) {
        String text = rawText.toLowerCase(Locale.ROOT).trim();
        BlockPos playerPos = player.blockPosition();

        MagicalSignature.cast(level, player.position());

        // 1. «40 тысяч куриц» -> Буквально 40 куриц с неба
        if (text.contains("куриц") || text.contains("chicken")) {
            for (int i = 0; i < 40; i++) {
                double ox = level.getRandom().nextDouble() * 6.0D - 3.0D;
                double oz = level.getRandom().nextDouble() * 6.0D - 3.0D;
                LivingEntity chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
                if (chicken != null) {
                    chicken.snapTo(playerPos.getX() + 0.5D + ox, playerPos.getY() + 10.0D + (i * 0.2D),
                            playerPos.getZ() + 0.5D + oz, player.getYRot(), 0.0F);
                    level.addFreshEntity(chicken);
                }
            }
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
}
