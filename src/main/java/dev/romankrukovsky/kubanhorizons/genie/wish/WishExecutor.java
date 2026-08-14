package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.memory.ContractEngine;
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

        // Integrate ContractEngine: check for active penalties/breaches before execution
        ContractEngine contracts = ContractEngine.get(level);
        java.util.List<dev.romankrukovsky.kubanhorizons.genie.memory.ContractEngine.Penalty> penalties =
                contracts.getPenalties(player.getUUID());
        if (!penalties.isEmpty()) {
            // Handle Penalty from ContractEngine: apply corruption, cooldown, or deny
            handleContractPenalty(level, player, penalties);
            return new Result(false, "message.kubanhorizons.genie.wish.contract_penalty");
        }

        Result result;
        WishIntent.Category cat = intent.category();
        if (cat == WishIntent.Category.META_RULE) {
            result = new Result(false, "message.kubanhorizons.genie.wish.policy_confirmation_required");
        } else if (cat == WishIntent.Category.GIGANTISM) {
            result = GigantismScaleEngine.execute(level, player, intent);
        } else if (cat == WishIntent.Category.MATERIAL) {
            result = executeMaterialWish(level, player, intent);
        } else if (cat == WishIntent.Category.CIVILIZATION) {
            result = executeCivilizationWish(level, player, intent);
        } else if (cat == WishIntent.Category.DISTORTED_HIGHER_WISH) {
            if (player instanceof net.minecraft.server.level.ServerPlayer) {
                net.minecraft.server.level.ServerPlayer serverPlayer =
                        (net.minecraft.server.level.ServerPlayer) player;
                result = DistortedWishEngine.execute(level, serverPlayer, intent);
            } else {
                result = new Result(false, "message.kubanhorizons.genie.wish.unknown");
            }
        } else {
            result = new Result(false, "message.kubanhorizons.genie.wish.unknown");
        }
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

    private static void handleContractPenalty(ServerLevel level, Player player,
                                              java.util.List<dev.romankrukovsky.kubanhorizons.genie.memory.ContractEngine.Penalty> penalties) {
        // Handle Penalty from ContractEngine: apply corruption, cooldown, or other effects on breach
        for (dev.romankrukovsky.kubanhorizons.genie.memory.ContractEngine.Penalty penalty : penalties) {
            if (player instanceof net.minecraft.server.level.ServerPlayer) {
                net.minecraft.server.level.ServerPlayer sp = (net.minecraft.server.level.ServerPlayer) player;
                dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment att =
                        sp.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA);
                att.setCorruption(att.getCorruption() + penalty.severity());
                // Example: set cooldown based on penalty type
                if ("BREACH".equalsIgnoreCase(penalty.type())) {
                    att.setLastWishTick(level.getGameTime() + 1200L); // 1 minute cooldown
                }
            }
        }
    }

    private static Result executeCivilizationWish(ServerLevel level, Player player, WishIntent intent) {
        BlockPos target = player.blockPosition().relative(player.getDirection(), 2);
        if (level.isEmptyBlock(target)) {
            level.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
            if (level.getBlockEntity(target) instanceof Container) {
                    Container chest = (Container) level.getBlockEntity(target);
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
        if (!(level.getBlockEntity(target) instanceof Container)) {
            return false;
        }
        Container chest = (Container) level.getBlockEntity(target);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.DIAMOND, 64));
        }
        chest.setChanged();
        level.sendParticles(ParticleTypes.ENCHANT, target.getX() + 0.5D, target.getY() + 0.8D,
                target.getZ() + 0.5D, 48, 0.8D, 0.6D, 0.8D, 0.1D);
        return true;
    }

    /** Простой результат исполнения желания (замена record для совместимости со старым MC). */
    public static final class Result {
        private final boolean executed;
        private final String messageKey;

        public Result(boolean executed, String messageKey) {
            this.executed = executed;
            this.messageKey = messageKey;
        }

        public boolean executed() {
            return executed;
        }

        public String messageKey() {
            return messageKey;
        }

        public Component message(int precision) {
            return Component.translatable(messageKey, precision);
        }
    }
}
