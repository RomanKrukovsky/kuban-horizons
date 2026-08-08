package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Единый серверный исполнитель всех категорий желаний. */
public final class WishExecutor {
    private WishExecutor() {
    }

    public static Result execute(ServerLevel level, Player player, WishIntent intent) {
        if (!intent.understood()) {
            return new Result(false, "message.kubanhorizons.genie.wish.unknown");
        }

        Result result = switch (intent.category()) {
            case META_RULE -> MetaRuleEngine.execute(level, player, intent);
            case GIGANTISM -> GigantismScaleEngine.execute(level, player, intent);
            case MATERIAL -> executeMaterialWish(level, player, intent);
            case CIVILIZATION -> executeCivilizationWish(level, player, intent);
            case DISTORTED_HIGHER_WISH -> (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                    ? DistortedWishEngine.execute(level, serverPlayer, intent)
                    : new Result(false, "message.kubanhorizons.genie.wish.unknown");
            default -> new Result(false, "message.kubanhorizons.genie.wish.unknown");
        };
        if (result.executed()) {
            WorldGenieMemory.get(level).recordWish(player.blockPosition(), intent.target().name(),
                    intent.precision(), level.getGameTime());
        }
        return result;
    }

    private static Result executeMaterialWish(ServerLevel level, Player player, WishIntent intent) {
        if (intent.isPreciseAndSafe()) {
            return placeDiamondChest(level, player)
                    ? new Result(true, "message.kubanhorizons.genie.wish.safe")
                    : new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }

        BlockPos source = player.blockPosition().above(3);
        while (!level.isEmptyBlock(source) && source.getY() < level.getMaxY() - 1) {
            source = source.above();
        }
        if (!level.isEmptyBlock(source)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                FallingBlockEntity falling = FallingBlockEntity.fall(level, source.offset(x, 0, z),
                        Blocks.DIAMOND_ORE.defaultBlockState());
                falling.setHurtsEntities(8.0F, 40);
            }
        }
        level.sendParticles(ParticleTypes.PORTAL, source.getX() + 0.5D, source.getY() + 0.5D,
                source.getZ() + 0.5D, 36, 0.7D, 0.7D, 0.7D, 0.1D);
        return new Result(true, "message.kubanhorizons.genie.wish.literal");
    }

    private static Result executeCivilizationWish(ServerLevel level, Player player, WishIntent intent) {
        BlockPos target = player.blockPosition().relative(player.getDirection(), 2);
        if (level.isEmptyBlock(target)) {
            level.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
            if (level.getBlockEntity(target) instanceof Container chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    chest.setItem(i, new ItemStack(Items.EMERALD, 64));
                }
                chest.setChanged();
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX() + 0.5D, target.getY() + 0.5D,
                        target.getZ() + 0.5D, 40, 0.8D, 0.8D, 0.8D, 0.1D);
                memoryRecordVillage(level, target);
                return new Result(true, "message.kubanhorizons.genie.wish.village_wealth");
            }
        }
        return new Result(false, "message.kubanhorizons.genie.wish.no_space");
    }

    private static void memoryRecordVillage(ServerLevel level, BlockPos pos) {
        WorldGenieMemory memory = WorldGenieMemory.get(level);
        memory.recordVillageSaved(pos, level.getGameTime());
    }

    private static boolean placeDiamondChest(ServerLevel level, Player player) {
        Direction facing = player.getDirection();
        BlockPos origin = player.blockPosition().relative(facing, 2);
        BlockPos target = origin;
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos candidate = origin.above(dy);
            if (level.isEmptyBlock(candidate) && level.getBlockState(candidate.below()).isSolid()) {
                target = candidate;
                break;
            }
        }
        if (!level.isEmptyBlock(target) || !level.getBlockState(target.below()).isSolid()) {
            return false;
        }

        level.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        if (!(level.getBlockEntity(target) instanceof Container chest)) {
            return false;
        }
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.DIAMOND, 64));
        }
        chest.setChanged();
        level.sendParticles(ParticleTypes.ENCHANT, target.getX() + 0.5D, target.getY() + 0.8D,
                target.getZ() + 0.5D, 48, 0.8D, 0.6D, 0.8D, 0.1D);
        return true;
    }

    public record Result(boolean executed, String messageKey) {
        public Component message(int precision) {
            return Component.translatable(messageKey, precision);
        }
    }
}
