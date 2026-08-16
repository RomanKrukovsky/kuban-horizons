package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Невысказанное Желание (GENIE_VISION §Существа): джинния угадывает, чего
 * хочет игрок, по контексту, когда тот молчит.
 *
 * <p>Закон памяти: джинния узнаёт желание по предмету в руке или блоку под
 * прицелом — меч в руке у моря значит «почини меч», инструмент у руды —
 * «добыть руду». Это угадывание намерения без слов.</p>
 */
public final class UnspokenWishEngine {

    private UnspokenWishEngine() {
    }

    /**
     * Пытается угадать невысказанное желание.
     *
     * @return true если желание исполнено, false если контекст не прочитан
     */
    public static boolean guess(ServerLevel level, ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        HitResult hit = player.pick(8.0D, 0.0F, false);

        // 1. Повреждённый меч в руке -> починить его.
        if (held.is(net.minecraft.tags.ItemTags.SWORDS) && held.isDamaged()) {
            int repaired = Math.min(held.getDamageValue(), held.getMaxDamage() - 1);
            held.setDamageValue(Math.max(0, held.getDamageValue() - Math.max(1, repaired / 2)));
            MagicalSignature.cast(level, player.position());
            player.sendSystemMessage(Component.translatable(
                    "wish.kubanhorizons.unspoken.repair"));
            return true;
        }

        // 2. Взгляд на руду -> добыть глыбу этой руды.
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            ItemStack ore = oreFor(level.getBlockState(pos).getBlock());
            if (!ore.isEmpty()) {
                player.getInventory().add(ore);
                MagicalSignature.cast(level, player.position());
                player.sendSystemMessage(Component.translatable(
                        "wish.kubanhorizons.unspoken.ore"));
                return true;
            }
        }

        // 3. Пустые руки и взгляд на грядку -> семена для неё.
        if (held.isEmpty() && hit.getType() == HitResult.Type.BLOCK
                && hit instanceof BlockHitResult blockHit2) {
            BlockPos pos = blockHit2.getBlockPos();
            if (level.getBlockState(pos).is(Blocks.FARMLAND)) {
                player.getInventory().add(new ItemStack(Items.WHEAT_SEEDS, 8));
                MagicalSignature.cast(level, player.position());
                player.sendSystemMessage(Component.translatable(
                        "wish.kubanhorizons.unspoken.seeds"));
                return true;
            }
        }

        return false;
    }

    private static ItemStack oreFor(net.minecraft.world.level.block.Block block) {
        if (block == Blocks.COAL_ORE) {
            return new ItemStack(Items.COAL, 8);
        }
        if (block == Blocks.IRON_ORE) {
            return new ItemStack(Items.RAW_IRON, 8);
        }
        if (block == Blocks.COPPER_ORE) {
            return new ItemStack(Items.RAW_COPPER, 8);
        }
        if (block == Blocks.GOLD_ORE) {
            return new ItemStack(Items.RAW_GOLD, 8);
        }
        return ItemStack.EMPTY;
    }
}